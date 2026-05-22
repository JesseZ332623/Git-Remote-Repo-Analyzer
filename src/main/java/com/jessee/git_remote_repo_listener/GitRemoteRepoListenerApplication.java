package com.jessee.git_remote_repo_listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Git 远程仓库变更分析器服务启动类。*/
@EnableScheduling
@SpringBootApplication
public class GitRemoteRepoListenerApplication
{
	public static void main(String[] args) {
		SpringApplication.run(GitRemoteRepoListenerApplication.class, args);
	}
}