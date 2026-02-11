package com.filesharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文件分享系统主启动类
 * 
 * @author FileSharing Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class FileSharingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileSharingApplication.class, args);
        System.out.println("==========================================");
        System.out.println("文件分享系统启动成功！");
        System.out.println("API文档地址: http://localhost:8080/api/swagger-ui/");
        System.out.println("==========================================");
    }
}