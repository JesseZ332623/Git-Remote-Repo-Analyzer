package com.jessee.git_remote_repo_listener.scheduler_tasks;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import com.jessee.git_remote_repo_listener.component.RedissonLocker;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.properties.EmailSendConcurrentProperties;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/** 远程仓库分析定时任务服务类。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzeSchedulerTasks
{
    /** 手动 subscribe() 后处理 onError() 信号的回调函数。*/
    private static final Consumer<? super Throwable>
    ERROR_CONSMER = (error) ->
        log.error("Execute remote repository analysis scheduled task failed...", error);

    /** 手动 subscribe() 后处理 onComplete() 信号的回调函数。*/
    private static final Consumer<? super Void>
    SUCCESS_CONSUMER = (ignore) ->
        log.info("Execute remote repository analysis scheduled task success!");

    /**
     * 手动 subscribe() 后马上执行的回调函数，
     * {@link Subscription} 负责处理背压和取消，在这里暂时用不到。
     */
    private static final Consumer<? super Subscription>
    SUBSCRIPTION_CONSUER = (ignore) ->
        log.info("Start to execute remote repository analysis scheduled task.");

    /** 邮件收件人缓存类接口。*/
    private final EmailRecipientCacher cacher;

    /** 远程仓库分析服务类接口。*/
    private final RemoteRepositoryAnalyzerService remoteRepositoryAnalyzerService;

    /** 分析报告邮件发送器接口。*/
    private final AnalyzeReportEmailSender analyzeReportEmailSender;

    /** Redisson 分布式锁封装工具组件接口。*/
    private final RedissonLocker redissonLocker;

    /** 分析记录邮件发送限流配置。*/
    private final EmailSendConcurrentProperties properties;

    /**
     * 向缓存中的每个收件人发送所有仓库的变更报告，
     * 每天早上 9 点 05 分和系下午的 5 点 55 分各执行一次。
     * 在定时任务场景下，订阅的回调处理处理任务就不是框架代劳了，
     * 我需要自己手动注册回调函数，具体描述如下所式：
     *
     * <table>
     *     <caption>subscribe() 回调方法说明</caption>
     *     <thead>
     *         <tr>
     *             <th>回调函数接口</th>
     *             <th>触发时机</th>
     *             <th>执行次数</th>
     *         </tr>
     *    </thead>
     *    <tbody>
     *        <tr>
     *            <td>consumer</td>
     *                <td>每个数据元素</td>
     *                <td>0 ~ N 次</td>
     *        </tr>
     *        <tr>
     *            <td>errorConsumer</td>
     *            <td>发生异常</td>
     *            <td>0 或 1 次</td>
     *        </tr>
     *        <tr>
     *            <td>completeConsumer</td>
     *            <td>正常完成</td>
     *            <td>0 或 1 次</td>
     *        </tr>
     *        <tr>
     *            <td>subscriptionConsumer</td>
     *            <td>订阅建立时</td>
     *            <td>0 或 1 次</td>
     *        </tr>
     *     </tbody>
     * </table>
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
                )
                .then();
        })
        .as(this.redissonLocker.lockAround(true))
        .doOnSubscribe(SUBSCRIPTION_CONSUER)
        .doOnSuccess(SUCCESS_CONSUMER)
        .doOnError(ERROR_CONSMER)
        .subscribe();
    }
}