package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.AnalyzeRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/** 分析记录表数据映射接口。*/
@Repository
public interface AnalyzeRecordMapper
    extends ReactiveCrudRepository<AnalyzeRecord, Long> {}