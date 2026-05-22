package com.jessee.git_remote_repo_listener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Spring 模板引擎配置，用于 Thymeleaf Template 生成。*/
@Slf4j
@Configuration
public class ThymeleafConfig
{
    /** 邮件模板的存放路径。*/
    @Value("${app.thymeleaf.prefix}")
    private String prefix;

    @Bean
    public SpringWebFluxTemplateEngine templateEngine()
    {
        final SpringWebFluxTemplateEngine templateEngine
            = new SpringWebFluxTemplateEngine();

        final ClassLoaderTemplateResolver
            resolver = new ClassLoaderTemplateResolver();

        resolver.setPrefix(prefix);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true); // 生产环境开启缓存

        templateEngine.setTemplateResolver(resolver);

        return templateEngine;
    }
}