package com.jessee.git_remote_repo_listener.scheduler_tasks;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.service.AnalyzeReportEmailSender;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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

    /**
     * 向缓存中的每个收件人发送所有仓库的变更报告，
     * 每天早上 9 点 05 分和系下午的   5 点 55 分各执行一次。
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

            return
            Flux.fromIterable(recipients)
                .flatMap((recipient) ->
                    Flux.fromIterable(analyzeResult.getAyalyzResult())
                        .flatMap((branchFileChanges) ->
                            this.analyzeReportEmailSender
                                .send(recipient, analyzeResult.getAnalyzDateTime(), branchFileChanges)
                        )
                ).collectList();
        }).subscribe();
    }
}