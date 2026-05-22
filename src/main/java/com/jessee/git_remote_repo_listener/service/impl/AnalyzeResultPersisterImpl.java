package com.jessee.git_remote_repo_listener.service.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.*;
import com.jessee.git_remote_repo_listener.mapper.*;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import com.jessee.git_remote_repo_listener.service.AnalyzeResultPersister;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 远程仓库分析结果持久化器实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeResultPersisterImpl implements AnalyzeResultPersister
{
    /** 分析记录表数据映射接口。*/
    private final AnalyzeRecordMapper analyzeRecordMapper;

    /** 代码仓库表数据映射接口。*/
    private final RepositoryMapper repositoryMapper;

    /** 仓库分支数据映射接口。*/
    private final BranchMapper branchMapper;

    /** 分支快照引用变化数据映射接口。*/
    private final BranchRefChangeMapper branchRefChangeMapper;

    /** 分支文件提交变更数据映射接口。*/
    private final BranchFileChangeMapper branchFileChangeMapper;

    /** R2DBC 事务操作器。*/
    @Qualifier("R2dbcTransactionalOperator")
    private final TransactionalOperator transactionalOperator;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 往 analyze_record 表中写入分析记录数据，返回 ID。*/
    public Mono<Long>
    saveAnalyzeRecord(LocalDateTime ayalyzDateTime)
    {
        return
        this.globalIdConsumer.nextId()
            .flatMap((id) -> {
                final AnalyzeRecord analyzeRecord
                    = new AnalyzeRecord();

                analyzeRecord.setId(id);
                analyzeRecord.setIsNew(true);

                return
                this.analyzeRecordMapper
                    .save(analyzeRecord.setAnalyzeDateTime(ayalyzDateTime));
            })
            .map((analyzeRecord) ->
                Objects.requireNonNull(analyzeRecord.getId()));
    }

    /** 往 repository 表写入仓库数据，返回该条记录的 ID。*/
    public Mono<Long> saveRepository(
        final Long ayalyzeId,
        final RepoPathProperties.RepoConfig repoConfig
    )
    {
        return
        this.globalIdConsumer.nextId()
            .flatMap((id) -> {
                final RepositoryEntity repository
                    = new RepositoryEntity();

                repository.setId(id);
                repository.setIsNew(true);

                return
                this.repositoryMapper.save(
                    repository.setAnalyzeId(ayalyzeId)
                              .setLocalPath(repoConfig.getPath())
                              .setRemoteName(repoConfig.getRemote())
                );
            })
            .map((repository) ->
                Objects.requireNonNull(repository.getId()))
            .doOnSuccess((repoId) ->
                log.info(
                    "Call saveRepository({}) return repo id = {}",
                    repoConfig, repoId
                )
            );
    }

    /** 往 branch 表写入指定仓库下的分支数据，返回写入的所有分支记录的 ID 列表。*/
    public Mono<List<Long>> saveBranchs(
        final Long          ayalyzeId,
        final Long          repositoryId,
        final List<String>  branchNames,
        final LocalDateTime queryDateTime
    )
    {
        if (CollectionUtils.isEmpty(branchNames)) {
            return Mono.empty();
        }

        return
        Flux.zip(
            this.globalIdConsumer.nextBatchIdFlux(branchNames.size()),
            Flux.fromIterable(branchNames)
        )
        .map((tuple) -> {
            final Long id           = tuple.getT1();
            final String branchName = tuple.getT2();

            final BranchEntity branch = new BranchEntity();

            branch.setId(id);
            branch.setIsNew(true);

            return
            branch.setAnalyzeId(ayalyzeId)
                  .setRepositoryId(repositoryId)
                  .setBranchName(branchName);
        })
        .collectList()
        .flatMap((branchs) ->
            this.branchMapper.saveAll(branchs)
                .map((branch) ->
                    Objects.requireNonNull(branch.getId()))
                .collectList()
        )
        .doOnSuccess((ignore) ->
            log.info(
                "Call saveBranchs({}, {}, {})",
                repositoryId, branchNames, queryDateTime
            )
        );
    }

    /**
     * 往 branch_ref_change 表写入两次分支快照的变化数据。
     * branch 与  branch_ref_change 的关系是一对多，
     * 但是在同一次分析任务下，每个分支都只会有一次变化记录。
     *
     * @param repositoryId     仓库 ID
     * @param branchIds        仓库下的分支 ID 列表
     * @param branchRefChanges 仓库下每个分支的一次变化记录
     *
     * @return 无后续延伸，无需向下游传递数据
     */
    public Mono<Void> saveBranchRefChanges(
        final Long                  ayalyzeId,
        final Long                  repositoryId,
        final List<Long>            branchIds,
        final List<BranchRefChange> branchRefChanges
    )
    {
        if (CollectionUtils.isEmpty(branchIds)) {
            return Mono.empty();
        }

        return
        Flux.zip(
            this.globalIdConsumer.nextBatchIdFlux(branchIds.size()),
            Flux.fromIterable(branchIds),
            Flux.fromIterable(branchRefChanges)
        )
        .map((tuple) -> {
            final Long id       = tuple.getT1();
            final Long branchId = tuple.getT2();
            final BranchRefChange refChange = tuple.getT3();

            final BranchRefChangeEntity branchRefChange
                = new BranchRefChangeEntity();

            branchRefChange.setId(id);
            branchRefChange.setIsNew(true);

            return
            branchRefChange.setAnalyzeId(ayalyzeId)
                .setRepositoryId(repositoryId)
                .setBranchId(branchId)
                .setPrevLatestCommitHash(refChange.getPrevLatestCommitHash())
                .setLatestCommitHash(refChange.getLatestCommitHash())
                .setChangeStaus(refChange.getStatus());
        })
        .collectList()
        .flatMap((entities) ->
            this.branchRefChangeMapper
                .saveAll(entities)
                .then())
        .doOnSuccess((ignore) ->
            log.info(
                "Call saveBranchRefChanges({}, branchIds, branchRefChanges, queryDateTime)",
                repositoryId
            )
        );
    }

    /**
     * 往 branch_file_change 表写入每个分支的文件提交变化数据。
     *
     * @param repositoryId    仓库 ID
     * @param branchId        仓库下的分支 ID
     * @param fileChanges     本次分析获取的本分支文件提交变化列表
     *
     * @return 无后续延伸，无需向下游传递数据
     */
    public Mono<Void> saveBranchFileChanges(
        final Long              ayalyzeId,
        final Long              repositoryId,
        final Long              branchId,
        final List<FileChange>  fileChanges
    )
    {
        if (CollectionUtils.isEmpty(fileChanges)) {
            return Mono.empty();
        }

        return
        Flux.zip(
            this.globalIdConsumer.nextBatchIdFlux(fileChanges.size()),
            Flux.fromIterable(fileChanges)
        )
        .map((tuple) -> {
            final Long id               = tuple.getT1();
            final FileChange fileChange = tuple.getT2();

            final BranchFileChangeEntity branchFileChange
                = new BranchFileChangeEntity();

            branchFileChange.setId(id);
            branchFileChange.setIsNew(true);

            return
            branchFileChange.setAnalyzeId(ayalyzeId)
                .setRepositoryId(repositoryId)
                .setBranchId(branchId)
                .setChangeStatus(fileChange.getStatus())
                .setSimilarity(fileChange.getSimilarity())
                .setOldPath(fileChange.getOldPath())
                .setNewPath(fileChange.getNewPath());
        })
        .collectList()
        .flatMap((fileChangeEntities) ->
            this.branchFileChangeMapper
                .saveAll(fileChangeEntities)
                .then())
        .doOnSuccess((ignore) ->
            log.info(
                "Call saveBranchFileChanges({}, {}, fileChanges, queryDateTime)",
                repositoryId, branchId
            )
        );
    }

    /**
     * 将 {@link RemoteRepositoryAnalyzerService#doAnalysis()}
     * 的分析结果持久化到数据库。
     */
    @Override
    public Mono<Void>
    save(LocalDateTime ayalyzDateTime, List<BranchFileChanges> analyzeResults)
    {
        return
        this.saveAnalyzeRecord(ayalyzDateTime)
            .flatMap((analyzeId) ->
                Flux.fromIterable(analyzeResults).concatMap((branchFileChanges) ->
                    this.saveRepository(analyzeId, branchFileChanges.getRepoConfig())
                        .flatMap((repositoryId) -> {
                            final List<String> branchNames
                                = branchFileChanges.getBranchFileChanges()
                                    .stream()
                                    .map(BranchFileChange::getBranchName)
                                    .toList();

                            return
                            this.saveBranchs(analyzeId, repositoryId, branchNames, ayalyzDateTime)
                                .flatMap((branchIds) -> {
                                    final List<BranchRefChange> branchRefChanges
                                        = branchFileChanges.getBranchFileChanges()
                                            .stream()
                                            .map(BranchFileChange::getBranchRefChange)
                                            .toList();

                                    final Map<Long, List<FileChange>> branchFileChangesMap
                                        = IntStream.range(
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


                                    return
                                    this.saveBranchRefChanges(
                                        analyzeId, repositoryId, branchIds,
                                            branchRefChanges
                                        )
                                        .then(
                                            Flux.fromIterable(branchFileChangesMap.entrySet())
                                                .flatMap((entry) -> {
                                                    final Long branchId = entry.getKey();
                                                    final List<FileChange> fileChanges = entry.getValue();

                                                    return
                                                    this.saveBranchFileChanges(
                                                        analyzeId, repositoryId, branchId,
                                                        fileChanges
                                                    );
                                                })
                                                .collectList()
                                                .then()
                                        );
                                });
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .as(this.transactionalOperator::transactional)
                    )
                    .collectList()
                    .then()
                    .doOnError(e -> log.error("Save failed", e))
            );
    }
}