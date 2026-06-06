package com.jessee.git_remote_repo_listener.cache.impl;

import com.jessee.git_remote_repo_listener.cache.RemoteRepositoryCacher;
import com.jessee.git_remote_repo_listener.component.RedissonLocker;
import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.pojo.RemoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/** 待分析仓库缓存操作实现。*/
@Component
@RequiredArgsConstructor
public class RemoteRepositoryCacherImpl implements RemoteRepositoryCacher
{
    /** 待分析仓库缓存键。*/
    private final static String
    REMOTE_REPO_CACHE_KEY = "git-remote-repo-analyzer:repositories";

    /** 通用的 Redis 响应式操作模板。*/
    private final
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** Git 远程仓库分析器接口。*/
    private final
    RemoteRepositoryAnalyzer remoteRepositoryAnalyzer;

    /** Redisson 分布式锁封装工具组件接口。*/
    private final RedissonLocker redissonLocker;

    /**
     * 将 {@link RemoteRepository} 列表处理成 {@link Map}，
     * 下游会存入缓存。
     */
    private static Mono<Map<String, String>>
    makeRepositoryMap(List<RemoteRepository> remoteRepositories)
    {
        return
        Flux.fromIterable(remoteRepositories)
            .flatMap(RemoteRepository::checkPath)
            .then(
                Mono.fromCallable(() ->
                    remoteRepositories.stream()
                        .collect(
                            Collectors.toUnmodifiableMap(
                                RemoteRepository::getPath,
                                RemoteRepository::getRemote,
                                // 如果发生冲突则保留前者
                                (a, b) -> a
                            )
                        )
                )
            );
    }

    /** 从缓存中读取当前的待分析仓库路径列表。*/
    private Mono<Set<String>> getCachedRepoPaths()
    {
        return
        this.redisTemplate.opsForHash()
            .keys(REMOTE_REPO_CACHE_KEY)
            .map(String::valueOf)
            .collectList()
            .map(HashSet::new);
    }

    /** 从缓存中获取所有待分析的仓库信息。*/
    @Override
    public Flux<RemoteRepository> getAll()
    {
        return
        this.redisTemplate.opsForHash()
            .entries(REMOTE_REPO_CACHE_KEY)
            .map(RemoteRepository::fromCache)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 添加一个待分析仓库。
     *（在分析任务执行时禁止操作）
     */
    @Override
    public Mono<Void> add(RemoteRepository remoteRepository)
    {
        final String path   = remoteRepository.getPath();
        final String remote = remoteRepository.getRemote();

        return
        remoteRepository.checkPath()
            .then(
                this.redisTemplate.opsForHash()
                    .put(REMOTE_REPO_CACHE_KEY, path, remote)
                    .filter(Boolean::booleanValue)
                    .switchIfEmpty(
                        Mono.error(
                            new IllegalArgumentException(
                                format(
                                    "Add new repository info failed, " +
                                        "Caused by: repository path: %s already exists.",
                                    path
                                )
                            )
                        )
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(this.remoteRepositoryAnalyzer.gitConfigAddSafeDirectory(path))
            )
            .as(this.redissonLocker.lockAround(false));
    }

    /**
     * 添加一批待分析仓库。
     * （在分析任务执行时禁止操作）
     */
    @Override
    public Mono<Void> add(List<RemoteRepository> remoteRepositories)
    {
        return
        Mono.zip(
            makeRepositoryMap(remoteRepositories),
            this.getCachedRepoPaths()
        )
        .flatMap((tuple) -> {
            final Map<String, String> tobeAddedRepos = tuple.getT1();
            final Set<String> cachedRepoPaths        = tuple.getT2();

            // 找出可能存在的仓库路径冲突
            final List<String> duplicates
                = tobeAddedRepos.keySet().stream()
                    .filter(cachedRepoPaths::contains)
                    .toList();

            if (!CollectionUtils.isEmpty(duplicates))
            {
                return
                Mono.error(
                    new IllegalArgumentException(
                        format(
                            "Repository path %s already exist in cache! skip this add request.",
                            String.join(", ", duplicates)
                        )
                    )
                );
            }

            return
            this.redisTemplate.opsForHash()
                .putAll(REMOTE_REPO_CACHE_KEY, tobeAddedRepos)
                .subscribeOn(Schedulers.boundedElastic())
                .then(
                    Flux.fromIterable(tobeAddedRepos.keySet())
                        .flatMap(this.remoteRepositoryAnalyzer::gitConfigAddSafeDirectory)
                        .then()
                );
        })
        .as(this.redissonLocker.lockAround(false));
    }

    /**
     * 删除某条待分析仓库。
     *（在分析任务执行时禁止操作）
     */
    @Override
    public Mono<Void> delete(String repositoryPath)
    {
        return
        this.redisTemplate.opsForHash()
            .hasKey(REMOTE_REPO_CACHE_KEY, repositoryPath)
            .subscribeOn(Schedulers.boundedElastic())
            .filter(Boolean::booleanValue)
            .switchIfEmpty(
                Mono.error(
                    new IllegalArgumentException(
                        format("Remote repository %s not exist in cache!", repositoryPath)
                    )
                )
            )
            .flatMap((ignore) ->
                this.redisTemplate.opsForHash()
                    .remove(REMOTE_REPO_CACHE_KEY, repositoryPath)
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(
                        this.remoteRepositoryAnalyzer
                            .gitConfigDeleteSafeDirectory(repositoryPath)
                    )
            )
            .as(this.redissonLocker.lockAround(false));
    }
}