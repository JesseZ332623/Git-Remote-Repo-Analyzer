package com.jessee.git_remote_repo_listener.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/** 响应式 HTTP 客户端配置类。*/
@Configuration
public class HttpClientConfig
{
    @Bean
    public WebClient webClient()
    {
        // 创建一个HttpClient实例，并为其设定连接超时和响应超时
        final HttpClient httpClient
            = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300000) // 连接超时设为300秒
                .responseTimeout(Duration.ofSeconds(300));            // 响应超时设为300秒

        /*
         * ReactorClientHttpConnector 是
         * WebClient 与 Netty（底层 Http 引擎）之间的桥梁。
         */
        final ClientHttpConnector connector
            = new ReactorClientHttpConnector(httpClient);

        /*
         * ExchangeStrategies
         * 负责配置 WebClient 在处理请求体和响应体时的编解码（codecs）行为。
         */
        final ExchangeStrategies strategies
            = ExchangeStrategies.builder()
            .codecs(ClientCodecConfigurer::defaultCodecs)
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .clientConnector(connector)
            .build();
    }
}