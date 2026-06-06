package com.jessee.git_remote_repo_listener.component.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.component.RedissonLocker;
import com.jessee.git_remote_repo_listener.properties.RedisDistributeLockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.lang.String.format;

/** Redisson 分布式锁封装工具组件实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockerImpl implements RedissonLocker
{
    /** 响应式 Redisson 客户端。*/
    private final RedissonReactiveClient redissonReactiveClient;

    /** 全局 ID 消费机。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** Redisson 分布式锁属性配置类。*/
    private final RedisDistributeLockProperties properties;

    private static
    <T>Mono<T> timeoutExceptionMono(String message) {
        return Mono.error(new TimeoutException(message));
    }

    /**
     * 将上游的整个操作包裹在分布式锁内，
     * 可以使用 {@link Mono#as(Function)} 或者
     * {@link Flux#as(Function)} 组合本函数式接口。
     *
     * @param wait 是否等待（传 false 意味着不等待）
     */
    @Override
    public <T> Function<Mono<T>, Mono<T>> lockAround(boolean wait)
    {
        return (operator) ->
            this.withLock(operator, wait);
    }

    /**
     * 尝试获取分布式锁，获取锁后再执行传入的操作。
     *
     * @param <T>      操作 operator 最终要向下游发布的数据类型
     * @param operator 获取锁后要执行的操作
     * @param wait     是否等待（传 false 意味着不等待）
     *
     * @return operator 需要向下游发布的数据
     */
    @Override
    public <T> Mono<T> withLock(Mono<T> operator, boolean wait)
    {
        return
        this.withLock(operator, this.properties.getKeyPrefix(), wait);
    }

    /**
     * 尝试获取分布式锁，获取锁后再执行传入的操作。
     *
     * @param operator      获取锁后要执行的操作
     * @param lockPrefixKey 锁键前缀名
     * @param wait          是否等待（传 false 意味着不等待）
     * @return operator 需要向下游发布的数据
     */
    @Override
    public <T> Mono<T>
    withLock(Mono<T> operator, String lockPrefixKey, boolean wait)
    {
        return
        this.globalIdConsumer.nextId()
            .flatMap((id) ->
                Mono.defer(() -> {
                    // 获取锁实例
                    final RLockReactive lock
                        = this.redissonReactiveClient.getLock(lockPrefixKey);

                    // 响应式环境下线程 ID 不可靠，用雪花 ID 代替
                    final long threadId  = id;

                    final long waitTime
                        = (wait) ? this.properties.getLockWaitTimeout().toMillis() : 0L;

                    final long leaseTime
                        = this.properties.getLockLeaseTime().toMillis();

                    return
                    Mono.usingWhen(
                        lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS, threadId),
                        (isLocked) ->
                            (isLocked)
                                ? operator
                                : timeoutExceptionMono(
                                    format(
                                        "Wait to acquire lock timeout, " +
                                        "the lock is already occupied. (Wait time: %d)",
                                        waitTime
                                    )
                            ),
                        (ignore) ->
                             lock.isLocked()
                                 .filter(Boolean::booleanValue)
                                 .then(lock.unlock(threadId))
                                 .onErrorResume(IllegalMonitorStateException.class, e -> {

                                     log.warn("Lock already released or not owned by this threadId: {}", threadId);
                                     return Mono.empty();
                                 })
                    );
                })
            ).subscribeOn(Schedulers.boundedElastic());
    }
}