package com.jessee.git_remote_repo_listener.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpResponse;

/** 自定义响应体。*/
@Getter
@Builder
@RequiredArgsConstructor
public class CustomizedResponse<T>
{
    private final long timestamp = System.currentTimeMillis();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> Mono<CustomizedResponse<T>>
    responseOf(
        final ServerHttpResponse response,
        final HttpStatus         status,
        final String             message,
        final T                  data
    )
    {
        return Mono.fromCallable(() -> {
            response.setStatusCode(status);
            return new CustomizedResponse<>(message, data);
        });
    }
}
