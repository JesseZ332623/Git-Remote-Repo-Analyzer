package com.jessee.git_remote_repo_listener.pojo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表示一次远程仓库分析结果的 POJO，
 * 数据库，缓存，邮件各模块需要一个统一的时间。
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResult
{
    /** 本次分析的时间（精确到分钟）*/
    private LocalDateTime analyzDateTime;

    /** 分析结果列表 */
    private List<BranchFileChanges> ayalyzResult;
}