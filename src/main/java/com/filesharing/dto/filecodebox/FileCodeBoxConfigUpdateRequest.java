package com.filesharing.dto.filecodebox;

import lombok.Data;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * FileCodeBox 兼容配置更新请求。
 * 所有字段均为可选，未传字段保持原值。
 */
@Data
public class FileCodeBoxConfigUpdateRequest {

    private Boolean openUpload;

    @Min(value = 1, message = "upload_size 必须大于0")
    private Long uploadSize;

    @Min(value = 1, message = "upload_count 必须大于0")
    private Integer uploadCount;

    @Min(value = 1, message = "upload_minute 必须大于0")
    private Integer uploadMinute;

    @Min(value = 1, message = "error_count 必须大于0")
    private Integer errorCount;

    @Min(value = 1, message = "error_minute 必须大于0")
    private Integer errorMinute;

    @Min(value = 0, message = "max_save_seconds 不能小于0")
    private Long maxSaveSeconds;

    private List<String> expireStyles;

    @Min(value = 60, message = "presign_expire_seconds 不能小于60")
    private Integer presignExpireSeconds;

    @Min(value = 60, message = "download_token_ttl_seconds 不能小于60")
    private Integer downloadTokenTtlSeconds;
}
