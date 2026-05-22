package com.jessee.git_remote_repo_listener.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** R2DBC 属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.r2dbc")
public class R2dbcProperties
{
    private String host;
    private int    port;
    private String user;
    private String password;
    private String defaultSchema;
}
