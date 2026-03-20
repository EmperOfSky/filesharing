package com.filesharing.dto.filecodebox;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ShareSelectRequest {

    @NotBlank(message = "取件码不能为空")
    @Size(min = 32, max = 32, message = "取件码必须为32位")
    private String code;
}
