package com.jessee.git_remote_repo_listener.pojo;

import com.jessee.git_remote_repo_listener.constant.CommitChangeStatus;
import lombok.*;

/** 本 POJO 表示分支内一个文件的变更信息。*/
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FileChange
{
    /** 变更状态。*/
    private CommitChangeStatus status;

    /** 相似度，非 RENAME 和 COPY 的填 null。*/
    private Integer similarity;

    /** 旧路径。*/
    private String oldPath;

    /** 新路径。*/
    private String newPath;

    /** 在邮件通知时，将变更转化成字符串描述。*/
    public String toDescription()
    {
        return switch (this.status)
        {
            case RENAME ->
                String.format(
                    "(%s -> %s 相似度 %d)",
                    this.oldPath, this.newPath, this.similarity
                );

            case COPY ->
                String.format(
                    "(复制，%s -> %s 相似度 %d)",
                    this.oldPath, this.newPath, this.similarity
                );

            default -> this.newPath;
        };
    }

    /** 从提交变更状态中解析相似度。*/
    private static Integer
    parseSimilarity(String rawStatus)
    {
        return
        (rawStatus.length() > 1)
            ? Integer.valueOf(rawStatus.substring(1))
            : null;
    }

    /**
     * 解析 git diff --status-name 的一行输出为本对象实例。
     * 该命令的输出示例如下：
     *
     * <pre>
     * D       platform/employee/src/main/java/com/boyuan/employee/package-info.java
     * R095    platform/employee/pom.xml       platform/frontend/pom.xml
     * R064    platform/employee/src/main/java/com/boyuan/employee/config/EmployeeConfig.java  platform/frontend/src/main/java/com/boyuan/frontend/config/FrontendConfig.java
     * </pre>
     *
     * 按照 git diff 的文档说明：
     *
     * <pre>
     * The status letter is followed by a tab and the filename.
     * For rename or copy, the status letter is followed by a tab,
     * the original filename, a tab, and the new filename.
     * </pre>
     *
     * 所以确定格式就是：状态码 + \t + 文件路径（+\t + 新路径），
     * 因此按照 tab 分割即可。
     */
    public static FileChange fromDiffLine(String line)
    {
        final String[] parts = line.split("\t");

        final CommitChangeStatus changeStatus
            = CommitChangeStatus.fromStatus(parts[0]);

        final Integer similarity = parseSimilarity(parts[0]);

        return switch (changeStatus)
        {
            case RENAME, COPY ->
                new FileChange(
                    changeStatus,
                    similarity,
                    parts[1], parts[2]
                );

            default ->
                new FileChange(
                    changeStatus,
                    similarity,
                    null, parts[1]
                );
        };
    }
}
