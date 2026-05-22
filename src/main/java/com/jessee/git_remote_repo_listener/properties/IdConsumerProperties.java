package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 全局 ID 消费机属性类。*/
@Data
@ToString
@Component
@EqualsAndHashCode
@ConfigurationProperties(prefix = "app.id-consumer")
public class IdConsumerProperties
{
    /** 全局 ID 缓冲区键。*/
    private String globalIdKey;

    /** 获取 ID 的最大阻塞时间。*/
    private Duration blockTimeout;

    /** 当发现 ID 池枯竭后的重试次数。*/
    private Integer retries;

    /** 指数退避起始时间。*/
    private Duration startBackoff;

    /** 指数退避封顶时间。*/
    private Duration maxBackoff;
}