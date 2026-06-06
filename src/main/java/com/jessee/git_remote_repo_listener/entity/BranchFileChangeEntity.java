package com.jessee.git_remote_repo_listener.entity;

import com.jessee.git_remote_repo_listener.constant.CommitChangeStatus;
import com.jessee.git_remote_repo_listener.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** branch_file_change 表实体类，记录仓库下提交文件发状态。*/
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "branch_file_change")
public class BranchFileChangeEntity extends BaseEntity
{
    @Column(value = "repository_id")
    private Long repositoryId;

    @Column(value = "branch_id")
    private Long branchId;

    @Column(value = "change_status")
    private CommitChangeStatus changeStatus;

    @Column(value = "similarity")
    private Integer similarity;

    @Column(value = "old_path")
    private String oldPath;

    @Column(value = "new_path")
    private String newPath;

    @Column(value = "analyze_id")
    private Long analyzeId;
}