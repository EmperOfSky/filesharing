package com.filesharing.dto.filecodebox;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PresignUploadInitRequest {

    @NotBlank(message = "file_name 不能为空")
    @JsonProperty("file_name")
    private String fileName;

    @NotNull(message = "file_size 不能为空")
    @Min(value = 1, message = "file_size 必须大于0")
    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("expire_value")
    private Integer expireValue = 1;

    @JsonProperty("expire_style")
    private String expireStyle = "day";
}
