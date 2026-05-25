package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.AnalyzeRecord;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** 分析记录表数据映射接口。*/
@Repository
public interface AnalyzeRecordMapper
    extends ReactiveCrudRepository<AnalyzeRecord, Long>
{
    /** 在执行完分析后，标记本次分析完成。*/
    @Modifying
    @Query("UPDATE analyze_record SET is_completed = 'Y' WHERE id = :id")
    Mono<Void> setAnalyzComplete(Long id);
}