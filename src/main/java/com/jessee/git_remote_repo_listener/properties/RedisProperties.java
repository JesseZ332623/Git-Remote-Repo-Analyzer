package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Redis 属性配置类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties
{
    private String host;
    private int    port;
    private String username;
    private String password;
}