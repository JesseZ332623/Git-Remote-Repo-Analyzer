package com.jessee.git_remote_repo_listener.exception;

/**
 * 在 Git 命令执行的过程中，如果因为并发锁或者网络等问题失败，
 * 则抛出本异常，意味着值得重试。
 */
public class GitRetryableException extends RuntimeException
{
    public GitRetryableException(String message) {
        super(message);
    }
}