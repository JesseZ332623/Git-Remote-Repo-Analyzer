# Git 远程仓库变更分析器服务

<p>
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,redis,spring,lua,git,thymeleaf" alt="技术选型">
    </a>
</p>

使用 Reactor Stream 串联 git 命令形成管道，实现远程仓库变更分析的自动化流水线。

### 管道描述

- [Redisson 分布式锁封装工具组件](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/component/impl/RedissonLockerImpl.java)

- [响应式 Git 命令执行器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/component/impl/GitCommandRunnerImpl.java)

- [Git 远程仓库分析器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/component/impl/RemoteRepositoryAnalyzerImpl.java)

- [远程仓库分析结果缓存器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/cache/impl/RemoteRepositoryAnalyzerCacherImpl.java)

- [远程仓库分析结果持久化器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/service/persistence/impl/AnalyzeResultPersisterImpl.java)

- [远程仓库分析结果邮件发送器](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/src/main/java/com/jessee/git_remote_repo_listener/service/impl/AnalyzeReportEmailSenderImpl.java)

### 文档

- [分析结果邮件示例](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/document/Gmail%20-%20Git%20%E8%BF%9C%E7%A8%8B%E4%BB%93%E5%BA%93%E5%8F%98%E6%9B%B4%E6%8A%A5%E5%91%8A%E7%A4%BA%E4%BE%8B.pdf)

### LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Git-Remote-Repo-Analyzer/blob/develop/LICENSE)

### 2025.06.05
