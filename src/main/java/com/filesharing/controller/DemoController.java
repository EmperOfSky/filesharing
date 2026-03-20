package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.UserRegisterRequest;
import com.filesharing.dto.UserResponse;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoController {
    
    private final UserService userService;
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("系统运行正常", "OK"));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerDemoUser(@RequestBody UserRegisterRequest request) {
        try {
            UserResponse userResponse = userService.register(request);
            return ResponseEntity.ok(ApiResponse.success("用户注册成功", userResponse));
        } catch (Exception e) {
            log.error("注册失败: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("注册失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<String>> getSystemInfo() {
        String info = "文件共享系统 v1.0\n" +
                     "功能特性:\n" +
                     "- 用户注册登录\n" +
                     "- 文件上传下载\n" +
                     "- 文件夹管理\n" +
                     "- 权限控制\n" +
                     "- 协作功能\n" +
                     "- 搜索功能";
        return ResponseEntity.ok(ApiResponse.success("系统信息", info));
    }
}