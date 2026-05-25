package com.jessee.git_remote_repo_listener.exception;

import com.jessee.git_remote_repo_listener.response.CustomizedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

import static com.jessee.git_remote_repo_listener.response.CustomizedResponse.responseOf;

/** 本服务全局异常处理器。*/
@Slf4j
@RestControllerAdvice(
    // 只对指定的类生效，这里暂定为全局处理
    assignableTypes = {
        // RemoteRepositoryAnalyzeController.class
        /* ... */
    }
)
public class GlobalExceptionHandler
{
    /** Git 异步缓冲区读取线程或者进程等待线程被外部中断时要处理的异常。*/
    @ExceptionHandler(ClientAbortException.class)
    public Mono<CustomizedResponse<Object>>
    handleClientAbort(ClientAbortException exception)
    {
        log.warn("", exception);

        return responseOf(
            HttpStatus.CONFLICT,
            "The client has disconnected and the task has been terminated.",
            null
        );
    }

    /** Git 或者其他阻塞操作超时时要处理的异常。*/
    @ExceptionHandler(TimeoutException.class)
    public Mono<CustomizedResponse<Object>>
    handleTimeout(TimeoutException exception)
    {
        log.error("", exception);

        return responseOf(
            HttpStatus.BAD_REQUEST,
            "Operator execute timeout, please try again later.",
            null
        );
    }

    /** Git 操作重试多次仍然失败时需要处理的异常。*/
    @ExceptionHandler(GitRetryableException.class)
    public Mono<CustomizedResponse<Object>>
    handleGitRetryable(GitRetryableException exception)
    {
        log.error("", exception);

        return responseOf(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Git operation still fails after multiple retries.",
            null
        );
    }

    /** 兜底异常处理。*/
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<CustomizedResponse<Object>>
    handleAllUncaughtException(Exception ex)
    {
        log.error("Unexpected exception: ", ex);
        return responseOf(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unknow error",
            null
        );
    }
}
