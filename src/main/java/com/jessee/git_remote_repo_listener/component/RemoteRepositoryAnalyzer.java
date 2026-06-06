package com.jessee.git_remote_repo_listener.component;

import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/** Git 远程仓库分析器接口。*/
public interface RemoteRepositoryAnalyzer
{
    /**
     * 调用：
     *
     * <pre>
     *     git config --global --get-all safe.directory
     * </pre>
     *
     * 命令，查询并返回所有被标记为 safe 的本地仓库路径。
     */
    Mono<String> gitConfigGetAllSafeDirectory();

    /**
     * 调用：
     *
     * <pre>
     *     git config --global --add safe.directory (localRepoPath)
     * </pre>
     *
     * 命令，开放指定仓库的操作权限，系统自行调用的时候不会被拒绝。
     *
     * @param localRepoPath 本地仓库路径字符串
     *
     * @return git config 命令没有任何输出，无需向下游传递数据
     */
    Mono<Void> gitConfigAddSafeDirectory(String localRepoPath);

    /**
     * 调用：
     *
     * <pre>
     *     git config --global --unset safe.directory (localRepoPath)
     * </pre>
     *
     * 命令，关闭指定仓库的操作权限。
     */
    Mono<Void> gitConfigDeleteSafeDirectory(String localRepoPath);

    /**
     * 静默调用 git fetch 先从远程仓库拉去最新的变更信息，
     * 再删除远程已经删除的分支引用。
     *
     * @param localRepoPath 本地仓库路径字符串
     */
    Mono<Void> gitFetch(String localRepoPath);

    /**
     * 调用 git for-each-ref 查询指定仓库每一个分支的最新提交，
     * 解析并映射成 Map。
     *
     * @param localRepoPath 本地仓库路径字符串
     * @param remoteName    远程仓库名
     *
     * @return 向下游传递表示分支名与最新提交哈希的关系表，
     *         K 远程分支名，V 最新提交哈希值
     */
    Mono<Map<String, String>>
    gitForeachRef(String localRepoPath, String remoteName);

    /**
     * 调用 git diff 查询并解析有更新的远程分支的详细文件变更状态，
     * 每个文件的变更都会被解析成 {@link FileChange} 最后聚合成列表。
     *
     * @param localRepoPath   本地仓库路径字符串
     * @param branchRefChange 表示远程分支在两次 fetch 快照之间的引用变化。
     *
     * @return 向下游传递该分支每个文件的详细变更列表
     */
    Mono<List<FileChange>>
    gitDiff(String localRepoPath, BranchRefChange branchRefChange);
}