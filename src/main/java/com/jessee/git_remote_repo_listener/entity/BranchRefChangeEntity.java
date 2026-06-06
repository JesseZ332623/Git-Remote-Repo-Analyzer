package com.jessee.git_remote_repo_listener.entity;

import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import com.jessee.git_remote_repo_listener.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** branch_ref_change 表实体类，记录分支引用的变化。*/
@Data
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "branch_ref_change")
public class BranchRefChangeEntity extends BaseEntity
{
    @Column(value = "repository_id")
    private Long repositoryId;

    @Column(value = "branch_id")
    private Long branchId;

    @Column(value = "prev_latest_commit_hash")
    private String prevLatestCommitHash;

    @Column(value = "latest_commit_hash")
    private String latestCommitHash;

    @Column(value = "change_status")
    private RemoteChangeStaus changeStaus;

    @Column(value = "analyze_id")
    private Long analyzeId;
}