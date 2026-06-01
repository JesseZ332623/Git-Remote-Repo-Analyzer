package com.jessee.git_remote_repo_listener.scheduler_tasks;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.properties.EmailSendConcurrentProperties;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** 远程仓库分析定时任务服务类。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzeSchedulerTasks
{
    /** 邮件收件人缓存类接口。*/
    private final EmailRecipientCacher cacher;

    /** 远程仓库分析服务类接口。*/
    private final RemoteRepositoryAnalyzerService remoteRepositoryAnalyzerService;

    /** 分析报告邮件发送器接口。*/
    private final AnalyzeReportEmailSender analyzeReportEmailSender;

    /** 分析记录邮件发送限流配置。*/
    private final EmailSendConcurrentProperties properties;

    /** 表示异步任务是否在执行的标志位。*/
    @Qualifier(value = "AnalyzeRunningFlag")
    private final AtomicBoolean runningFlag;

    /**
     * 向缓存中的每个收件人发送所有仓库的变更报告，
     * 每天早上 9 点 05 分和系下午的 5 点 55 分各执行一次。
     */
    @Scheduled(
        cron = "0 5 9 * * ?",
        zone = "Asia/Shanghai"
    )
    @Scheduled(
        cron = "0 55 17 * * ?",
        zone = "Asia/Shanghai"
    )
    public void remoteRepoAnalysisTask()
    {
        // 如果检查到本任务正在手动执行，直接跳过即可。
        if (!this.runningFlag.compareAndSet(false, true))
        {
            log.warn(
                "The remote warehouse analysis task is currently being executed, " +
                "skipping this scheduled task."
            );
        }

        Mono.zip(
            this.cacher.getAllRecipientAddress(),
            this.remoteRepositoryAnalyzerService.doAnalysis()
        ).flatMap((tuple) -> {
            final List<String> recipients     = tuple.getT1();
            final AnalyzeResult analyzeResult = tuple.getT2();
            final int maxRecipients           = this.properties.getMaxRecipients();
            final Duration eachSendDelay      = this.properties.getEachSendDelay();

            /*
             * 将于同一批分析结果并行的传递给每一个下游的收件人（限制并发量，每次处理 max-recipients 个收件人），
             * 但给每个收件人发件时需要串行，每封邮件间隔 100 毫秒，
             * 避免过快发送触发邮件服务的拦截导致丢件。
             */
            return
            Flux.fromIterable(recipients)
                .flatMap((recipient) ->
                    Flux.fromIterable(analyzeResult.getAyalyzResult())
                        .delayElements(eachSendDelay)
                        .concatMap((branchFileChanges) ->
                            this.analyzeReportEmailSender
                                .send(recipient, analyzeResult.getAnalyzDateTime(), branchFileChanges)
                        ), maxRecipients
                ).collectList();
        })
        .doFinally((ignore) -> this.runningFlag.set(false))
        .subscribe();
    }
}