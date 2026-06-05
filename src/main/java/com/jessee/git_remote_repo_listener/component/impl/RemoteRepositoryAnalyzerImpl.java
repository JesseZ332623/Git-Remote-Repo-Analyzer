package com.jessee.git_remote_repo_listener.component.impl;

import com.jessee.git_remote_repo_listener.component.GitCommandRunner;
import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Git 远程仓库分析器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteRepositoryAnalyzerImpl implements RemoteRepositoryAnalyzer
{
    /** 响应式 Git 命令执行器。*/
    private final GitCommandRunner gitCommandRunner;

    /** 标准化路径。*/
    private static String normalizePath(String path) {
        return Path.of(path).normalize().toString();
    }

    @Override
    public Mono<String> gitConfigGetAllSafeDirectory()
    {
        final List<String> arguments
            = List.of(
                "git", "config",
                "--global", "--get-all",
                "safe.directory"
        );

        return
        this.gitCommandRunner.run(arguments)
            .onErrorReturn(RuntimeException.class, "")
            .doOnError((exception) -> log.error("", exception))
            .doOnSuccess((ignore) ->
                log.info("Query safe directory list.")
            );
    }

    @Override
    public Mono<Void>
    gitConfigAddSafeDirectory(String localRepoPath)
    {
        final String repoPath
            = normalizePath(localRepoPath);

        final List<String> arguments
            = List.of(
                "git", "config",
                "--global", "--add",
                "safe.directory", repoPath
        );

        return
        this.gitCommandRunner.run(arguments)
            .doOnSuccess((ignore) ->
                log.info("Add path: {} to safe.directory config.", repoPath))
            .then();
    }

    @Override
    public Mono<Void>
    gitConfigDeleteSafeDirectory(String localRepoPath)
    {
        final String repoPath
            = normalizePath(localRepoPath).replace("\\", "\\\\");

        final List<String> arguments
            = List.of(
                "git", "config",
                "--global", "--unset",
                "safe.directory", repoPath
        );

        return
        this.gitCommandRunner.run(arguments)
            .doOnSuccess((ignore) ->
                log.info("Delete path: {} to safe.directory config.", repoPath))
            .then();
    }

    @Override
    public Mono<Void> gitFetch(String localRepoPath)
    {
        final String repoPath
            = normalizePath(localRepoPath);

        final List<String> arguments
            = List.of(
                "git",
                "-C", repoPath, "fetch",
                "--all", "--quiet", "--prune"
        );

        return
        this.gitCommandRunner.run(arguments)
            .doOnSuccess((ignore) ->
                log.info("Fetch all remote change information to repository {} success.", repoPath))
            .then();
    }

    @Override
    public Mono<Map<String, String>>
    gitForeachRef(String localRepoPath, String remoteName)
    {
        final String repoPath
            = normalizePath(localRepoPath);

        final String completeRemoteName
            = "refs/remotes/" + remoteName;

        final List<String> arguments = List.of(
                "git",
                "-C", repoPath,
                "for-each-ref", completeRemoteName,
                "--format=%(objectname) %(refname:short)"
        );

        return
        this.gitCommandRunner.run(arguments)
            .map((output) -> {
                /*
                 * 命令的示例输出如下：
                 * e2cb328c564955b508d6d617b032b6c152016c91 origin
                 * e2cb328c564955b508d6d617b032b6c152016c91 origin/develop
                 * a518d68ecd31cca39f484c35f5c2a416f1fd0285 origin/feature/v1.0
                 * 49261aa623834ef35b92b4e4514eb5e6ee294371 origin/feature/v2.0
                 * .....
                 *
                 * 由于第一个 origin 即为refs/remotes/origin/HEAD（指向远程仓库的默认分支），
                 * 先去除第一个 remoteName，再解析成 Map。
                 */
                return
                output.lines()
                       .map((line) -> line.split(" ", 2))
                       .filter((splited) -> !splited[1].equals(remoteName))
                       .collect(
                           Collectors.toMap(
                               (splited) -> splited[1],
                               (splited) -> splited[0],
                               (existing, replacement) -> replacement
                           )
                       );
                })
                .doOnSuccess((ignore) ->
                    log.info("Query latest commit hash of all remote from repository {} success!", repoPath));
    }

    @Override
    public Mono<List<FileChange>>
    gitDiff(String localRepoPath, BranchRefChange branchRefChange)
    {
        // 对于非更新状态的远程分支，执行 git diff 无意义，直接跳过即可。
        if (!branchRefChange.getStatus().equals(RemoteChangeStaus.UPDATE)) {
            return Mono.just(List.of());
        }

        final String repoPath
            = normalizePath(localRepoPath);

        final String commitHash
            = branchRefChange.getPrevLatestCommitHash() + ".." + branchRefChange.getLatestCommitHash();

        final List<String> arguments
            = List.of(
                "git",
                "-C", repoPath,
                "diff", "--name-status",
                commitHash
        );

        return
        this.gitCommandRunner.run(arguments)
            .map((output) ->
                output.lines()
                      .map(FileChange::fromDiffLine)
                      .toList())
            .doOnSuccess((ignore) ->
                log.info(
                    "Parse commit diffrent from {} to {} with repository {} success!",
                    branchRefChange.getPrevLatestCommitHash().substring(0, 8),
                    branchRefChange.getLatestCommitHash().substring(0, 8),
                    repoPath
                )
            );
    }
}