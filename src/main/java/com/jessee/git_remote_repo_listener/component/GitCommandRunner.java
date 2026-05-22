package com.jessee.git_remote_repo_listener.component;

import reactor.core.publisher.Mono;

import java.util.List;

/** 响应式 Git 命令执行器接口。*/
public interface GitCommandRunner
{
    /**
     * 执行 Git 命令。
     *
     * @param arguments 命令 + 参数组成的列表
     *
     * @return 向下游传递命令的执行结果字符串
     */
    Mono<String> run(List<String> arguments);
}