package com.jessee.git_remote_repo_listener.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交状态枚举。
 * 一次 git diff --name-status prev_hash..curr_hash 的输出示例如下：
 *
 * <pre>
 * $ git diff --name-status oldHash..newHash
 *
 * M       src/main/UserService.java        # 修改
 * A       src/main/PaymentService.java     # 新增
 * D       src/main/OldLegacy.java          # 删除
 * R100    src/main/OldName.java            # 重命名（完全没改内容）
 *         src/main/NewName.java
 * R087    src/main/UserUtil.java           # 重命名+小改
 *         src/main/UserHelper.java
 * C100    src/main/Original.java           # 复制
 *         src/main/Copy.java
 * T       src/main/test.c                  # 文件类型变更
 *         src/main/test.java
 *
 * U       # 冲突未解决
 *
 * X       # 未知，兜底策略，极其罕见
 * </pre>
 *
 *
 * 其中，状态 R 和 C 后面的数字代表相似度，
 * 比如 R087 表示文件重命名并修改了 23% 的内容。
 */
@Slf4j
@Getter
@ToString
@RequiredArgsConstructor
public enum CommitChangeStatus
{
    MODIFY("M",  "修改"),
    ADD("A",     "新增"),
    DELETE("D",  "删除"),
    RENAME("R",  "重命名"),
    COPY("C",    "复制"),
    TYPE("T",    "文件类型变更"),
    UNMERGE("U", "冲突未解决"),
    UNKNOWN("X", "未知");

    private final String status;
    private final String desc;

    public static CommitChangeStatus fromStatus(String rawStatus)
    {
        final String code = rawStatus.substring(0, 1);

        for (CommitChangeStatus commitChangeStatus : CommitChangeStatus.values())
        {
            if (commitChangeStatus.getStatus().equals(code)) {
                return commitChangeStatus;
            }
        }

        log.warn("Unexprected commit status {}.", rawStatus);

        return UNKNOWN;
    }
}