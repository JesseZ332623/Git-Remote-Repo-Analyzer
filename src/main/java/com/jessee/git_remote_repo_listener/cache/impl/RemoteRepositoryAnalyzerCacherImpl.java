package com.jessee.git_remote_repo_listener.cache.impl;

import com.jessee.git_remote_repo_listener.cache.RemoteRepositoryAnalyzerCacher;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/** 远程仓库分析器服务缓存实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzerCacherImpl implements RemoteRepositoryAnalyzerCacher
{
    /** Redis 通用响应式模板。*/
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 专用于 BranchFileChange 的 Redis 响应式模板。*/
    private final
    ReactiveRedisTemplate<String, BranchFileChange> branchFileChangeRedisTemplate;

    /** 拼接远程分支变化表缓存键。*/
    @Contract(pure = true)
    private static String
    forEachRefCacheKey(RepoPathProperties.RepoConfig repoConfig)
    {
        return "git-remote-repo-analyzer:for-each-ref:"
                + repoConfig.getDirectoryName() 
                + ":" 
                + repoConfig.getRemote();
    }

    /** 更新的远程分支的详细文件变更状态缓存键。*/
    @Contract(pure = true)
    private static String
    diffResultCacheKey(RepoPathProperties.RepoConfig repoConfig)
    {
        return "git-remote-repo-analyzer:diff-result:" 
                + repoConfig.getDirectoryName()
                + ":"
                + repoConfig.getRemote();
    }

    @Override
    public Mono<Void>
    cacheForEachRefsMap(RepoPathProperties.RepoConfig repo, Map<String, String> forEachRefs)
    {
        final String forEachRefKey = forEachRefCacheKey(repo);

        return
        this.redisTemplate.opsForHash()
            .putAll(forEachRefKey, forEachRefs)
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    @Override
    public Mono<Void>
    cacheBranchFileChanges(RepoPathProperties.RepoConfig repo, List<BranchFileChange> branchFileChanges)
    {
        final String diffResultCacheKey = diffResultCacheKey(repo);

        return
        this.branchFileChangeRedisTemplate.opsForList()
            .rightPushAll(diffResultCacheKey, branchFileChanges.toArray(BranchFileChange[]::new))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    /** 获取缓存的每个远程分支的当前最新提交哈希表。*/
    @Override
    public Mono<Map<String, String>>
    getForEachRefsMap(RepoPathProperties.RepoConfig repo)
    {
        final String forEachRefKey = forEachRefCacheKey(repo);

        return
        this.redisTemplate.opsForHash()
            .entries(forEachRefKey)
            .collectMap(
                (entry) -> (String) entry.getKey(),
                (entry) -> (String) entry.getValue())
            .subscribeOn(Schedulers.boundedElastic());
    }

    /** 获取缓存的每个远程分支的提交文件变更信息。*/
    @Override
    public Mono<List<BranchFileChange>>
    getBranchFileChanges(RepoPathProperties.RepoConfig repo)
    {
        final String diffResultCacheKey = diffResultCacheKey(repo);

        return
        this.branchFileChangeRedisTemplate
            .opsForList()
            .range(diffResultCacheKey, 0, -1)
            .cast(BranchFileChange.class)
            .collectList()
            .subscribeOn(Schedulers.boundedElastic());
    }
}