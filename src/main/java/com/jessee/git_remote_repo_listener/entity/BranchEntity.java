package com.jessee.git_remote_repo_listener.entity;

import com.jessee.git_remote_repo_listener.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** branch 表实体类。*/
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "branch")
public class BranchEntity extends BaseEntity
{
    @Column(value = "repository_id")
    private Long repositoryId;

    @Column(value = "branch_name")
    private String branchName;

    @Column(value = "analyze_id")
    private Long analyzeId;
}