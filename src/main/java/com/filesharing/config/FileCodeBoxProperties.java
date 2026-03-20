package com.filesharing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 快传中心运行配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fcb")
public class FileCodeBoxProperties {

    /**
     * 是否允许游客上传。
     */
    private boolean openUpload = true;

    /**
     * 单文件最大大小，默认 10MB。
     */
    private long uploadSize = 10L * 1024L * 1024L;

    /**
     * 上传频率限制（窗口内最大次数）。
     */
    private int uploadCount = 10;

    /**
     * 上传频率限制窗口（分钟）。
     */
    private int uploadMinute = 1;

    /**
     * 取件码错误频率限制（窗口内最大次数）。
     */
    private int errorCount = 10;

    /**
     * 取件码错误窗口（分钟）。
     */
    private int errorMinute = 1;

    /**
     * 最长保存秒数，0 表示不启用该限制。
     */
    private long maxSaveSeconds = 0L;

    /**
     * 允许的过期策略。
     */
    private List<String> expireStyles = Arrays.asList("day", "hour", "minute", "forever", "count");

    /**
     * 预签名上传 URL 有效期（秒）。
     */
    private int presignExpireSeconds = 900;

    /**
     * 下载令牌有效期（秒）。
     */
    private int downloadTokenTtlSeconds = 600;
}
