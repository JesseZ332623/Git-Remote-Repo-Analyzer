package com.jessee.git_remote_repo_listener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

/** 异步工具组件配置类。*/
@Configuration
public class AsyncConfig
{
    /** 表示异步分析任务是否在执行的标志位。*/
    @Bean("AnalyzeRunningFlag")
    public AtomicBoolean analyzeRunningFlag() {
        return new AtomicBoolean(false);
    }
}
