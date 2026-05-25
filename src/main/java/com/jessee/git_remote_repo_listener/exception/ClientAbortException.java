package com.jessee.git_remote_repo_listener.exception;

/**
 * 客户端中断一个请求时，
 * 捕获 {@link InterruptedException} 再包装成本异常。
 */
public class ClientAbortException extends RuntimeException
{
    public ClientAbortException(String message) {
        super(message);
    }

    public ClientAbortException(String message, Throwable throwable) {
        super(message, throwable);
    }
}