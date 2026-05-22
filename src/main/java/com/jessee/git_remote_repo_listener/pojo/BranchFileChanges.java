package com.jessee.git_remote_repo_listener.pojo;

import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import lombok.*;

import java.util.List;

/** 整个远程仓库下所有分支的文件提交变化。*/
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class BranchFileChanges
{
    /** 仓库信息 */
    private RepoPathProperties.RepoConfig repoConfig;

    /** 远程仓库下每个分支的文件变更状态 */
    private List<BranchFileChange> branchFileChanges;

    public static BranchFileChanges
    of(RepoPathProperties.RepoConfig repoConfig, List<BranchFileChange> branchFileChanges) {
        return new BranchFileChanges(repoConfig, branchFileChanges);
    }
}