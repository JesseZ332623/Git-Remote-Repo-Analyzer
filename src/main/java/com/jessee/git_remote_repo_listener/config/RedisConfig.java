package com.jessee.git_remote_repo_listener.config;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.properties.RedisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

/** Redis 客户端配置类。*/
@Configuration
@RequiredArgsConstructor
public class RedisConfig
{
    private final RedisProperties redisProperties;

    /** Redis 响应式连接工厂配置类。*/
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory
    reactiveRedisConnectionFactory()
    {
        // 1. 创建独立 Redis 配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(this.redisProperties.getHost());       // Redis 地址
        config.setUsername(this.redisProperties.getUsername());   // Redis 用户名
        config.setPort(this.redisProperties.getPort());           // Redis 端口
        config.setDatabase(0);                                    // 明确指定数据库

        // 密码
        config.setPassword(
            RedisPassword.of(
                this.redisProperties.getPassword()
            )
        );

        // 2. 创建客户端配置
        LettuceClientConfiguration clientConfig
            = LettuceClientConfiguration.builder()
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    // 客户端检查到 “协议错误” 时，不再执行重连操作。
                    .suspendReconnectOnProtocolFailure(true)
                    .disconnectedBehavior(
                        // 断开连接时拒绝接收命令
                        ClientOptions.DisconnectedBehavior.REJECT_COMMANDS
                    )
                    .socketOptions(
                        SocketOptions.builder()
                            .connectTimeout(Duration.ofSeconds(2L)) // 连接超时
                            .keepAlive(true) // 自动管理 TCP 连接存活
                            .build()
                    )
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(1L)) // 操作超时
                            .build()
                    ).build()
            )
            .commandTimeout(Duration.ofSeconds(1L))  // 命令超时时间
            .shutdownTimeout(Duration.ofSeconds(5L))  // 关闭超时时间
            .build();

        // 3. 创建连接工厂
        LettuceConnectionFactory connectionFactory
            = new LettuceConnectionFactory(config, clientConfig);

        connectionFactory.setValidateConnection(false); // 禁用连接验证

        return connectionFactory;
    }

    /**
     * Redis 响应式模板的构建。
     *
     * @param factory Redis 连接工厂，
     *                Spring 会自动读取配置文件中的属性去构建。
     * @return 配置好的 Redis 响应式模板
     */
    @Bean
    @Primary
    public ReactiveRedisTemplate<String, Object>
    reactiveRedisTemplate(
        final ReactiveRedisConnectionFactory factory,
        @Qualifier("RedisObjectMapper")
        final ObjectMapper redisObjectMapper
    )
    {
        /* Redis 键使用字符串进行序列化。 */
        RedisSerializer<String> keySerializer
            = new StringRedisSerializer();

        /* Redis 值使用 Jackson 进行序列化。 */
        RedisSerializer<Object> valueSerializer
            = new GenericJacksonJsonRedisSerializer(redisObjectMapper);

        /* Redis Hash Key / Value 的序列化。 */
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object>
            builder = RedisSerializationContext.newSerializationContext(keySerializer);

        /* 创建 Redis 序列化上下文，设置序列化方式。 */
        RedisSerializationContext<String, Object> context
            = builder.value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        /* 根据上述配置构建 ReactiveRedisTemplate。 */
        return new ReactiveRedisTemplate<>(factory, context);
    }

    /** 专用于 BranchFileChange 的 ReactiveRedisTemplate。*/
    @Bean
    public ReactiveRedisTemplate<String, BranchFileChange>
    branchFileChangeRedisTemplate(
        final ReactiveRedisConnectionFactory factory,
        @Qualifier("RedisObjectMapper")
        final ObjectMapper redisObjectMapper
    )
    {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();

        // 关键：使用具体类型的 Serializer
        RedisSerializer<BranchFileChange> valueSerializer =
            new JacksonJsonRedisSerializer<>(redisObjectMapper, BranchFileChange.class);

        RedisSerializationContext<String, BranchFileChange> context
            = RedisSerializationContext.<String, BranchFileChange>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}