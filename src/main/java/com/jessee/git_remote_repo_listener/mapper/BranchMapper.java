package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.BranchEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/** 仓库分支数据映射接口。*/
@Repository
public interface BranchMapper
    extends ReactiveCrudRepository<BranchEntity, Long> {}