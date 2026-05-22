package com.jessee.git_remote_repo_listener.mapper;

import com.jessee.git_remote_repo_listener.entity.RepositoryEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/** 代码仓库表数据映射接口。*/
@Repository
public interface RepositoryMapper
    extends ReactiveCrudRepository<RepositoryEntity, Long> {}