package com.jessee.git_remote_repo_listener.pojo;

import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import lombok.*;

import java.util.Objects;

/**
 * 表示远程分支在两次 fetch 快照之间的引用变化。
 * 包含变更前后的 commit hash 以及变更类型（新增/更新/删除）。
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class BranchRefChange
{
    /** 更新前的最新提交哈希。*/
    private String prevLatestCommitHash;

    /** 更新后的最新提交哈希。*/
    private String latestCommitHash;

    /** 远程分支变更状态。*/
    private RemoteChangeStaus status;

    /** 按前后两次分支哈希构造。*/
    public static BranchRefChange of(String prevHash, String currHash)
    {
        if (Objects.isNull(prevHash))
        {
            return new BranchRefChange(
                null, currHash,
                RemoteChangeStaus.NEW
            );
        }
        else if (Objects.isNull(currHash))
        {
            return new BranchRefChange(
                prevHash, null,
                RemoteChangeStaus.DELETE
            );
        }
        else if (prevHash.equals(currHash))
        {
            return new BranchRefChange(
                prevHash, currHash,
                RemoteChangeStaus.IMMUTABLE
            );
        }

        return new BranchRefChange(
            prevHash, currHash,
            RemoteChangeStaus.UPDATE
        );
    }
}