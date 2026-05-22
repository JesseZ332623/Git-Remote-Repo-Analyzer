package com.jessee.git_remote_repo_listener;

import com.jessee.git_remote_repo_listener.scheduler_tasks.RemoteRepositoryAnalyzeSchedulerTasks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/** 远程仓库分析定时任务测试类。*/
@SpringBootTest
public class RemoteRepositoryAnalyzeSchedulerTasksTest
{
    @Autowired
    private RemoteRepositoryAnalyzeSchedulerTasks remoteRepositoryAnalyzeSchedulerTasks;

    @Test
    public void remoteRepoAnalysisTaskTest() throws InterruptedException
    {
        this.remoteRepositoryAnalyzeSchedulerTasks
            .remoteRepoAnalysisTask();

        TimeUnit.MINUTES.sleep(30);
    }
}