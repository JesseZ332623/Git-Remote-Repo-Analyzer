package com.jessee.git_remote_repo_listener.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/** JSON 格式美化工具类。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class PrettyJSONPrinter
{
    /** 启用了美化格式的 JSON 对象映射器。*/
    @Getter
    private final static
    ObjectMapper mapper
        = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** 将任意对象序列化成美化格式的 JSON 字符串。*/
    public static String
    getPrettyFormatJSON(@NotNull Object instance)
    {
        try
        {
            return
            mapper.writeValueAsString(instance);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    /** 获取美化格式后的 JSON 字符串。*/
    public static String
    getPrettyFormatJSON(@NotNull String json)
    {
        try
        {
            return
            mapper.writeValueAsString(mapper.readTree(json));
        }
        catch (JsonProcessingException exception)
        {
            log.error("{}", exception.getMessage());
            return json;
        }
    }
}