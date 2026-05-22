package com.jessee.git_remote_repo_listener.service;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/** 远程仓库分析结果持久化器接口。*/
public interface AnalyzeResultPersister
{
    /**
     * 将 {@link RemoteRepositoryAnalyzerService#doAnalysis()}
     * 的分析结果持久化到数据库。
     */
    Mono<Void>
    save(LocalDateTime ayalyzDateTime, List<BranchFileChanges> analyzeResults);
}