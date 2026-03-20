package com.filesharing.dto.filecodebox;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PresignUploadConfirmRequest {

    @JsonProperty("expire_value")
    private Integer expireValue;

    @JsonProperty("expire_style")
    private String expireStyle;
}
