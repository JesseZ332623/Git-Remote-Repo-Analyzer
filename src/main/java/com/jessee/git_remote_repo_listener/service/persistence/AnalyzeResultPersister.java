package com.jessee.git_remote_repo_listener.service.persistence;

import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/** 远程仓库分析结果持久化器接口。*/
public interface AnalyzeResultPersister
{
    /**
     * 保存一条分析记录数据，最开始 is_complete 字段会被设为 N。
     *
     * @param analyzeDateTime   上游给出的分析开始时间
     *
     * @return 分析记录 ID
     */
    Mono<Long> saveAnalyzeRecord(LocalDateTime analyzeDateTime);

    /**
     * 将 {@link RemoteRepositoryAnalyzerService#doAnalysis()}
     * 的分析结果持久化到数据库。
     *
     * @param analyzeId         上游给出的分析记录 ID
     * @param analyzeDateTime   上游给出的分析开始时间
     * @param analyzeResults    上游给出的分析结果
     */
    Mono<Void> save(
        final Long                    analyzeId,
        final LocalDateTime           analyzeDateTime,
        final List<BranchFileChanges> analyzeResults
    );
}