package com.jessee.git_remote_repo_listener.properties;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/** 本地待分析仓库路径配置类。*/
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "app.local-repository-path")
public class RepoPathProperties
{
    private List<RepoConfig> repos;

    /**
     * 在仓库配置好了后做校验，
     * 若出现重复的本地仓库路径，整个服务不得启动。
     */
    @PostConstruct
    private void repoValidate()
    {
        final long distinctPaths
            = this.repos.stream()
                  .map(RepoConfig::getPath)
                  .map(Path::of)
                  .map(Path::normalize)
                  .distinct()
                  .count();

        if (distinctPaths != this.repos.size())
        {
            throw new IllegalStateException(
                "Duplicate repository path in the configuration be detected."
            );
        }
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepoConfig
    {
        private String path;
        private String remote;

        /**
         * 获取整个路径中最后一段目录名。
         * 例：D:\a\b\c\ai-by-training -> ai-by-training
         */
        public String getDirectoryName() {
            return Path.of(this.path).normalize().getFileName().toString();
        }
    }
}