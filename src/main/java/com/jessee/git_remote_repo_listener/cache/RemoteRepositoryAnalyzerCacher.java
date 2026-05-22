package com.jessee.git_remote_repo_listener.cache;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChange;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/** 远程仓库分析器服务缓存接口。*/
public interface RemoteRepositoryAnalyzerCacher
{
    /**
     * 缓存远程仓库下每个远程分支的当前最新提交哈希表。
     *
     * @param repo        仓库属性类
     * @param forEachRefs 每个远程分支的当前最新提交哈希表
     */
    Mono<Void>
    cacheForEachRefsMap(RepoPathProperties.RepoConfig repo, Map<String, String> forEachRefs);

    /**
     * 缓存远程仓库下每个远程分支的提交文件变更信息。
     *
     * @param repo              仓库属性类
     * @param branchFileChanges 每个远程分支的提交文件变更信息
     */
    Mono<Void>
    cacheBranchFileChanges(RepoPathProperties.RepoConfig repo, List<BranchFileChange> branchFileChanges);

    /** 获取缓存的每个远程分支的当前最新提交哈希表。*/
    Mono<Map<String, String>> getForEachRefsMap(RepoPathProperties.RepoConfig repo);

    /** 获取缓存的每个远程分支的提交文件变更信息。*/
    Mono<List<BranchFileChange>> getBranchFileChanges(RepoPathProperties.RepoConfig repo);
}