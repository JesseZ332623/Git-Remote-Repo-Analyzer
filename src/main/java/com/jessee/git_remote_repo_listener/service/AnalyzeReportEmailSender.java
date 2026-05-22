package com.jessee.git_remote_repo_listener.service;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** 分析报告邮件发送器接口。*/
public interface AnalyzeReportEmailSender
{
    /**
     * 发送一封远程仓库分析报告邮件。
     *
     * @param recipient         收件人邮箱地址
     * @param analyzDateTime    分析执行日期
     * @param branchFileChanges 远程仓库所有分支的文件变化
     */
    Mono<Void>
    send(String recipient, LocalDateTime analyzDateTime, BranchFileChanges branchFileChanges);
}