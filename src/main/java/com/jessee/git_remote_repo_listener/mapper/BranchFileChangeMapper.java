package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.BranchFileChangeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/** 分支文件提交变更数据映射接口。*/
@Repository
public interface BranchFileChangeMapper
    extends ReactiveCrudRepository<BranchFileChangeEntity, Long> {}