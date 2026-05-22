package com.jessee.git_remote_repo_listener.entity;

import com.jessee.git_remote_repo_listener.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** repository 表实体类。*/
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(name = "repository")
public class RepositoryEntity extends BaseEntity
{
    @Column(value = "local_path")
    private String localPath;

    @Column(value = "remote_name")
    private String remoteName;

    @Column(value = "analyze_id")
    private Long analyzeId;
}