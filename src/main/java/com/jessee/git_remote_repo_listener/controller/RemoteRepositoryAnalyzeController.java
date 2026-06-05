package com.jessee.git_remote_repo_listener.controller;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import com.jessee.git_remote_repo_listener.component.RedissonLocker;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.properties.EmailSendConcurrentProperties;
import com.jessee.git_remote_repo_listener.response.CustomizedResponse;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static com.jessee.git_remote_repo_listener.response.CustomizedResponse.responseOf;

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

    /** Redisson 分布式锁封装工具组件接口。*/
    private final RedissonLocker redissonLocker;

    /** 分析记录邮件发送限流配置。*/
    private final
    EmailSendConcurrentProperties emailSendConcurrentProperties;

    @PostMapping(path = "/execute")
    public Mono<CustomizedResponse<Object>>
    remoteRepoAnalysis(final ServerHttpResponse response)
    {
        // 如果检查到本任务正在被定时任务执行执行，直接跳过即可。
//        if (!this.runningFlag.compareAndSet(false, true))
//        {
//            return responseOf(
//                response,
//                HttpStatus.CONFLICT,
//                "The remote repository analysis task is currently being executed.",
//                null
//            );
//        }

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
                response,
                HttpStatus.OK,
                "The remote repository analysis task manually execute complete.",
                null
            )
        )
        .as(this.redissonLocker.lockAround(true));
    }
}