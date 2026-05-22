package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.BranchRefChangeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/** 分支快照引用变化数据映射接口。*/
@Repository
public interface BranchRefChangeMapper
    extends ReactiveCrudRepository<BranchRefChangeEntity, Long> {}
