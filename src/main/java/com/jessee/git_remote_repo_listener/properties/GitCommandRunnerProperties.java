package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Git 命令执行器配置参数属性类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.git-command-runner")
public class GitCommandRunnerProperties
{
    /** 进程最大等待时间（单位：秒）*/
    private int processMaxWaitTime;

    /** 异步读取的缓冲区大小（单位：字节）*/
    private int asyncBufferSize;

    /** 重试策略配置 */
    private Retry retry;

    @Data
    @NoArgsConstructor
    public static class Retry
    {
        /** 最多查尝试次数 */
        private int maxAttempts;

        /** 指数退避起始时间 */
        private Duration minBackoff;

        /** 指数退避封顶时间 */
        private Duration maxBackoff;

        /** 随机抖动因子 */
        private double jitterFactor;
    }
}