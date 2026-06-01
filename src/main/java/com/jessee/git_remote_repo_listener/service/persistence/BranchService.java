package com.jessee.git_remote_repo_listener.service.persistence;

import reactor.core.publisher.Mono;


import java.util.List;

/** 远程仓库分析仓库下分支服务接口。*/
public interface BranchService
{
    /**
     * 往 branch 表写入指定仓库下的分支数据。
     *
     * @param analyzeId         分析记录 ID
     * @param repositoryId      仓库 ID
     * @param branchNames       分支名
     *
     * @return 向下游传递写入的所有分支记录的 ID 列表
     */
    Mono<List<Long>> saveBranchs(
        final Long          analyzeId,
        final Long          repositoryId,
        final List<String>  branchNames
    );
}