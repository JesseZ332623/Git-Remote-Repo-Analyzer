package com.jessee.git_remote_repo_listener.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Git 错误重试特征检测工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitRetryableUtils
{
    /** git 命令值得重试的错误信息特征。*/
    private static final
    List<String> RETRYABLE_PATTERNS = Stream.of(
        // 网络/传输层
        "Could not resolve host",
        "Connection refused",
        "Connection timed out",
        "Connection reset",
        "Failed to connect",
        "Network is unreachable",
        "Remote end hung up unexpectedly",
        "RPC failed",
        "curl",
        "TLS connect error",
        // 锁文件（并发操作导致，稍后重试可能成功）
        "Unable to create '",
        // 临时性远端问题
        "503 Service Unavailable",
        "The requested URL returned error: 502",
        "The requested URL returned error: 503",
        "The requested URL returned error: 504",
        "Internal Server Error",
        "temporary failure",
        // fetch/clone 时的临时问题
        "early EOF",
        "fetch-pack: unexpected disconnect",
        "error: RPC failed",
        "fatal: the remote end hung up unexpectedly"
    ).map((s) -> s.toLowerCase(Locale.ROOT)).toList();

    /** git 命令不值得重试的（重试后大概率还是失败的）错误信息特征。*/
    private static final
    List<String> NON_RETRYABLE_PATTERNS = Stream.of(
        "Repository not found",
        "Authentication failed",
        "Permission denied",
        "not authorized",
        "fatal: couldn't find remote ref",
        "does not exist",
        "pathspec"
    ).map((s) -> s.toLowerCase(Locale.ROOT)).toList();

    /** 检查 git 命令的错误输出是否值得重试。*/
    public static boolean isRetryable(String output)
    {
        final String lowerOutput
            = output.toLowerCase(Locale.ROOT);

        return
        NON_RETRYABLE_PATTERNS.stream()
            .noneMatch(lowerOutput::contains)
        &&
        RETRYABLE_PATTERNS.stream()
            .anyMatch(lowerOutput::contains);
    }
}