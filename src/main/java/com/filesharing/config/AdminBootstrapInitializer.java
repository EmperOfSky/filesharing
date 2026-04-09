package com.filesharing.config;

import com.filesharing.entity.User;
import com.filesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            String username = sanitizeCredential(properties.getUsername(), "admin1", 6, "用户名");
            String rawPassword = sanitizeCredential(properties.getPassword(), "admin1", 6, "密码");
            String configuredEmail = normalize(properties.getEmail(), username + "@filesharing.local");

            User admin = userRepository.findByUsername(username).orElse(null);

            if (admin == null) {
                admin = new User();
                admin.setUsername(username);
                admin.setEmail(resolveAvailableAdminEmail(configuredEmail, username));
                admin.setNickname(StringUtils.hasText(properties.getNickname()) ? properties.getNickname().trim() : "系统管理员");
                admin.setPassword(passwordEncoder.encode(rawPassword));
                admin.setStatus(User.UserStatus.ACTIVE);
                admin.setRole(User.UserRole.ADMIN);
                admin.setStorageQuota(1073741824L);
                admin.setUsedStorage(0L);

                userRepository.save(admin);
                log.info("已创建默认管理员账号: username={}, role=ADMIN", username);
                return;
            }

            boolean changed = false;

            if (admin.getRole() != User.UserRole.ADMIN) {
                admin.setRole(User.UserRole.ADMIN);
                changed = true;
            }

            if (admin.getStatus() != User.UserStatus.ACTIVE) {
                admin.setStatus(User.UserStatus.ACTIVE);
                changed = true;
            }

            if (properties.isResetPasswordOnStartup()) {
                admin.setPassword(passwordEncoder.encode(rawPassword));
                changed = true;
            }

            if (StringUtils.hasText(properties.getNickname()) && !properties.getNickname().trim().equals(admin.getNickname())) {
                admin.setNickname(properties.getNickname().trim());
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                log.info("已同步默认管理员账号: username={}, role=ADMIN", admin.getUsername());
            } else {
                log.info("默认管理员账号已存在，无需更新: username={}", admin.getUsername());
            }
        } catch (Exception ex) {
            log.error("默认管理员账号初始化失败", ex);
        }
    }

    private String resolveAvailableAdminEmail(String preferredEmail, String username) {
        String email = normalize(preferredEmail, username + "@filesharing.local");
        if (!userRepository.existsByEmail(email)) {
            return email;
        }

        String fallback = username + "@filesharing.local";
        if (!userRepository.existsByEmail(fallback)) {
            return fallback;
        }

        return username + "+" + System.currentTimeMillis() + "@filesharing.local";
    }

    private String normalize(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private String sanitizeCredential(String value, String fallback, int minLength, String fieldName) {
        String normalized = normalize(value, fallback);
        if (normalized.length() >= minLength) {
            return normalized;
        }
        log.warn("默认管理员{}长度不足{}，已回退到默认值", fieldName, minLength);
        return fallback;
    }
}