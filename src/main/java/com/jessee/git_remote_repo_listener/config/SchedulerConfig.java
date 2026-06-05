package com.jessee.git_remote_repo_listener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** 分析作业专用调度器配置类。*/
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer
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

    @Bean(name = "GitOutputReaderExecuterService")
    public ExecutorService gitOutputReaderExecuterService()
    {
        // 虚拟线程工厂
        final ThreadFactory factory
            = Thread.ofVirtual()
                    .name("git-output-reader-", 0)  // 自动编号
                    .factory();

        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler()
    {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(8);                          // 可根据实际定时任务数量调整
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRemoveOnCancelPolicy(true);

        return scheduler;
    }
}