package com.filesharing.service;

import com.filesharing.dto.UserLoginRequest;
import com.filesharing.dto.UserRegisterRequest;
import com.filesharing.dto.UserResponse;
import com.filesharing.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    UserResponse register(UserRegisterRequest request);
    
    /**
     * 用户登录
     */
    String login(UserLoginRequest request);
    
    /**
     * 根据ID获取用户信息
     */
    UserResponse getUserById(Long userId);
    
    /**
     * 根据用户名获取用户信息
     */
    UserResponse getUserByUsername(String username);
    
    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long userId, UserRegisterRequest request);
    
    /**
     * 更新用户头像
     */
    UserResponse updateAvatar(Long userId, String avatarUrl);
    
    /**
     * 更新存储配额
     */
    void updateStorageQuota(Long userId, Long newQuota);
    
    /**
     * 增加已使用存储空间
     */
    void increaseUsedStorage(Long userId, Long size);
    
    /**
     * 减少已使用存储空间
     */
    void decreaseUsedStorage(Long userId, Long size);
    
    /**
     * 检查存储空间是否足够
     */
    boolean hasEnoughStorage(Long userId, Long fileSize);
    
    /**
     * 禁用用户
     */
    void disableUser(Long userId);
    
    /**
     * 启用用户
     */
    void enableUser(Long userId);
    
    /**
     * 根据用户名或邮箱查找用户实体
     */
    User findUserByIdentifier(String identifier);
    
    /**
     * 根据ID查找用户实体
     */
    User findUserById(Long userId);
}