package com.jessee.git_remote_repo_listener.cache;

import com.jessee.git_remote_repo_listener.pojo.RemoteRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** 待分析仓库缓存操作接口。*/
public interface RemoteRepositoryCacher
{
    /** 从缓存中获取所有待分析的仓库信息。*/
    Flux<RemoteRepository> getAll();

    /**
     * 添加一个待分析仓库。
     *（在分析任务执行时禁止操作）
     */
    Mono<Void> add(RemoteRepository remoteRepository);

    /**
     * 添加一批待分析仓库。
     *（在分析任务执行时禁止操作）
     */
    Mono<Void> add(List<RemoteRepository> remoteRepositories);

    /**
     *删除某条待分析仓库。
     *（在分析任务执行时禁止操作）
     */
    Mono<Void> delete(String repositoryPath);
}