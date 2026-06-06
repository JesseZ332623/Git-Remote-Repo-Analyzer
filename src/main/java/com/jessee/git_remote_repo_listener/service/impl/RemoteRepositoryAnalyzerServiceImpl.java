package com.jessee.git_remote_repo_listener.service.impl;

import com.jessee.git_remote_repo_listener.cache.RemoteRepositoryAnalyzerCacher;
import com.jessee.git_remote_repo_listener.cache.RemoteRepositoryCacher;
import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.pojo.*;
import com.jessee.git_remote_repo_listener.service.persistence.AnalyzeResultPersister;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import com.jessee.git_remote_repo_listener.utils.RemoteSnapshotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** 远程仓库分析服务类实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzerServiceImpl
    implements RemoteRepositoryAnalyzerService
{
    /** 待分析仓库缓存操作接口。*/
    private final
    RemoteRepositoryCacher remoteRepositoryCacher;

    /** Git 远程仓库分析器接口。*/
    private final RemoteRepositoryAnalyzer remoteRepositoryAnalyzer;

    /** 远程仓库分析器服务缓存接口。*/
    private final RemoteRepositoryAnalyzerCacher cacher;

    /** 分析结果持久化器。*/
    private final AnalyzeResultPersister analyzeResultPersister;

    /** 查询本地仓库每个分支的最新提交哈希，并将该结果缓存至 Redis。*/
    private Mono<Void>
    queryForEachRef(RemoteRepository remoteRepository)
    {
        final String localRepoPath = remoteRepository.getPath();
        final String repoRemote    = remoteRepository.getRemote();

        return
        this.remoteRepositoryAnalyzer
            .gitForeachRef(localRepoPath, repoRemote)
            .flatMap((forEachRefs) ->
                this.cacher.cacheForEachRefsMap(remoteRepository, forEachRefs))
            .then();
    }

    /** 从远程仓库拉取变更信息。*/
    private Mono<Void> fetchRemote(String localRepoPath) {
        return this.remoteRepositoryAnalyzer.gitFetch(localRepoPath);
    }

    /** 查询当前本地仓库每个分支的最新提交哈希并与上一个快照进行比对。*/
    private Mono<Map<String, BranchRefChange>>
    compareForEachRef(RemoteRepository remoteRepository)
    {
        final String localRepoPath = remoteRepository.getPath();
        final String repoRemote    = remoteRepository.getRemote();

        return
        Mono.zip(
                this.cacher.getForEachRefsMap(remoteRepository),
                this.remoteRepositoryAnalyzer
                    .gitForeachRef(localRepoPath, repoRemote)
            )
            .flatMap((tuple) ->
                RemoteSnapshotUtils.compareRemoteHash(tuple.getT1(), tuple.getT2()));
    }

    /** 查询并解析有更新的远程分支的详细文件变更状态。*/
    private Mono<List<BranchFileChange>>
    makeBranchFileChangeList(
        final RemoteRepository remoteRepository,
        final Map<String, BranchRefChange> refChangeMap
    )
    {
        // 如果上游的 refChangeMap 为空，
        // 则意味着分支状态完全同步，从缓存返回结果即可。
        if (refChangeMap.isEmpty()) {
            return this.cacher.getBranchFileChanges(remoteRepository);
        }

        return
        Flux.fromIterable(refChangeMap.entrySet())
            .flatMap((refChangeEntry) ->
                this.remoteRepositoryAnalyzer
                    .gitDiff(remoteRepository.getPath(), refChangeEntry.getValue())
                    .map((fileChanges) ->
                        BranchFileChange.of(
                            refChangeEntry.getKey(),
                            refChangeEntry.getValue(),
                            fileChanges
                        )
                    )
            )
            .collectList()
            .flatMap((branchFileChanges) ->
                this.cacher
                    .cacheBranchFileChanges(remoteRepository, branchFileChanges)
                    .thenReturn(branchFileChanges)
            );
    }

    @Override
    public Mono<AnalyzeResult> doAnalysis()
    {
        return
        Mono.defer(() -> {
            final LocalDateTime analyzeDateTime
                = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

            return
            this.analyzeResultPersister.saveAnalyzeRecord(analyzeDateTime)
                .flatMap((analyzeId) ->
                    this.remoteRepositoryCacher.getAll()
                        .flatMap((repo) ->
                            this.queryForEachRef(repo)
                                .then(this.fetchRemote(repo.getPath()))
                                .then(this.compareForEachRef(repo))
                                .flatMap((refChangeMap) ->
                                    this.makeBranchFileChangeList(repo, refChangeMap)
                                        .map((list) ->
                                            BranchFileChanges.of(repo, list))
                                )
                                .doOnError((exception) ->
                                    log.error(
                                        "Analysis local repository {}, remote {} failed.",
                                        repo.getPath(), repo.getRemote(),
                                        exception
                                    )
                                )
                                // 如果其中一个仓库的分析失败了，不要中断整个任务
                                .onErrorResume((exception) -> Mono.empty())
                        )
                        .collectList()
                        .flatMap((analyzeResults) ->
                            Mono.zip(
                                RemoteSnapshotUtils.toFile(analyzeResults),
                                this.analyzeResultPersister
                                    .save(analyzeId, analyzeDateTime, analyzeResults)
                            )
                            .thenReturn(new AnalyzeResult(analyzeDateTime, analyzeResults))
                        )
                );
        });
    }
}