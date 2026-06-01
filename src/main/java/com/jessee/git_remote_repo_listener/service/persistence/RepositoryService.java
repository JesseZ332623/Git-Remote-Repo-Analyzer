package com.jessee.git_remote_repo_listener.service.persistence;

import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import reactor.core.publisher.Mono;

/** 远程仓库分析仓库数据服务接口。*/
public interface RepositoryService
{
    /**
     * 往 repository 表写入仓库数据，返回该条记录的 ID。
     *
     * @param analyzeId  分析记录 ID
     * @param repoConfig 仓库配置数据
     *
     * @return 本次分析下该仓库记录的 ID
     */
    Mono<Long>
    saveRepository(Long analyzeId, RepoPathProperties.RepoConfig repoConfig);
}