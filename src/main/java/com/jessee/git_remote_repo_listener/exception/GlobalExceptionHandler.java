package com.jessee.git_remote_repo_listener.exception;

import com.jessee.git_remote_repo_listener.response.CustomizedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jessee.git_remote_repo_listener.response.CustomizedResponse.responseOf;

/** 本服务全局异常处理器。*/
@Slf4j
@RestControllerAdvice(
    // 可以只对指定的类生效，这里暂定为全局处理
    assignableTypes = {
        // RemoteRepositoryAnalyzeController.class
        /* ... */
    }
)
public class GlobalExceptionHandler
{
    private static final
    Pattern DOUBLE_QUOTE_EXTRACT = Pattern.compile("\"([^\"]*)\"");

    /** Git 异步缓冲区读取线程或者进程等待线程被外部中断时要处理的异常。*/
    @ExceptionHandler(ClientAbortException.class)
    public Mono<CustomizedResponse<Object>>
    handleClientAbort(
        final ServerHttpResponse   response,
        final ClientAbortException exception
    )
    {
        log.warn("", exception);

        return responseOf(
            response,
            HttpStatus.CONFLICT,
            "The client has disconnected and the task has been terminated.",
            null
        );
    }

    /** Git 或者其他阻塞操作超时时要处理的异常。*/
    @ExceptionHandler(TimeoutException.class)
    public Mono<CustomizedResponse<Object>>
    handleTimeout(
        final ServerHttpResponse response,
        final TimeoutException   exception
    )
    {
        log.error(
            "Operator execute timeout, please try again later. Caused by: {}",
            exception.getMessage()
        );

        return responseOf(
            response,
            HttpStatus.BAD_REQUEST,
            "Operator execute timeout, please try again later. " +
                "Caused by: " + exception.getMessage(),
            null
        );
    }

    /** Git 操作重试多次仍然失败时需要处理的异常。*/
    @ExceptionHandler(GitRetryableException.class)
    public Mono<CustomizedResponse<Object>>
    handleGitRetryable(
        final ServerHttpResponse    response,
        final GitRetryableException exception
    )
    {
        log.error("", exception);

        return responseOf(
            response,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Git operation still fails after multiple retries.",
            null
        );
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public Mono<CustomizedResponse<Object>>
    handleMissingRequestValue(
        final ServerHttpResponse           response,
        final MissingRequestValueException exception
    )
    {
        log.warn("", exception);

        return
        Mono.defer(() -> {
            final Matcher errorMessageMatcher
                = DOUBLE_QUOTE_EXTRACT.matcher(exception.getMessage());

            return responseOf(
                response,
                HttpStatus.BAD_REQUEST,
                (errorMessageMatcher.find())
                    ? errorMessageMatcher.group(1)
                    : "Missing Request Value....",
                null
            );
        });
    }

    /** 兜底异常处理。*/
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<CustomizedResponse<Object>>
    handleAllUncaughtException(
        final ServerHttpResponse  response,
        final Exception          exception)
    {
        log.error("Unexpected exception: ", exception);

        return responseOf(
            response,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unknow error",
            null
        );
    }
}