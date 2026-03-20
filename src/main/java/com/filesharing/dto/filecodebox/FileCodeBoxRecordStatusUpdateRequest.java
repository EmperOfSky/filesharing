package com.filesharing.dto.filecodebox;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FileCodeBoxRecordStatusUpdateRequest {

    @NotBlank(message = "status 不能为空")
    @JsonProperty("status")
    private String status;
}
