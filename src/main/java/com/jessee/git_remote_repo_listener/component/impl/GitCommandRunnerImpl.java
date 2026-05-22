package com.jessee.git_remote_repo_listener.component.impl;

import com.jessee.git_remote_repo_listener.component.GitCommandRunner;
import com.jessee.git_remote_repo_listener.exception.GitRetryableException;
import com.jessee.git_remote_repo_listener.properties.GitCommandRunnerProperties;
import com.jessee.git_remote_repo_listener.utils.GitRetryableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** 响应式 Git 命令执行器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class GitCommandRunnerImpl implements GitCommandRunner
{
    /** 分析作业专用调度器。*/
    @Qualifier(value = "GitWorkerScheduler")
    private final Scheduler scheduler;

    /** readerThread 的递增唯一编号。*/
    private final AtomicInteger readerCounter = new AtomicInteger(0);

    /** Git 命令执行器配置参数。*/
    private final GitCommandRunnerProperties properties;

    /**
     * 本服务调用 processBuilder.start() 启动  git 进程后，
     * 操作系统内核会维护一个默认为 64KB 管道缓冲区（环形队列），用于进程间通信。
     * 如果管道缓冲区写满但没有任何的消费，进程就会阻塞等待直到超时。
     * 所以必须单开一个线程异步的读取 git 进程的输出，
     * 确保进程不会因为缓冲区写满而阻塞导致最终超时。
     *
     * @param process 已经调用 processBuilder.start() 启动的进程实例
     * @param timeout 分配给线程读取缓冲区的时间（一般是总超时期限的一半）
     */
    private String
    asyncOutputRead(Process process, Duration timeout)
    {
        // 如果管理本异步读取线程的闭锁 wait() 操作超时，
        // 后续的结果读取很有可能会得到脏数据，所以面对这个写多读少的环境，
        // 使用 StringBuffer 是最简单有效的。
        final StringBuffer outputBuilder = new StringBuffer();

        // 使用一个闭锁来控制本线程的等待时间，比 join() 死等更灵活。
        final CountDownLatch latch = new CountDownLatch(1);

        final Thread readerThread
            = Thread.ofVirtual()
                    .name("git-output-reader-", this.readerCounter.incrementAndGet())
                    .start(() -> {
                        try (var reader = process.getInputStream())
                        {
                            final byte[] buffer = new byte[this.properties.getAsyncBufferSize()];
                            int bytesRead;

                            while ((bytesRead = reader.read(buffer)) != -1)
                            {
                                outputBuilder.append(
                                    new String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                                );
                            }
                        }
                        catch (IOException exception)
                        {
                            // 如果是外部主动中断的，则不需要日志记录，
                            // 反之才是真正的 I/O 错误
                            if (!Thread.currentThread().isInterrupted()) {
                                log.warn("Failed to read git output.", exception);
                            }
                        }
                        finally {
                            // 不论读取失败与否，都释放闭锁
                            latch.countDown();
                        }
                    });

        try
        {
            // 等待读取线程，最多等待 timeout
            final boolean finished
                = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished)
            {
                log.warn("Git output reader thread timed out.");
                readerThread.interrupt();
            }
        }
        catch (InterruptedException exception)
        {
            log.warn("Output reader thread was interrupted.", exception);
            Thread.currentThread().interrupt();
            readerThread.interrupt();
        }

        return outputBuilder.toString();
    }

    @Override
    public Mono<String> run(List<String> arguments)
    {
        final long processWaitTimeMs = this.properties.getProcessMaxWaitTime() * 1000L;
        final int maxAttempts        = this.properties.getRetry().getMaxAttempts();
        final Duration minBackoff    = this.properties.getRetry().getMinBackoff();
        final Duration maxBackoff    = this.properties.getRetry().getMaxBackoff();
        final double jitterFactor    = this.properties.getRetry().getJitterFactor();
        final String commandLine     = String.join(" ", arguments);

        final Callable<String> task
            = () ->  {
                final ProcessBuilder processBuilder = new ProcessBuilder(arguments);

                final long deadline = System.currentTimeMillis() + processWaitTimeMs;

                // 合并 stderr 到 stdout
                processBuilder.redirectErrorStream(true);

                final Process process = processBuilder.start();

                // 将总等待时间的一半分配给异步的读取
                final long readTimeoutMs = processWaitTimeMs / 2L;

                final String output
                    = this.asyncOutputRead(process, Duration.ofMillis(readTimeoutMs));

                final long remaingMs
                    = deadline - System.currentTimeMillis();

                // 剩下的时间用于进程的关闭与回收
                final boolean finished
                    = process.waitFor(remaingMs, TimeUnit.MILLISECONDS);

                // 如果关闭超时直接传递异常
                if (!finished)
                {
                    // 强制杀死这个进程
                    process.destroyForcibly();

                    throw new TimeoutException(
                        String.format(
                            "Command %s timed out after %d milliseconds.",
                            commandLine, processWaitTimeMs
                        )
                    );
                }

                final int exitCode = process.exitValue();

                // 非正常退出，则传递异常
                if (exitCode != 0)
                {
                    if (GitRetryableUtils.isRetryable(output))
                    {
                        throw new GitRetryableException(
                            String.format(
                                "Command %s execute retryable failed with exit code: %d, error message: %s",
                                commandLine, exitCode, output
                            )
                        );
                    }

                    throw new RuntimeException(
                        String.format(
                            "Command %s execute failed with exit code: %d, error message: %s",
                            commandLine, exitCode, output
                        )
                    );
                }

                return output;
            };

            final Retry retryStrategy
                = Retry.backoff(maxAttempts, minBackoff)
                       .maxBackoff(maxBackoff)
                       .jitter(jitterFactor)
                       .filter((exception) -> exception instanceof GitRetryableException)
                       .doBeforeRetry((signal) ->
                            log.warn(
                                "Retrying command {}, attempt {} / {} due to: {}",
                                commandLine,
                                signal.totalRetries() + 1,
                                maxAttempts,
                                signal.failure().getMessage()
                            )
                        );

            return
            Mono.fromCallable(task)
                .retryWhen(retryStrategy)
                .subscribeOn(this.scheduler);
    }
}