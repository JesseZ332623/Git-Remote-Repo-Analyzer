package com.jessee.git_remote_repo_listener.component.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.properties.IdConsumerProperties;
import io.github.jessez332623.reactive_luascript_reader.LuaScriptReader;
import io.github.jessez332623.reactive_luascript_reader.impl.LuaOperatorResult;
import io.github.jessez332623.reactive_luascript_reader.impl.exception.LuaScriptExecuteFailed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.jessee.git_remote_repo_listener.constant.LuaScriptOperatorType.GLOBAL_ID_CONSUMER;

/** 全局 ID 消费机实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIdConsumerImpl implements GlobalIdConsumer
{
    /** ID 消费机属性类。*/
    private final IdConsumerProperties properties;

    /** 通用 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 专用于执行 Lua 脚本的 Redis 模板。*/
    private final
    ReactiveRedisTemplate<String, LuaOperatorResult> scriptTemplate;

    /** Lua 脚本读取器。*/
    private final LuaScriptReader scriptReader;

    /** Jackson 对象映射器。*/
    private final ObjectMapper mapper;

    /** 响应式 HTTP 客户端。*/
    private final WebClient webClient;

    /** 构造在全局 ID 池短暂枯竭时的重试策略。*/
    private @NotNull Function<Flux<Long>, ? extends Publisher<?>> repeatStrategy()
    {
        final Duration startBackoff = this.properties.getStartBackoff();
        final Duration maxBackoff   = this.properties.getMaxBackoff();

        return (attempts) ->
            attempts.map((index) -> {
                // 计算下一次指数退避的时间
                final long baseBackoff
                    = startBackoff.toMillis() * (long) Math.pow(2, index);

                // 不得超过封顶并附带全抖动
                return Duration.ofMillis(
                    // Math.random() 伪随机生成的范围是 [0.0, 1.0)
                    (long) (Math.random() * Math.min(baseBackoff, maxBackoff.toMillis()))
                );
            }).concatMap(Mono::delay);
    }

    private Mono<List<Long>> parseIdsJson(String JSON)
    {
        return Mono.fromCallable(() ->
            this.mapper
                .readValue(JSON, new TypeReference<List<String>>() {})
                .stream()
                .map((idStr) -> idStr.substring(1, idStr.length() - 1))
                .map(Long::parseLong)
                .toList()
        );
    }

    private Mono<Long> nextIdDirectory()
    {
        return
        this.webClient.get()
            .uri(URI.create(this.properties.getDirectoryUrls().getNext()))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap((body) ->
                Mono.fromCallable(() ->
                    mapper.readTree(body).get("data").asLong()))
            .onErrorResume((exception) -> {
                log.error("", exception);
                return Mono.empty();
            });
    }

    private Mono<List<Long>> nextBatchIdsDirectory(int batchSize)
    {
        final String uri
            = this.properties.getDirectoryUrls()
                  .getNextBatch() + "?size=" + batchSize;

        return
        this.webClient.get()
            .uri(URI.create(uri))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap((body) ->
                Mono.fromCallable(() -> {
                    final Spliterator<JsonNode> spliterator
                        = this.mapper.readTree(body).get("data")
                              .spliterator();

                    return
                    StreamSupport.stream(spliterator, false)
                        .map(JsonNode::asLong)
                        .toList();
                })
            )
            .onErrorResume((exception) -> {
                log.error("", exception);
                return Mono.empty();
            });
    }

    /** 获取下一个 ID。*/
    @Override
    public Mono<Long> nextId()
    {
        final String   globalIdKey  = this.properties.getGlobalIdKey();
        final int      maxRetries   = this.properties.getRetries();
        final Duration blockTimeout = this.properties.getBlockTimeout();

        return
        this.redisTemplate.opsForList()
            .rightPop(globalIdKey, blockTimeout)
            .map(String::valueOf)
            .map(Long::parseLong)
            .switchIfEmpty(this.nextIdDirectory())
            .repeatWhenEmpty(maxRetries, this.repeatStrategy());
    }

    /** 获取下一批 ID。*/
    @Override
    public Mono<List<Long>> nextBatchId(int batchSize)
    {
        final String globalIdKey = this.properties.getGlobalIdKey();
        final int    maxRetries  = this.properties.getRetries();

        // 批大小不得小于等于 0
        if (batchSize <= 0)
        {
            log.warn("Batch size must be positive!");
            return Mono.just(List.of());
        }

        return
        this.scriptReader.read(GLOBAL_ID_CONSUMER, "batchIdsConsumer.lua")
            .flatMap((script) ->
                this.scriptTemplate
                    .execute(script, List.of(globalIdKey), batchSize)
                    .next()
                    .flatMap((result) -> {
                        if ("SUCCESS".equals(result.getStatus()))
                        {
                            log.info(
                                "[{}] {}",
                                result.getStatus(), result.getMessage()
                            );

                            // 如果本次调用发现 ID 池完全枯竭了，直接直连
                            if (!StringUtils.hasText(result.getData())) {
                                return this.nextBatchIdsDirectory(batchSize);
                            }

                            return
                            this.parseIdsJson(result.getData())
                                .flatMap((ids) -> {
                                    final int elements   = ids.size();
                                    final int requestelements = batchSize - elements;

                                    if (requestelements != 0)
                                    {
                                        return
                                        this.nextBatchIdsDirectory(requestelements)
                                            .map((requestIds) ->
                                                Stream.concat(ids.stream(), requestIds.stream())
                                                      .toList()
                                            );
                                    }

                                    return Mono.just(ids);
                                });
                        }
                        else
                        {
                            return
                            Mono.error(
                                new LuaScriptExecuteFailed(
                                    result.getStatus(),
                                    result.getMessage(), result.getTimestamp()
                                )
                            );
                        }
                    })
                )
                .repeatWhenEmpty(maxRetries, this.repeatStrategy());
    }

    @Override
    public Flux<Long> nextBatchIdFlux(int batchSize)
    {
        return
        this.nextBatchId(batchSize)
            .flatMapMany(Flux::fromIterable);
    }
}