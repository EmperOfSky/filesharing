package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.UserLoginRequest;
import com.filesharing.dto.UserRegisterRequest;
import com.filesharing.dto.UserResponse;
import com.filesharing.entity.User;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody UserRegisterRequest request) {
        try {
            // 手动验证
            if (request.getUsername() == null || request.getUsername().trim().isEmpty() || request.getUsername().length() < 3 || request.getUsername().length() > 50) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名长度必须在3-50个字符之间"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty() || !request.getEmail().contains("@")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("邮箱格式不正确"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码不能为空"));
            }
            if (!STRONG_PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码需8-64位，且包含大小写字母、数字和特殊字符"));
            }
            
            UserResponse response = userService.register(request);
            return ResponseEntity.ok(ApiResponse.success("注册成功", response));
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody UserLoginRequest request) {
        try {
            // 检查请求对象
            if (request == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请求体为空"));
            }
            
            String identifier = request.getIdentifier();
            String password = request.getPassword();
            
            log.info("登录请求 - identifier: {}, password: {}", identifier, password != null ? "[密码已接收]" : "[密码为空]");
            
            // 手动验证
            if (identifier == null || identifier.trim().isEmpty()) {
                log.warn("登录验证失败: 用户名为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名或邮箱不能为空"));
            }
            if (password == null || password.trim().isEmpty()) {
                log.warn("登录验证失败: 密码为空");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码不能为空"));
            }
            
            String token = userService.login(request);
            return ResponseEntity.ok(ApiResponse.success("登录成功", token));
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            UserResponse userResponse = userService.getUserById(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}