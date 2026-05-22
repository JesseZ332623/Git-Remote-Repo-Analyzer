package com.jessee.git_remote_repo_listener;

import com.jessee.git_remote_repo_listener.component.RemoteRepositoryAnalyzer;
import com.jessee.git_remote_repo_listener.pojo.BranchRefChange;
import com.jessee.git_remote_repo_listener.pojo.FileChange;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Git 远程仓库分析器测试类。*/
@Slf4j
@SpringBootTest
class RemoteRepositoryAnalyzerTests
{
	private final static String
	REPOSITORY_PATH = "D:\\ai-by-training";

	private final static String
	REMOTE_NAME = "origin";

	private final static String
	PREV_HASH = "a518d68ecd31cca39f484c35f5c2a416f1fd0285";

	private final static String
	CURRENT_HASH = "e2cb328c564955b508d6d617b032b6c152016c91";

	@Autowired
	private RemoteRepositoryAnalyzer remoteRepositoryAnalyzer;

	@Test
	public void gitFetchTest()
	{
		// git -C <REPOSITORY_PATH> fetch --all --quiet --prune
		StepVerifier.create(this.remoteRepositoryAnalyzer.gitFetch(REPOSITORY_PATH))
			.verifyComplete();
	}

	@Test
	public void gitForeachRefTest()
	{
		// git -C <REPOSITORY_PATH>
		// 	   for-each-ref <refs/remotes/REMOTE_NAME>
		//     --format=%(objectname) %(refname:short)
		StepVerifier.create(
			this.remoteRepositoryAnalyzer
				.gitForeachRef(REPOSITORY_PATH, REMOTE_NAME))
			.expectNextMatches((map) -> {
				map.forEach((k, v) ->
					System.out.printf("%s, %s\n", k, v)
				);

				return
				map.entrySet().stream()
					.allMatch((entry) ->
						entry.getKey().contains(REMOTE_NAME)
						&& entry.getValue().length() == 40
					);
			}).verifyComplete();
	}

	@Test
	public void gitDiff()
	{
		// git -C <REPOSITORY_PATH>
		// 	   diff --name-status PREV_HASH..CURRENT_HASH
		StepVerifier.create(
			this.remoteRepositoryAnalyzer
				.gitDiff(REPOSITORY_PATH, BranchRefChange.of(PREV_HASH, CURRENT_HASH)))
				.expectNextMatches((list) -> {

					// 按照 CommitChangeStatus 每个状态挑 1 个 FileChange 出来
					final List<FileChange> resultMap
						= list.stream().collect(
							Collectors.collectingAndThen(
								Collectors.toMap(
									FileChange::getStatus,
									Function.identity(),
									(a, b) -> a
								),
								(map) ->
									new ArrayList<>(map.values())
							)
						);

					resultMap.forEach(System.out::println);

					// 没什么好验的，确保列表不空即可
					return !resultMap.isEmpty();
				})
				.verifyComplete();
	}
}