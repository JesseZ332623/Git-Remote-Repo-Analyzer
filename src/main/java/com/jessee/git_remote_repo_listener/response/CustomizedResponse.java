package com.jessee.git_remote_repo_listener.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/** 自定义响应体。*/
@Getter
@Builder
@RequiredArgsConstructor
public class CustomizedResponse<T>
{
    private final long timestamp = System.currentTimeMillis();

    private final HttpStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> Mono<CustomizedResponse<T>>
    responseOf(HttpStatus status, String message, T data)
    {
        return
        Mono.just(new CustomizedResponse<>(status, message, data));
    }
}
