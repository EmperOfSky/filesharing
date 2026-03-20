package com.filesharing.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    private SecretKey secretKey;
    @Value("${jwt.secret:}")
    private String jwtSecret;

    public JwtUtil() {
        // 保留无参构造，实际密钥在 init() 中初始化
    }

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = jwtSecret != null && !jwtSecret.isEmpty() ? jwtSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
            // 确保密钥长度至少为64字节（512位），满足HS512的要求
            if (keyBytes.length < 64) {
                // 如果密钥太短，使用SHA-256多次扩展
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] extended = new byte[64];
                for (int i = 0; i < 64; i += 32) {
                    byte[] hash = md.digest(i == 0 ? keyBytes : md.digest(keyBytes));
                    System.arraycopy(hash, 0, extended, i, Math.min(32, 64 - i));
                }
                keyBytes = extended;
            }
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT密钥已初始化，长度: {} 字节", keyBytes.length);
        } catch (Exception e) {
            log.warn("无法初始化JWT签名密钥，使用随机密钥: {}", e.getMessage());
            this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    @Value("${jwt.expiration:86400000}") // 24小时，默认毫秒
    private Long expiration;

    /**
     * 生成JWT令牌
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从JWT令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从JWT令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从JWT令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 检查JWT令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 刷新JWT令牌
     */
    public String refreshToken(String token) {
        String username = getUsernameFromToken(token);
        Long userId = getUserIdFromToken(token);
        return generateToken(userId, username);
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return secretKey;
    }

    /**
     * 从令牌中提取Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}