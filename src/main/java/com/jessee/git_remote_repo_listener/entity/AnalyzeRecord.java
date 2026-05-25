package com.jessee.git_remote_repo_listener.entity;

import com.jessee.git_remote_repo_listener.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** analyze_record 表实体类。*/
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "analyze_record")
public class AnalyzeRecord extends BaseEntity
{
    @Column(value = "analyze_datetime")
    private LocalDateTime analyzeDateTime;

    @Column(value = "is_completed")
    private String isCompleted;

    /** 本次分析是否已经完成？ */
    public boolean completed() {
        return "Y".equals(this.isCompleted);
    }
}