package com.jessee.git_remote_repo_listener.constant;

import io.github.jessez332623.reactive_luascript_reader.impl.LuaScriptCatalogue;
import lombok.RequiredArgsConstructor;

/** 要读取的 Lua 脚本的类型枚举。*/
@RequiredArgsConstructor
public enum LuaScriptOperatorType implements LuaScriptCatalogue
{

    /** 全局 ID 消费相关脚本。*/
    GLOBAL_ID_CONSUMER("global-id-consumer"),

    /** 其他脚本。*/
    OTHERS("others");

    private final String catalogue;

    @Override
    public String getCatalogue() {
        return this.catalogue;
    }
}