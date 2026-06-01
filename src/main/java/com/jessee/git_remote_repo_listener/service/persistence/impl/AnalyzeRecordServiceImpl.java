package com.jessee.git_remote_repo_listener.service.persistence.impl;

import com.jessee.git_remote_repo_listener.component.GlobalIdConsumer;
import com.jessee.git_remote_repo_listener.entity.AnalyzeRecord;
import com.jessee.git_remote_repo_listener.mapper.AnalyzeRecordMapper;
import com.jessee.git_remote_repo_listener.service.RemoteRepositoryAnalyzerService;
import com.jessee.git_remote_repo_listener.service.persistence.AnalyzeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

/** 远程仓库分析记录数据服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeRecordServiceImpl implements AnalyzeRecordService
{
    /** 分析记录表数据映射接口。*/
    private final AnalyzeRecordMapper analyzeRecordMapper;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /**
     * 往 analyze_record 表中写入分析记录数据，返回 ID。
     *
     * @param analyzeDateTime
     *        分析开始的时间，
     *        由 {@link RemoteRepositoryAnalyzerService#doAnalysis()} 给出
     *
     * @return 这条分析记录在数据表中的 ID
     */
    @Override
    public Mono<Long> saveAnalyzeRecord(LocalDateTime analyzeDateTime)
    {
        return
        this.globalIdConsumer.nextId()
            .flatMap((id) -> {
                final AnalyzeRecord analyzeRecord
                    = new AnalyzeRecord();

                analyzeRecord.setId(id);
                analyzeRecord.setIsNew(true);
                analyzeRecord.setIsCompleted("N");

                return
                this.analyzeRecordMapper
                    .save(analyzeRecord.setAnalyzeDateTime(analyzeDateTime));
            })
            .map((analyzeRecord) ->
                Objects.requireNonNull(analyzeRecord.getId()));
    }

    /**
     * 在执行完分析后，标记本次分析完成。
     *
     * @param id 需要标记完成的分析记录 ID
     *
     * @return 无需向下游传递数据
     */
    @Override
    public Mono<Void> setAnalyzComplete(Long id) {
        return this.analyzeRecordMapper.setAnalyzComplete(id);
    }
}