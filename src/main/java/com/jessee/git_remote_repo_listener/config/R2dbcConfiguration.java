package com.jessee.git_remote_repo_listener.config;

import com.jessee.git_remote_repo_listener.config.converter.CommitChangeStatusReadConverter;
import com.jessee.git_remote_repo_listener.config.converter.CommitChangeStatusWriteConverter;
import com.jessee.git_remote_repo_listener.properties.R2dbcProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ValidationDepth;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** R2DBC 数据库配置类。*/
@Configuration
@RequiredArgsConstructor
public class R2dbcConfiguration
{
    /** 来自配置文件的 R2DBC 从数据库属性类。*/
    private final R2dbcProperties slaverProperties;

    /** R2DBC Connect Factory 配置。*/
    @Bean(name = "R2dbcConnectionFactory")
    public @NotNull ConnectionFactory connectionFactory()
    {
        final String connectionURL
            = String.format(
                "r2dbc:mysql://%s:%s@%s:%d/%s?serverTimezone=Asia/Shanghai" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true"              +
                "&characterEncoding=UTF8"       +
                "&sslMode=preferred"            +
                "&connectTimeout=PT10S"         +
                "&socketTimeout=PT30S"          +
                "&tcpKeepAlive=true",
                slaverProperties.getUser(),
                URLEncoder.encode(slaverProperties.getPassword(), StandardCharsets.UTF_8),
                slaverProperties.getHost(),
                slaverProperties.getPort(),
                slaverProperties.getDefaultSchema()
        );

        final ConnectionFactory connectionFactory = ConnectionFactories.get(connectionURL);

        // 配置连接池
        final ConnectionPoolConfiguration poolConfiguration
            = ConnectionPoolConfiguration.builder()
                .name("sql-monitor-r2dbc-pool")   // 连接池名
                .connectionFactory(connectionFactory)
                .validationQuery("SELECT 1")             // 连接验证查询语句
                .validationDepth(ValidationDepth.REMOTE) // 连接验证深度（远程）
                .initialSize(5)                          // 初始连接池大小
                .minIdle(2)                              // 最低闲置连接数 (r2dbc-pool 0.9+)
                .maxSize(15)                             // 最大连接池大小
                .backgroundEvictionInterval(Duration.ofSeconds(30L)) // 失效连接检查间隔
                .maxIdleTime(Duration.ofMinutes(5L))                 // 连接最大闲置时间
                .maxLifeTime(Duration.ofMinutes(30L))                // 连接最大存活时间
                .maxAcquireTime(Duration.ofSeconds(10L))             // 获取连接期限时间
                .acquireRetry(5)                       // 获取连接失败最多重试次数
                .maxCreateConnectionTime(Duration.ofSeconds(10L))   // 建立单个连接期限时间
                .registerJmx(true)                                  // 将本连接池注册到 JMX，方便观察调试
                .build();

        return new ConnectionPool(poolConfiguration);
    }

    /** R2DBC 数据库客户端配置。*/
    @Bean("R2dbcDatabaseClient")
    public @NotNull DatabaseClient
    databaseClient(
        @Autowired
        @Qualifier("R2dbcConnectionFactory")
        final ConnectionFactory connectionFactory
    )
    {
        return
        DatabaseClient.builder()
            .connectionFactory(connectionFactory)
            .build();
    }

    /** R2DBC 事务管理器配置。*/
    @Bean
    public ReactiveTransactionManager transactionManager(
        @Qualifier("R2dbcConnectionFactory")
        ConnectionFactory connectionFactory
    )
    {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /** R2DBC 事务操作器。*/
    @Bean("R2dbcTransactionalOperator")
    public TransactionalOperator
    transactionalOperator(ReactiveTransactionManager transactionManager)
    {
        return
        TransactionalOperator.create(transactionManager);
    }

    /**  R2DBC 自定义转换器配置。*/
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions()
    {
        final List<Object> converters
            = Arrays.asList(
                new CommitChangeStatusReadConverter(),
                new CommitChangeStatusWriteConverter()
        );

        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
    }
}