package com.filesharing.dto.filecodebox;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ShareSelectRequest {

    @NotBlank(message = "取件码不能为空")
    @Size(min = 8, max = 8, message = "取件码必须为8位")
    @Pattern(regexp = "^\\d{8}$", message = "取件码仅支持8位数字")
    private String code;
}
