package com.jessee.git_remote_repo_listener.service.impl;

import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.properties.RepoPathProperties;
import com.jessee.git_remote_repo_listener.service.GitConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Git 配置操作服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class GitConfigServiceImpl implements GitConfigService
{
    /** 已经被配置的 safe.directory 仓库路径集合缓存。*/
    private static final
    Set<String> CONFIGURED_REPOS = ConcurrentHashMap.newKeySet();

    /** 监视锁管理器，每个仓库名分配一个监视锁。*/
    private static final
    ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    /** 本地待分析仓库路径配置类。*/
    private final RepoPathProperties repoPathProperties;

    /** Git 远程仓库分析器接口。*/
    private final RemoteRepositoryAnalyzer remoteRepositoryAnalyzer;

    /**
     * 服务启动的时候先执行一次，
     * 将待分析的仓库目录全部纳入 safe.directory 列表中并缓存，
     * 外部仅开放查询。
     */
    @PostConstruct
    public void doSafeDirectoryConfiguration()
    {
        this.remoteRepositoryAnalyzer.gitConfigGetAllSafeDirectory()
            .map((output) ->
                output.trim().lines().toList())
            .flatMap((safeDirectorys) ->
                Flux.fromIterable(this.repoPathProperties.getRepos())
                    .flatMap((repo) ->
                        this.ensureSafeDirectoryConfigured(repo.getPath(), safeDirectorys))
                    .then()
            ).subscribe();
    }

    private Mono<Boolean>
    isSafeDirectoryConfigured(String repoPath, List<String> safeDirectorys)
    {
        return
        Mono.fromCallable(() ->
            safeDirectorys.stream()
                .anyMatch((safeDirectory) ->
                        safeDirectory.equals(repoPath)
                )
        );
    }

    /**
     * 确保指定的仓库在 safe.directory 之下，
     * 如果没有则添加，反之直接返回。
     */
    private Mono<Void>
    ensureSafeDirectoryConfigured(String repoPath, List<String> safeDirectorys)
    {
        return
        Mono.defer(() -> {
            if (CONFIGURED_REPOS.contains(repoPath)) {
                return Mono.empty();
            }

            final Object lock
                = LOCKS.computeIfAbsent(repoPath, (key) -> new Object());

            synchronized (lock)
            {
                if (CONFIGURED_REPOS.contains(repoPath)) {
                    return Mono.empty();
                }

                return
                this.isSafeDirectoryConfigured(repoPath, safeDirectorys)
                    .flatMap((isConfigured) -> {
                        if (isConfigured)
                        {
                            return
                            Mono.fromRunnable(() -> CONFIGURED_REPOS.add(repoPath))
                                .then();
                        }

                        return
                        this.remoteRepositoryAnalyzer
                            .gitConfigAddSafeDirectory(repoPath)
                            .then(Mono.fromRunnable(() -> CONFIGURED_REPOS.add(repoPath)));
                    });
            }
        });
    }

    /** 检查指定的目录是否在 safe.directory 之下。*/
    @Override
    public Mono<Boolean>
    isSafeDirectoryConfigured(String repoPath)
    {
        return
        Mono.fromCallable(() -> CONFIGURED_REPOS.contains(repoPath))
            .flatMap((isConfigured) ->
                (!isConfigured)
                    ? this.remoteRepositoryAnalyzer
                          .gitConfigGetAllSafeDirectory()
                          .map((output) -> output.contains(repoPath))
                          .onErrorReturn(false)
                    : Mono.just(true)
            );
    }
}