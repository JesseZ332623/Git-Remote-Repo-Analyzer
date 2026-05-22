package com.jessee.git_remote_repo_listener.config.converter;

import com.jessee.git_remote_repo_listener.constant.CommitChangeStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** 字符串 -> CommitChangeStatus 枚举转化器。*/
@ReadingConverter
public class CommitChangeStatusReadConverter
    implements Converter<String, CommitChangeStatus>
{
    @Override
    public CommitChangeStatus convert(String source)
    {
        if (source == null || source.isBlank()) {
            return CommitChangeStatus.UNKNOWN;
        }

        return CommitChangeStatus.fromStatus(source);
    }
}