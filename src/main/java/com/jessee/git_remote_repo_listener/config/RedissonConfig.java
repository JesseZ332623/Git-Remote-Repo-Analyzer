package com.jessee.git_remote_repo_listener.config;

import com.jessee.git_remote_repo_listener.properties.RedisProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.redisson.config.FullJitterDelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/** Redisson 客户端配置类。*/
@Configuration
@RequiredArgsConstructor
public class RedissonConfig
{
    private final RedisProperties redisProperties;

    /** Redisson 响应式客户端实例配置。*/
    @Bean
    @Primary
    public RedissonReactiveClient redissonReactiveClient()
    {
        final Config singleServerConfig = new Config();

        // 组合服务器地址
        final String redisAddress
            = "redis://" +
              this.redisProperties.getHost() + ":" +
              this.redisProperties.getPort();

        singleServerConfig.setUsername(this.redisProperties.getUsername());
        singleServerConfig.setPassword(this.redisProperties.getPassword());
        singleServerConfig.setTcpKeepAlive(true);

        singleServerConfig
            .useSingleServer()
            .setAddress(redisAddress)
            .setTimeout(3000)
            .setRetryAttempts(3)
            /*
             * FullJitterDelay（全抖动）
             * 核心思想：“指数退避 + 全抖动”（Exponential Back - off + Full Jitter）。
             *
             * 初始延迟为 baseDelay（如 100ms）；
             * 随着重试次数增加，当前延迟值按指数增长（例如第1次 100ms，第2次 200ms，第3次 400ms…，直到达到 maxDelay 上限）；
             * 每次重试的实际延迟是 [0, 当前延迟值) 内的随机值（“全抖动”指随机范围覆盖整个当前延迟区间）。
             */
            .setRetryDelay(new FullJitterDelay(Duration.ofMillis(200L), Duration.ofSeconds(3L)))
            .setConnectionPoolSize(12)
            .setConnectionMinimumIdleSize(3)
            .setSubscriptionConnectionPoolSize(10)
            .setSubscriptionConnectionMinimumIdleSize(2)
            .setPingConnectionInterval(30000)    // 30 秒一次心跳检查
            .setDnsMonitoringInterval(-1);       // DNS 监控（固定 IP，不需要）

        return
        Redisson.create(singleServerConfig).reactive();
    }
}