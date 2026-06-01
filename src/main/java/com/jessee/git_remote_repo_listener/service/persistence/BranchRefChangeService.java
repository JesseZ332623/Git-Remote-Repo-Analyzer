package com.jessee.git_remote_repo_listener.service.persistence;

import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import reactor.core.publisher.Mono;

import java.util.List;

/** 远程仓库分析分支引用变化数据服务接口。*/
public interface BranchRefChangeService
{
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
    Mono<Void> saveBranchRefChanges(
        final Long                  analyzeId,
        final Long                  repositoryId,
        final List<Long>            branchIds,
        final List<BranchRefChange> branchRefChanges
    );
}