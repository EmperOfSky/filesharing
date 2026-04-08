package com.filesharing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class AdminBootstrapProperties {

    private boolean enabled = true;

    @NotBlank
    @Size(min = 6, max = 50, message = "默认管理员用户名长度必须在6-50个字符之间")
    private String username = "admin1";

    @NotBlank
    @Size(min = 6, max = 100, message = "默认管理员密码长度必须在6-100个字符之间")
    private String password = "admin1";

    @NotBlank
    private String nickname = "系统管理员";

    @NotBlank
    private String email = "admin1@gmail.com";

    private boolean resetPasswordOnStartup = true;
}