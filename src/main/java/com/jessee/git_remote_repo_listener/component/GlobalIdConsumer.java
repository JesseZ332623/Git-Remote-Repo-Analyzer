package com.jessee.git_remote_repo_listener.component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** 全局 ID 消费机接口。*/
public interface GlobalIdConsumer
{
    /** 获取下一个 ID。*/
    Mono<Long> nextId();

    /** 获取下一批 ID。*/
    Mono<List<Long>> nextBatchId(int batchSize);

    /** 获取下一批 ID，但返回 {@link Flux}。*/
    Flux<Long> nextBatchIdFlux(int batchSize);
}