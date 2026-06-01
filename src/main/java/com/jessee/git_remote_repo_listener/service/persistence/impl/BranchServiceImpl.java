package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.BranchEntity;
import com.jessee.git_remote_repo_listener.mapper.BranchMapper;
import com.jessee.git_remote_repo_listener.service.persistence.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/** 远程仓库分析仓库下分支服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 仓库分支数据映射接口。*/
    private final BranchMapper branchMapper;

    /**
     * 往 branch 表写入指定仓库下的分支数据。
     *
     * @param analyzeId       分析记录 ID
     * @param repositoryId    仓库 ID
     * @param branchNames     分支名
     *
     * @return 向下游传递写入的所有分支记录的 ID 列表
     */
    @Override
    public Mono<List<Long>> saveBranchs(
        final Long          analyzeId,
        final Long          repositoryId,
        final List<String>  branchNames
    )
    {
        // 如果本次分析检查到本仓库的所有分支状态不变，
        // 则 branchNames 为空，不向下游发布任何数据。
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
            branch.setAnalyzeId(analyzeId)
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
                log.info("Call saveBranchs(), analyze id = {}", analyzeId)
            );
    }
}