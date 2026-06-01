package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.BranchFileChangeEntity;
import com.jessee.git_remote_repo_listener.mapper.BranchFileChangeMapper;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import com.jessee.git_remote_repo_listener.service.persistence.BranchFileChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** 远程仓库分析分支文件变更数据服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchFileChangeServiceImpl implements BranchFileChangeService
{
    /** 分支文件提交变更数据映射接口。*/
    private final BranchFileChangeMapper branchFileChangeMapper;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /**
     * 往 branch_file_change 表写入每个分支的文件提交变化数据。
     *
     * @param analyzeId    分析记录 ID
     * @param repositoryId 仓库 ID
     * @param branchId     仓库下的分支 ID
     * @param fileChanges  本次分析获取的本分支文件提交变化列表
     *
     * @return 无需向下游传递数据
     */
    @Override
    public Mono<Void>
    saveBranchFileChanges(
        final Long              analyzeId,
        final Long              repositoryId,
        final Long              branchId,
        final List<FileChange>  fileChanges
    )
    {
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
            branchFileChange.setAnalyzeId(analyzeId)
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
                  "Call saveBranchFileChanges() analyze id = {}",
                  analyzeId
              )
        );
    }
}