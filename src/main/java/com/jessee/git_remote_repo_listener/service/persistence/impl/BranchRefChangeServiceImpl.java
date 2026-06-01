package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.BranchRefChangeEntity;
import com.jessee.git_remote_repo_listener.mapper.BranchRefChangeMapper;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.service.persistence.BranchRefChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** 远程仓库分析分支引用变化数据服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchRefChangeServiceImpl implements BranchRefChangeService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 分支快照引用变化数据映射接口。*/
    private final BranchRefChangeMapper branchRefChangeMapper;

    /**
     * 往 branch_ref_change 表写入两次分支快照的变化数据。
     * branch 与  branch_ref_change 的关系是一对多，
     * 但是在同一次分析任务下，每个分支都只会有一次变化记录。
     *
     * @param analyzeId        分析记录 ID
     * @param repositoryId     仓库 ID
     * @param branchIds        仓库下的分支 ID 列表
     * @param branchRefChanges 仓库下每个分支的一次变化记录
     *
     * @return 无需向下游传递数据
     */
    @Override
    public Mono<Void> saveBranchRefChanges(
        final Long                  analyzeId,
        final Long                  repositoryId,
        final List<Long>  branchIds,
        final List<BranchRefChange> branchRefChanges
    )
    {
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
            branchRefChange.setAnalyzeId(analyzeId)
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
             log.info("Call saveBranchRefChanges(), analyze id = {}", analyzeId)
        );
    }
}