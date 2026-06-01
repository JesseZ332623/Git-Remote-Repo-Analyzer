package com.jessee.git_remote_repo_listener.service.persistence;

import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** 远程仓库分析记录数据服务接口。*/
public interface AnalyzeRecordService
{
    /**
     * 往 analyze_record 表中写入分析记录数据，返回 ID。
     *
     * @param analyzeDateTime
     *        分析开始的时间，
     *        由 {@link RemoteRepositoryAnalyzerService#doAnalysis()} 给出
     *
     * @return 这条分析记录在数据表中的 ID
     */
    Mono<Long> saveAnalyzeRecord(LocalDateTime analyzeDateTime);

    /**
     * 在执行完分析后，标记本次分析完成。
     *
     * @param id 需要标记完成的分析记录 ID
     *
     * @return 无需向下游传递数据
     */
    Mono<Void> setAnalyzComplete(Long id);
}