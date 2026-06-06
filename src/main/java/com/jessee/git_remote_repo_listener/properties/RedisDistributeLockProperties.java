package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redisson 分布式锁属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.redis-distribute-lock")
public class RedisDistributeLockProperties
{
    /** 分布式锁键前缀 */
    private String keyPrefix;

    /** 获取分布式锁超时时间 */
    private Duration lockWaitTimeout;

    /** 分布式锁有效期时间 */
    private Duration lockLeaseTime;
}