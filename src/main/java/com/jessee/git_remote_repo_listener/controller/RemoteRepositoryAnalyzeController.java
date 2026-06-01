package com.jessee.git_remote_repo_listener.controller;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.properties.EmailSendConcurrentProperties;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import com.jessee.git_remote_repo_listener.response.CustomizedResponse;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jessee.git_remote_repo_listener.response.CustomizedResponse.responseOf;
import static java.lang.String.format;

/** 远程仓库分析手动执行控制器。*/
@Slf4j
@RestController
@RequestMapping(path = "/api/analysis")
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzeController
{
    /** 邮件收件人缓存类接口。*/
    private final
    EmailRecipientCacher cacher;

    /** 远程仓库分析服务类接口。*/
    private final
    RemoteRepositoryAnalyzerService remoteRepositoryAnalyzerService;

    /** 分析报告邮件发送器接口。*/
    private final
    AnalyzeReportEmailSender analyzeReportEmailSender;

    /** 本地待分析仓库路径配置类。*/
    private final
    RepoPathProperties repoPathProperties;

    /** 分析记录邮件发送限流配置。*/
    private final
    EmailSendConcurrentProperties emailSendConcurrentProperties;

    /** 表示异步任务是否在执行的标志位。*/
    @Qualifier(value = "AnalyzeRunningFlag")
    private final AtomicBoolean runningFlag;

    @GetMapping(path = "/repos")
    public Mono<CustomizedResponse<RepoPathProperties>> remoteRepos()
    {
        return
        CollectionUtils.isEmpty(this.repoPathProperties.getRepos())
            ? responseOf(
                HttpStatus.NOT_FOUND,
                "No remote repos to be analyzed.",
                null
            )
            : responseOf(
                HttpStatus.OK,
                format("%d remote repos to be analyzed.", this.repoPathProperties.getRepos().size()),
                this.repoPathProperties
            );
    }

    @PostMapping(path = "/execute")
    public Mono<CustomizedResponse<Object>> remoteRepoAnalysis()
    {
        // 如果检查到本任务正在被定时任务执行执行，直接跳过即可。
        if (!this.runningFlag.compareAndSet(false, true))
        {
            return responseOf(
                HttpStatus.CONFLICT,
                "The remote repository analysis task is currently being executed.",
                null
            );
        }

        return
        Mono.zip(
            this.cacher.getAllRecipientAddress(),
            this.remoteRepositoryAnalyzerService.doAnalysis()
        ).flatMap((tuple) -> {
            final List<String> recipients     = tuple.getT1();
            final AnalyzeResult analyzeResult = tuple.getT2();
            final int maxRecipients      = this.emailSendConcurrentProperties.getMaxRecipients();
            final Duration eachSendDelay = this.emailSendConcurrentProperties.getEachSendDelay();

            return
            Flux.fromIterable(recipients)
                .flatMap((recipient)  ->
                    Flux.fromIterable(analyzeResult.getAyalyzResult())
                        .delayElements(eachSendDelay)
                        .concatMap((branchFileChanges) ->
                            this.analyzeReportEmailSender
                                .send(recipient, analyzeResult.getAnalyzDateTime(), branchFileChanges)
                        ), maxRecipients
                ).collectList();
        })
        .then(
            responseOf(
                HttpStatus.OK,
                "The remote repository analysis task manually execute complete.",
                null
            )
        )
        .doFinally((ignore) -> this.runningFlag.set(false));
    }
}