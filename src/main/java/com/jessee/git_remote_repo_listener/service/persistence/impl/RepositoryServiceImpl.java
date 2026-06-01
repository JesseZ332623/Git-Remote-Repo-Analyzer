package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.RepositoryEntity;
import com.jessee.git_remote_repo_listener.mapper.RepositoryMapper;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import com.jessee.git_remote_repo_listener.service.persistence.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** 远程仓库分析仓库数据服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryServiceImpl implements RepositoryService
{
    /** 代码仓库表数据映射接口。*/
    private final RepositoryMapper repositoryMapper;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /**
     * 往 repository 表写入仓库数据，返回该条记录的 ID。
     *
     * @param analyzeId  分析记录 ID
     * @param repoConfig 仓库配置数据
     *
     * @return 本次分析下该仓库记录的 ID
     */
    @Override
    public Mono<Long>
    saveRepository(Long analyzeId, RepoPathProperties.RepoConfig repoConfig)
    {
        return
        this.globalIdConsumer.nextId()
            .flatMap((id) -> {
                final RepositoryEntity repository
                    = new RepositoryEntity();

                repository.setId(id);
                repository.setIsNew(true);

                return
                this.repositoryMapper.save(
                    repository.setAnalyzeId(analyzeId)
                        .setLocalPath(repoConfig.getPath())
                        .setRemoteName(repoConfig.getRemote())
                );
            })
            .map((repository) ->
                Objects.requireNonNull(repository.getId()))
            .doOnSuccess((repoId) ->
                log.info(
                    "Call saveRepository(), " +
                    "analyze id = {}, return repo id = {}",
                    analyzeId, repoId
                )
            );
    }
}