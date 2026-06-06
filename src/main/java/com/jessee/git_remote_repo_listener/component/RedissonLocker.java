package com.jessee.git_remote_repo_listener.component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/** Redisson 分布式锁封装工具组件接口。*/
public interface RedissonLocker
{
    /**
     * 尝试获取分布式锁，获取锁后再执行传入的操作。
     *
     * @param <T>      操作 operator 最终要向下游发布的数据类型
     * @param operator 获取锁后要执行的操作
     * @param wait     是否等待（传 false 意味着不等待）
     *
     * @return operator 需要向下游发布的数据
     */
    <T> Mono<T> withLock(Mono<T> operator, boolean wait);

    /**
     * 尝试获取分布式锁，获取锁后再执行传入的操作。
     *
     * @param <T>           操作 operator 最终要向下游发布的数据类型
     * @param operator      获取锁后要执行的操作
     * @param lockPrefixKey 锁键前缀名
     * @param wait          是否等待（传 false 意味着不等待）
     *
     * @return operator 需要向下游发布的数据
     */
    <T> Mono<T> withLock(Mono<T> operator, String lockPrefixKey, boolean wait);

    /**
     * 将上游的整个操作包裹在分布式锁内，
     * 可以使用 {@link Mono#as(Function)} 或者
     * {@link Flux#as(Function)} 组合本函数式接口。
     *
     * @param wait 是否等待（传 false 意味着不等待）
     */
    <T> Function<Mono<T>, Mono<T>> lockAround(boolean wait);
}
