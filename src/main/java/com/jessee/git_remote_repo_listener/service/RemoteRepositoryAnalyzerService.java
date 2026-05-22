package com.jessee.git_remote_repo_listener.service;

import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.constant.RemoteChangeStaus;
import com.jessee.git_remote_repo_listener.pojo.AnalyzeResult;
import com.jessee.git_remote_repo_listener.pojo.BranchFileChanges;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.utils.RemoteSnapshotUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 远程仓库分析服务类接口。*/
public interface RemoteRepositoryAnalyzerService
{
    /**
     * 读取并分析配置中给出的所以本地仓库路径和远程名（全量分析）。
     * 整个流水线的步骤如下：
     *
     * <ol>
     *     <li>
     *         调用 {@link RemoteRepositoryAnalyzer#gitForeachRef(String, String)}
     *         查询本地仓库每个分支的最新提交哈希，并将该结果缓存至 Redis。
     *     </li>
     *
     *     <li>
     *         调用 {@link RemoteRepositoryAnalyzer#gitFetch(String)}
     *         从远程仓库拉取变更信息。
     *     </li>
     *
     *     <li>
     *         再次调用 {@link RemoteRepositoryAnalyzer#gitForeachRef(String, String)}
     *         查询当前本地仓库每个分支的最新提交哈希，
     *         并调用 {@link RemoteSnapshotUtils#compareRemoteHash(Map, Map)} 与缓存中的快照进行比对。
     *        （如果本地已经和远程同步，则直接去查询缓存）。
     *     </li>
     *
     *     <li>
     *        对每个状态为 {@link RemoteChangeStaus#UPDATE} 分支调用
     *        {@link RemoteRepositoryAnalyzer#gitDiff(String, BranchRefChange)}
     *        查询并解析有更新的远程分支的详细文件变更状态。
     *     </li>
     *
     *     <li>最后聚合为 {@link BranchFileChanges} 列表</li>
     * </ol>
     */
    Mono<AnalyzeResult> doAnalysis();
}