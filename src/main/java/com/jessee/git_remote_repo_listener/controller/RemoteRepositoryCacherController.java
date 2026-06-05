package com.jessee.git_remote_repo_listener.controller;

import com.jessee.git_remote_repo_listener.cache.RemoteRepositoryCacher;
import com.jessee.git_remote_repo_listener.pojo.RemoteRepository;
import com.jessee.git_remote_repo_listener.response.CustomizedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.lang.String.format;

/** 待分析本地仓库缓存控制器类。*/
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/repos-cache")
public class RemoteRepositoryCacherController
{
    /** 待分析仓库缓存操作接口。*/
    private final RemoteRepositoryCacher remoteRepositoryCacher;

    @GetMapping(path = "/repos")
    public Mono<CustomizedResponse<List<RemoteRepository>>>
    getAllRemoteRepos(final ServerHttpResponse response)
    {
        return
        this.remoteRepositoryCacher.getAll()
            .collectList()
            .flatMap((repos) -> {
                if (CollectionUtils.isEmpty(repos))
                {
                    return
                    CustomizedResponse.responseOf(
                        response,
                        HttpStatus.NOT_FOUND,
                        "No remote repositories to be analyzed.",
                       List.of()
                    );
                }

                return
                CustomizedResponse.responseOf(
                    response,
                    HttpStatus.OK,
                    format("%d remote repos to be analyzed.", repos.size()),
                    repos
                );
            });
    }

    @PostMapping(path = "/single")
    public Mono<CustomizedResponse<Object>>
    addOneRepo(
        @RequestBody
        final RemoteRepository   remoteRepository,
        final ServerHttpResponse response
    )
    {
        return
        this.remoteRepositoryCacher.add(remoteRepository)
            .then(
                CustomizedResponse.responseOf(
                    response,
                    HttpStatus.CREATED,
                    "Added successfully.",
                    null
                )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    CustomizedResponse.responseOf(
                        response,
                        HttpStatus.CONFLICT,
                        exception.getMessage(),
                        null
                    )
            );
    }

    @PostMapping(path = "/batch")
    public Mono<CustomizedResponse<Object>>
    addBatchRepos(
        @RequestBody
        final List<RemoteRepository> remoteRepositories,
        final ServerHttpResponse     response
    )
    {
        return
        this.remoteRepositoryCacher.add(remoteRepositories)
            .then(
                CustomizedResponse.responseOf(
                    response,
                    HttpStatus.OK,
                    format("Added successfully. (Batch size: %d)", remoteRepositories.size()),
                    null
                )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    CustomizedResponse.responseOf(
                        response,
                        HttpStatus.CONFLICT,
                        exception.getMessage(),
                        null
                    )
            );
    }

    @PostMapping(path = "/del-single")
    public Mono<CustomizedResponse<Object>>
    deleteSingleRepo(
        @RequestBody
        final RemoteRepository remoteRepository,
        final ServerHttpResponse         response
    )
    {
        return
        this.remoteRepositoryCacher.delete(remoteRepository.getPath())
            .then(
                CustomizedResponse.responseOf(
                    response,
                    HttpStatus.OK,
                    "Deleted successfully.",
                    null
                )
            )
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    CustomizedResponse.responseOf(
                        response,
                        HttpStatus.NOT_FOUND,
                        exception.getMessage(),
                        null
                    )
            );
    }
}