package com.filesharing.service.impl;

import com.filesharing.dto.UserLoginRequest;
import com.filesharing.dto.UserRegisterRequest;
import com.filesharing.dto.UserResponse;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.UserService;
import com.filesharing.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Override
    public UserResponse register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        
        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setRole(User.UserRole.USER);
        user.setStorageQuota(1073741824L); // 1GB默认配额
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        log.info("用户注册成功: {}", savedUser.getUsername());
        
        return convertToResponse(savedUser);
    }
    
    @Override
    public String login(UserLoginRequest request) {
        // 根据用户名或邮箱查找用户
        User user = userRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 检查用户状态
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException("账户已被禁用");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);
        
        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        log.info("用户登录成功: {}", user.getUsername());
        
        return token;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToResponse(user);
    }
    
    @Override
    public UserResponse updateUser(Long userId, UserRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 检查用户名是否被其他人使用
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        
        // 检查邮箱是否被其他人使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        
        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setAvatar(avatarUrl);
        User updatedUser = userRepository.save(user);
        
        return convertToResponse(updatedUser);
    }
    
    @Override
    public void updateStorageQuota(Long userId, Long newQuota) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setStorageQuota(newQuota);
        userRepository.save(user);
    }
    
    @Override
    public void increaseUsedStorage(Long userId, Long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setUsedStorage(user.getUsedStorage() + size);
        userRepository.save(user);
    }
    
    @Override
    public void decreaseUsedStorage(Long userId, Long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        long newUsedStorage = Math.max(0, user.getUsedStorage() - size);
        user.setUsedStorage(newUsedStorage);
        userRepository.save(user);
    }
    
    @Override
    public boolean hasEnoughStorage(Long userId, Long fileSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        return user.getUsedStorage() + fileSize <= user.getStorageQuota();
    }
    
    @Override
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setStatus(User.UserStatus.DISABLED);
        userRepository.save(user);
        
        log.info("用户被禁用: {}", user.getUsername());
    }
    
    @Override
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        
        log.info("用户被启用: {}", user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public User findUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(HttpServletRequest request) {
        // 从请求头中获取JWT token
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException("未提供有效的认证令牌");
        }
        
        String token = authorizationHeader.substring(7); // 移除 "Bearer " 前缀
        
        try {
            // 验证token并获取用户ID
            Long userId = jwtUtil.getUserIdFromToken(token);
            return findUserById(userId);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            throw new BusinessException("无效的认证令牌");
        }
    }
    
    /**
     * 将User实体转换为UserResponse DTO
     */
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .storageQuota(user.getStorageQuota())
                .usedStorage(user.getUsedStorage())
                .status(user.getStatus().name())
                .role(user.getRole().name())
                .lastLoginTime(user.getLastLoginTime() != null ? 
                    user.getLastLoginTime().toString() : null)
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}