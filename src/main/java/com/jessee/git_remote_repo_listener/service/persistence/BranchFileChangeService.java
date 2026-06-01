package com.jessee.git_remote_repo_listener.service.persistence;

import com.jessee.git_remote_repo_listener.pojo.FileChange;
import reactor.core.publisher.Mono;

import java.util.List;

/** 远程仓库分析分支文件变更数据服务接口。*/
public interface BranchFileChangeService
{
    /**
     * 往 branch_file_change 表写入每个分支的文件提交变化数据。
     *
     * @param analyzeId       分析记录 ID
     * @param repositoryId    仓库 ID
     * @param branchId        仓库下的分支 ID
     * @param fileChanges     本次分析获取的本分支文件提交变化列表
     *
     * @return 无需向下游传递数据
     */
    Mono<Void> saveBranchFileChanges(
        final Long             analyzeId,
        final Long             repositoryId,
        final Long             branchId,
        final List<FileChange> fileChanges
    );
}