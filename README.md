# Git 远程仓库变更分析器服务

<p>
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,redis,spring,lua,git,thymeleaf" alt="技术选型">
    </a>
</p>

使用 Reactor Stream 串联 git 命令形成管道，实现远程仓库变更分析的自动化流水线。

### 管道描述

- [响应式 Git 命令执行器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/src/main/java/com/jessee/git_remote_repo_listener/component/impl/GitCommandRunnerImpl.java)

- [Git 远程仓库分析器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/src/main/java/com/jessee/git_remote_repo_listener/component/impl/RemoteRepositoryAnalyzerImpl.java)

- [远程仓库分析器缓存器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/src/main/java/com/jessee/git_remote_repo_listener/cache/impl/RemoteRepositoryAnalyzerCacherImpl.java)

- [远程仓库分析结果持久化器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/src/main/java/com/jessee/git_remote_repo_listener/service/impl/AnalyzeResultPersisterImpl.java)

- [远程仓库分析结果发送器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/src/main/java/com/jessee/git_remote_repo_listener/service/impl/AnalyzeReportEmailSenderImpl.java)


[Apache License Version 2.0](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/main/LICENSE)

### 2025.05.22
