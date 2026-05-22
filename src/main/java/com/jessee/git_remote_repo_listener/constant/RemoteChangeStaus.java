package com.jessee.git_remote_repo_listener.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** 远程分支变更状态枚举。*/
@ToString
@RequiredArgsConstructor
public enum RemoteChangeStaus
{
    NEW("分支新增"),
    UPDATE("分支提交变更"),
    DELETE("分支删除"),
    IMMUTABLE("分支不变");

    @Getter
    private final String desc;
}