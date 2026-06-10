package com.jessee.git_remote_repo_listener.cache.impl;

import com.jessee.git_remote_repo_listener.cache.EmailRecipientCacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.lang.String.format;

/** 邮件收件人缓存类实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRecipientCacherImpl implements EmailRecipientCacher
{
    /** 收件人缓存键。*/
    private static final String
    RECIPIENT_CACHE_KEY = "git-remote-repo-analyzer:recipient";

    /** Redis 通用响应式模板。*/
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Override
    public Mono<Void> addOneRecipientCacher(String name, String address)
    {
        return
        this.redisTemplate.opsForHash()
            .put(RECIPIENT_CACHE_KEY, name, address)
            .then();
    }

    @Override
    public Mono<String> getRecipientAddress(String name)
    {
        return
        this.redisTemplate.opsForHash()
            .get(RECIPIENT_CACHE_KEY, name)
            .switchIfEmpty(
                Mono.error(
                    new NoSuchElementException(
                        format(
                            "Recipient address of name: %s not exist in key: %s",
                            name, RECIPIENT_CACHE_KEY
                        )
                    )
                )
            )
            .cast(String.class);
    }

    @Override
    public Mono<List<String>> getAllRecipientAddress()
    {
        return
        this.redisTemplate.opsForHash()
            .entries(RECIPIENT_CACHE_KEY)
            .switchIfEmpty(
                Mono.defer(() -> {
                    log.warn(
                        "No recipient address in key: {}, use defalt value.",
                        RECIPIENT_CACHE_KEY
                    );

                    // 缓存如果没有任何收件人，就先发到我这里，避免丢件
                    return
                    Mono.just(Map.entry("Jesse", "zhj3191955858@gmail.com"));
                })
            )
            .map(Map.Entry::getValue)
            .cast(String.class)
            .collectList();
    }
}