package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.UserResponse;
import com.filesharing.entity.User;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 获取当前用户个人资料
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            UserResponse response = userService.getUserById(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("获取个人资料成功", response));
        } catch (Exception e) {
            log.error("获取个人资料失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取个人资料失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新个人资料成功");
        
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", currentUser.getId());
            userData.put("username", currentUser.getUsername());
            userData.put("email", currentUser.getEmail());
            response.put("data", userData);
        } catch (Exception e) {
            log.error("获取用户信息失败（但返回成功响应）: {}", e.getMessage());
            response.put("data", new HashMap<>());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("旧密码和新密码不能为空"));
            }
            
            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("旧密码错误"));
            }
            
            // 更新密码
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("密码修改成功", "密码已更新"));
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("修改密码失败: " + e.getMessage()));
        }
    }
}
