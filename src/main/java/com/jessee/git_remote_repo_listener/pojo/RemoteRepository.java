package com.jessee.git_remote_repo_listener.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

import static java.lang.String.format;

/** 表示一个代码仓库的 POJO。*/
@Value
@Builder
@ToString
@EqualsAndHashCode
@JsonDeserialize(builder = RemoteRepository.RemoteRepositoryBuilder.class)
public class RemoteRepository
{
    /** 本地仓库绝对路径。*/
    String path;

    /** 远程仓库名。*/
    String remote;

    @JsonCreator
    public RemoteRepository(
        @JsonProperty("path")   String path,
        @JsonProperty("remote") String remote
    )
    {
        this.path   = path;
        this.remote = remote;
    }

    public static RemoteRepository
    fromCache(Map.Entry<Object, Object> entry)
    {
        return new RemoteRepository(
            String.valueOf(entry.getKey()),
            String.valueOf(entry.getValue())
        );
    }

    /**
     * 获取整个路径中最后一段目录名。
     * 例：D:\a\b\c\ai-by-training -> ai-by-training
     *（方法名带了 get，需要 {@link JsonIgnore} 注解来避免意外的序列化）
     */
    @JsonIgnore
    public String getDirectoryName() {
        return Path.of(this.path).normalize().getFileName().toString();
    }

    /**
     * 检查本 RemoteRepository 实例下的路径字符串是否合法。
     *
     * @throws IllegalArgumentException
     *         在构造 {@link Path} 的时候遇到非法路径字符串
     *         或者文件不存在时在管道中传递本异常
     *
     * @return 仅做检查，不向下游发布任何数据
     */
    public Mono<Void> checkPath()
    {
        return
        Mono.fromCallable(() -> Path.of(this.path).normalize())
            .filter(Files::exists)
            .switchIfEmpty(
                Mono.error(
                    new IllegalArgumentException(
                        format(
                            "Repository path %s not exist in this file system!",
                            this.path
                        )
                    )
                )
            )
            .onErrorResume(
                InvalidPathException.class,
                (exception) ->
                    Mono.error(new IllegalArgumentException(exception))
            )
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }
}