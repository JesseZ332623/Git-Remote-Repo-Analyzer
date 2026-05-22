package com.jessee.git_remote_repo_listener.config.converter;

import com.jessee.git_remote_repo_listener.constant.CommitChangeStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** CommitChangeStatus -> 字符串 枚举转化器。*/
@ReadingConverter
public class CommitChangeStatusWriteConverter
    implements Converter<CommitChangeStatus, String>
{
    @Override
    public String convert(CommitChangeStatus source) {
        return source == null ? null : source.getStatus();
    }
}