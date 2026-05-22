package com.jessee.git_remote_repo_listener.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/** Jackson 配置类。*/
@Configuration
public class JacksonConfig
{
    /** Spring 默认使用的对象映射器。*/
    @Bean
    @Primary
    public com.fasterxml.jackson.databind.ObjectMapper
    defaultObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    /** Redis 序列化专用的对象映射器。*/
    @Bean(name = "RedisObjectMapper")
    public ObjectMapper redisObjectMapper()
    {
        final PolymorphicTypeValidator ptv
            = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.jessee.")
                .allowIfBaseType("java.util.")
                .allowIfBaseType("java.lang.")
                .build();

        return
        JsonMapper.builder()
            .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .removeAllModules()
            .build();
    }
}