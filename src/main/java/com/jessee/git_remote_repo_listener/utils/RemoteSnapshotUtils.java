package com.jessee.git_remote_repo_listener.utils;

import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/** 远程分支快照操作工具类。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RemoteSnapshotUtils
{
    /** 分析结果调试文件相对路径。*/
    private static final
    Path ANALYZE_JSON_FILE_PATH
        = Path.of("./debug-temp-file/analyze-res.json")
              .normalize();

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

    /** 将待持久化的分析结果序列化成 JSON 再写入文件，调试用。*/
    public static @NotNull Mono<Void>
    toFile(List<BranchFileChanges> analyzeResults)
    {
        return
        Mono.fromCallable(() -> {
            final String analyzeJson
                = PrettyJSONPrinter.getPrettyFormatJSON(analyzeResults);

            return
            Files.writeString(
                ANALYZE_JSON_FILE_PATH,
                analyzeJson,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        })
        .onErrorResume(
            IOException.class,
            (exception) -> {
                log.error(
                    "Write analyze result json to debug-file: {} failed, skip.",
                    ANALYZE_JSON_FILE_PATH.toAbsolutePath(),
                    exception
                );

                return Mono.empty();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}