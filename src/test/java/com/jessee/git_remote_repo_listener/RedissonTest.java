package com.jessee.git_remote_repo_listener;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.properties.RedisDistributeLockProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/** 响应式分布式锁使用练习测试，旨在研究最佳实践。*/
@Slf4j
@SpringBootTest
public class RedissonTest
{
    @Autowired
    private RedissonReactiveClient redissonReactiveClient;

    @Autowired
    private GlobalIdConsumer globalIdConsumer;

    @Autowired
    private RedisDistributeLockProperties properties;

    private static
    <T> Mono<T> timeoutExceptionMono(String message) {
        return Mono.error(new TimeoutException(message));
    }

    /** 响应式分布式锁使用最佳实践。*/
    @Test
    public void distributeLockBestPractice()
    {
        this.globalIdConsumer.nextId()
            .flatMap((id) ->
                Mono.defer(() -> {
                    // 获取锁实例
                    final RLockReactive lock
                        = this.redissonReactiveClient.getLock(properties.getKeyPrefix());

                    // 响应式环境下线程 ID 不可靠，用雪花 ID 代替
                    final long threadId  = id;
                    final long waitTime  = this.properties.getLockWaitTimeout().toMillis();
                    final long leaseTime = this.properties.getLockLeaseTime().toMillis();

                    return Mono.usingWhen(
                        lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS, threadId),
                        (isLocked) ->
                            (isLocked)
                                ? Mono.delay(Duration.ofSeconds(1L)) // 这个 Mono 可以变成参数从外部传入
                                : timeoutExceptionMono(
                                    format("Wait to acquire lock timeout. (Wait time: %d)", waitTime)
                                ),
                        (ignore) ->
                            lock.isLocked()
                                .filter(Boolean::booleanValue)
                                .then(lock.unlock(threadId))
                    );
                })
            )
            .subscribeOn(Schedulers.boundedElastic())
            .block();
    }
}