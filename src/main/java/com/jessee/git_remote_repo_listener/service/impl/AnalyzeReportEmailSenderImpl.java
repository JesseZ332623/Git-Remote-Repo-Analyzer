package com.jessee.git_remote_repo_listener.service.impl;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import io.github.jessez332623.reactive_email_sender.ReactiveEmailSender;
import io.github.jessez332623.reactive_email_sender.dto.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 分析报告邮件发送器实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeReportEmailSenderImpl implements AnalyzeReportEmailSender
{
    /** 仓库无变更报告模板名。*/
    private static final String
    EMPTY_REPORT_TEMPLATE =  "remote-repo-nochange-report";

    /** 仓库变更报告模板名。*/
    private static final String
    REPORT_TEMPLATE = "remote-repo-change-report";

    /** 仓库变更报告邮件标题。*/
    private static final String
    REPORT_SUBJECT = "Git 远程仓库变更报告";

    /** 响应式邮件发送器接口。*/
    private final ReactiveEmailSender emailSender;

    /** Spring 响应式模板引擎（用于 Thymeleaf 框架的 HTML 渲染）。*/
    private final SpringWebFluxTemplateEngine templateEngine;

    /** 获取当前时间字符串（带时区）。*/
    private static String
    zonedDateTime(LocalDateTime analyzDateTime)
    {
        return
        analyzDateTime
            .atZone(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private Mono<Void>
    sendAnalysisReportMail(String repositoryName, String recipient, String htmlMainText)
    {
        return
        EmailContent.fromHtml(recipient, REPORT_SUBJECT, htmlMainText)
            .flatMap(this.emailSender::sendEmail)
            .doOnSuccess((ignore) ->
                log.info(
                    "Complete to send repository {} analyze report email to {}",
                    repositoryName, recipient
                )
            );
        // 邮件依赖会抛出自己的异常，后续可以转化成定时任务专用的异常
        // .onErrorResume();
    }

    private Mono<String>
    generateAnalysisReport(LocalDateTime analyzDateTime, BranchFileChanges branchFileChanges)
    {
        final String repositoryName
            = branchFileChanges.getRemoteRepository().getPath()
            + " "
            + branchFileChanges.getRemoteRepository().getRemote();

        final List<BranchFileChange> changes
            = branchFileChanges.getBranchFileChanges();

        // 如果远程仓库下每个分支的文件变更状态列表为空，
        // 意味着本次分析下仓库数据同步，直接返回空报告模板即可。
        if (changes.isEmpty())
        {
            final Map<String, Object> noChangeReportData
                = Map.of(
                "repoName",   repositoryName,
                "reportDate", zonedDateTime(analyzDateTime)
            );

            return
            Mono.fromCallable(() ->
                this.templateEngine.process(
                    EMPTY_REPORT_TEMPLATE,
                    new Context(Locale.getDefault(), noChangeReportData)
                )
            );
        }

        final Map<String, Object> changeReportData = new HashMap<>();

        changeReportData.put("repoName", repositoryName);
        changeReportData.put("reportDate", zonedDateTime(analyzDateTime));
        changeReportData.put("branchFileChanges", branchFileChanges);

        return
        Mono.fromCallable(() ->
            this.templateEngine.process(
                REPORT_TEMPLATE,
                new Context(Locale.getDefault(), changeReportData)
            )
        );
    }

    @Override
    public Mono<Void> send(
        final String            recipient,
        final LocalDateTime     analyzDateTime,
        final BranchFileChanges branchFileChanges
    )
    {
        return
        this.generateAnalysisReport(analyzDateTime, branchFileChanges)
            .flatMap((htmlMainText) ->
                this.sendAnalysisReportMail(
                    branchFileChanges.getRemoteRepository().getDirectoryName(),
                    recipient,
                    htmlMainText
                )
            );
    }
}
