package com.jessee.git_remote_repo_listener.cache;

import reactor.core.publisher.Mono;

import java.util.List;

/** 邮件收件人缓存类接口。*/
public interface EmailRecipientCacher
{
    /**
     * 往缓存写入一个收件人邮箱地址。
     *
     * @param name    收件人姓名
     * @param address 收件人邮箱地址
     * */
    Mono<Void> addOneRecipientCacher(String name, String address);

    /** 按收件人姓名查询对应的邮箱地址。*/
    Mono<String> getRecipientAddress(String name);

    /** 查询当前缓存中所有收件人的邮箱地址。*/
    Mono<List<String>> getAllRecipientAddress();
}