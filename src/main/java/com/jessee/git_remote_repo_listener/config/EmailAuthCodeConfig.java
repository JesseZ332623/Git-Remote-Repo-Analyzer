package com.jessee.git_remote_repo_listener.config;

import io.github.jessez332623.reactive_email_sender.authorization.EmailServiceAuthCodeGetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.lang.String.format;

/** 邮箱服务授权码获取实现。*/
@Slf4j
@Configuration
public class EmailAuthCodeConfig implements EmailServiceAuthCodeGetter
{
    private final DatabaseClient databaseClient;

    @Value("${app.reactive-email-sender.sender-email}")
    private String senderEmail;

    public EmailAuthCodeConfig(@Qualifier("R2dbcDatabaseClient") DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    /**
     * 获取邮箱授权码的实现。
     *
     * @throws IllegalStateException 查询不到授权码或者授权码为空时抛出，直接终止整个应用。
     */
    @Override
    public String get()
    {
        final String authCodeGetterSQL
            = """
            SELECT
                `email_auth_code`
            FROM
                `email_authenticate`.`email_auth_code`
            WHERE
                email = :email
            """;

        return
        this.databaseClient
            .sql(authCodeGetterSQL)
            .bind("email", senderEmail)
            .fetch()
            .one()
            .map((result) ->
                (String) result.get("email_auth_code"))
            .timeout(Duration.ofSeconds(5L))
            .switchIfEmpty(
                Mono.error(
                    new IllegalStateException(
                        format("Email auth code of: %s is not found!", senderEmail)
                    )
                )
            )
            .filter((authCode) -> !authCode.trim().isEmpty())
            .switchIfEmpty(
                Mono.error(
                    new IllegalStateException(
                        format("Email auth code of: %s is null or empty!", senderEmail)
                    )
                )
            )
            .onErrorResume(Mono::error)
            .block();
    }
}
