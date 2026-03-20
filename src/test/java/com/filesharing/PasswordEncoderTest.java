package com.filesharing;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码编码测试
 */
public class PasswordEncoderTest {

    @Test
    public void testPasswordEncoding() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123";
        
        // 编码密码
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("原始密码: " + rawPassword);
        System.out.println("编码后的密码: " + encodedPassword);
        
        // 验证密码
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("密码匹配: " + matches);
        assertTrue(matches, "密码应该匹配");
        
        // 测试错误密码
        boolean notMatches = encoder.matches("wrongpassword", encodedPassword);
        System.out.println("错误密码匹配: " + notMatches);
        assertFalse(notMatches, "错误密码不应该匹配");
    }
}
