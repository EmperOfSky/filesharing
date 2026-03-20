package com.filesharing.dto.filecodebox;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ShareSelectRequest {

    @NotBlank(message = "取件码不能为空")
    private String code;
}
