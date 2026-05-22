package com.jessee.git_remote_repo_listener.entity.base;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

/**
 * 所有实体的抽象基类。
 * 补充：在 R2DBC 中，如果为实体手动分配 id，
 * R2DBC 实际执行的是 UPDATE 而非 INSERT，
 * 所以需要实体实现 Persistable 接口来区分更新与插入。
 */
@Data
public class BaseEntity implements Persistable<Long>
{
    @Id
    @Column(value = "id")
    private Long id;

    @Transient
    private Boolean isNew = true;

    /**
     * Returns the id of the entity.
     *
     * @return the id. Can be {@literal null}.
     */
    @Override
    public @Nullable Long getId() {
        return this.id;
    }

    /**
     * Returns if the {@code Persistable} is new or was persisted already.
     *
     * @return if {@literal true} the object is new.
     */
    @Override
    public boolean isNew() {
        return this.isNew;
    }
}