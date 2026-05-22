package com.jessee.git_remote_repo_listener.utils;

import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 远程分支快照操作工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RemoteSnapshotUtils
{
    /** 比较并解析两次 git for-each-ref 命令中，每个远程分支的最新提交变化。*/
    public static Mono<Map<String, BranchRefChange>>
    compareRemoteHash(Map<String, String> befor, Map<String, String> after)
    {
        return
        Flux.fromIterable(after.entrySet())
            .map((entry) -> {
                final String remoteName = entry.getKey();
                final String prevHash   = befor.get(remoteName);
                final String currHash   = entry.getValue();

                return Map.entry(remoteName, BranchRefChange.of(prevHash, currHash));
            })
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .filter((map) ->
                // 如果所有分支的状态都为不变，直接筛掉返回空表，
                // 下游会为这种情况准备空报告邮件
                !map.values().stream()
                    .map(BranchRefChange::getStatus)
                    .allMatch((remoteChangeStaus) ->
                        remoteChangeStaus.equals(RemoteChangeStaus.IMMUTABLE))
            )
            .defaultIfEmpty(Map.of());
    }
}