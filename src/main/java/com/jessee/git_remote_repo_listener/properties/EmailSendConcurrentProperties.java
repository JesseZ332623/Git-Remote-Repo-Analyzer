package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 分析记录邮件发送限流属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.email-send-concurrent")
public class EmailSendConcurrentProperties
{
    /** 最多同时处理的收件人数量。*/
    private Integer maxRecipients;

    /** 发送邮件前需要延迟的时间。*/
    private Duration eachSendDelay;
}
