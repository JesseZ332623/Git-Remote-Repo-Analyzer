package com.jessee.git_remote_repo_listener.pojo;

import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** 单个分支下的提交文件变化。*/
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class BranchFileChange
{
    /** 分支名 */
    private String branchName;

    /** 分支的状态变化 */
    private BranchRefChange branchRefChange;

    /** 分支下的文件提交状态列表 */
    private List<FileChange> fileChanges;

    @Contract("_, _ , _-> new")
    public static @NonNull BranchFileChange
    of(String branchName, BranchRefChange branchRefChange, List<FileChange> fileChanges) {
        return new BranchFileChange(branchName, branchRefChange, fileChanges);
    }
}