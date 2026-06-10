package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import com.jessee.git_remote_repo_listener.properties.DatabaseConcurrentProperties;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import com.jessee.git_remote_repo_listener.service.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 远程仓库分析结果持久化器实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeResultPersisterImpl implements AnalyzeResultPersister
{
    /** 远程仓库分析记录数据服务接口。*/
    private final AnalyzeRecordService analyzeRecordService;

    /** 远程仓库分析仓库数据服务接口。*/
    private final RepositoryService repositoryService;

    /** 远程仓库分析仓库下分支服务接口。*/
    private final BranchService branchService;

    /** 远程仓库分析分支引用变化数据服务接口。*/
    private final BranchRefChangeService branchRefChangeService;

    /** 远程仓库分析分支文件变更数据服务接口。*/
    private final BranchFileChangeService branchFileChangeService;

    /** 数据库的限流背压配置数据库的限流背压配置类。*/
    private final DatabaseConcurrentProperties properties;

    /** R2DBC 事务操作器。*/
    @Qualifier("R2dbcTransactionalOperator")
    private final TransactionalOperator transactionalOperator;

    /** 从远程仓库下每个分支的文件变更状态中提取分支名列表。*/
    private static List<String>
    extractBranchNameList(BranchFileChanges branchFileChanges)
    {
        return
        branchFileChanges.getBranchFileChanges()
            .stream()
            .map(BranchFileChange::getBranchName)
            .toList();
    }

    /** 从远程仓库下每个分支的文件变更状态中提取分支变化状态列表。*/
    private static List<BranchRefChange>
    extractBranchRefChangeList(BranchFileChanges branchFileChanges)
    {
        return
        branchFileChanges.getBranchFileChanges()
            .stream()
            .map(BranchFileChange::getBranchRefChange)
            .toList();
    }

    /** 将每个分支 ID 与对应的分支文件变化绑定成 Map。*/
    private static Map<Long, List<FileChange>>
    bindBrachFileChangesMap(
        final List<Long>        branchIds,
        final BranchFileChanges branchFileChanges
    )
    {
        return
        IntStream.range(
            0,
            Math.min(
                branchIds.size(),
                branchFileChanges.getBranchFileChanges().size()
            )
        )
        .mapToObj((index) ->
            Map.entry(
                branchIds.get(index),
                branchFileChanges.getBranchFileChanges()
                    .get(index)
                    .getFileChanges()
            )
        )
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 保存每个分支的文件变更1分析数据到数据库，
     * 在分析大仓库的时候（比如 jdk）每日会出现大量的文件变更，
     * 如果对 flatMap() 操作的并发数不加以限制，很容易压垮下游数据库，
     * 具体的限制参数见配置项 app.database-concurrent。
     */
    private Mono<Void> saveBranchFileChanges(
        final Long              analyzeId,
        final Long              repositoryId,
        final List<Long>        branchIds,
        final BranchFileChanges branchFileChanges
    )
    {
        return
        Flux.fromIterable(
             bindBrachFileChangesMap(branchIds, branchFileChanges)
                 .entrySet())
            .flatMap((entry) -> {
                final Long branchId                = entry.getKey();
                final List<FileChange> fileChanges = entry.getValue();

                /*
                 * 如果遇到了没有任何变化的分支，比如：
                 * {
                 *     "branchName" : "origin/fix/sys_operator_log",
                 *     "branchRefChange" : {
                 *          "prevLatestCommitHash" : "4667ffed...",
                 *          "latestCommitHash" : "4667ffed...",
                 *          "status" : "IMMUTABLE"
                 *      },
                 *      "fileChanges" : [ ] <<-- 注意这里
                 *  }
                 *
                 * 则直接返回，后续的持久化操作不要执行，
                 * 避免 ID 消费机出现这样的警告。
                 *
                 * WARN  c.j.g.c.impl.GlobalIdConsumerImpl - Batch size must be positive!
                 */
                if (CollectionUtils.isEmpty(fileChanges)) {
                    return Mono.empty();
                }

                return
                this.branchFileChangeService.saveBranchFileChanges(
                    analyzeId, repositoryId, branchId, fileChanges
                );
            }, this.properties.getMaxFileChanges()
        ).then();
    }

    /**
     * 保存一条分析记录数据，最开始 is_complete 字段会被设为 N。
     *
     * @param analyzeDateTime 上游给出的分析开始时间
     * @return 分析记录 ID
     */
    @Override
    public Mono<Long>
    saveAnalyzeRecord(LocalDateTime analyzeDateTime) {
        return this.analyzeRecordService.saveAnalyzeRecord(analyzeDateTime);
    }

    /**
     * 将 {@link RemoteRepositoryAnalyzerService#doAnalysis()}
     * 的分析结果持久化到数据库。
     */
    @Override
    public Mono<Void> save(
        final Long                    analyzeId,
        final LocalDateTime           ayalyzDateTime,
        final List<BranchFileChanges> analyzeResults
    )
    {
        return
        Flux.fromIterable(analyzeResults)
            .flatMap((branchFileChanges) ->
                this.repositoryService
                    .saveRepository(analyzeId, branchFileChanges.getRemoteRepository())
                    .flatMap((repositoryId) ->
                        this.branchService
                            .saveBranchs(analyzeId, repositoryId, extractBranchNameList(branchFileChanges))
                            .flatMap((branchIds) ->
                                Mono.zip(
                                    this.branchRefChangeService.saveBranchRefChanges(
                                        analyzeId, repositoryId, branchIds,
                                        extractBranchRefChangeList(branchFileChanges)
                                    ),
                                    this.saveBranchFileChanges(
                                        analyzeId, repositoryId, branchIds,
                                        branchFileChanges
                                    )
                                )
                            )
                    )
                    .then(this.analyzeRecordService.setAnalyzComplete(analyzeId))
                    .as(this.transactionalOperator::transactional)
                    .onErrorResume((exception) -> {
                        log.error(
                            branchFileChanges.getRemoteRepository().getDirectoryName(),
                            "Save analyze result data by repository {} failed, skip.",
                            exception
                        );

                        // 单个仓库的数据保存失败不影响整体
                        return Mono.empty();
                    }),
                this.properties.getMaxRepos()
            ).then();
    }
}