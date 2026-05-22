package com.jessee.git_remote_repo_listener.service;

import reactor.core.publisher.Mono;

/** Git 配置操作服务接口。*/
public interface GitConfigService
{
    /** 检查指定的目录是否在 safe.directory 之下。*/
    Mono<Boolean> isSafeDirectoryConfigured(String repoPath);
}