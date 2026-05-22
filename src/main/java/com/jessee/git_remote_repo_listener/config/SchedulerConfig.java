package com.jessee.git_remote_repo_listener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** 分析作业专用调度器配置类。*/
@Configuration
public class SchedulerConfig
{
    @Bean(name = "GitWorkerScheduler")
    public Scheduler gitWorkerScheduler()
    {
        final ThreadFactory vthreadFactory
            = Thread.ofVirtual()
                    .name("git-worker-", 0)
                    .factory();

        return Schedulers.fromExecutorService(
            Executors.newThreadPerTaskExecutor(vthreadFactory)
        );
    }
}