package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 数据库的限流背压配置数据库的限流背压属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.database-concurrent")
public class DatabaseConcurrentProperties
{
    /** 限制仓库并发数 */
    private Integer maxRepos;

    /** 限制文件变更并发数 */
    private Integer maxFileChanges;
}