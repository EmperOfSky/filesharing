package com.filesharing.config;

import com.filesharing.security.JwtAuthenticationFilter;
import com.filesharing.security.IpRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 安全配置类
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IpRateLimitFilter ipRateLimitFilter;
    private final SecurityHardeningProperties securityHardeningProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 开启 CSRF 防护，公共接口与 WebSocket 握手路径按需豁免。
                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers(
                        "/api/auth/**",
                        "/api/public/**",
                        "/api/shares/public/**",
                        "/ws/**",
                        "/collab/**",
                        "/api/health")
                .and()

                // 启用CORS
                .cors().configurationSource(corsConfigurationSource())
                .and()

                // 配置会话管理为无状态
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // 配置授权规则
                .authorizeRequests()
                // 公开接口
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .antMatchers("/api/register").permitAll()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/files/public/**").permitAll()
                .antMatchers("/api/shares/public/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/admin/login", "/api/admin/login/").permitAll()
                .antMatchers("/s/**").permitAll()
                .antMatchers("/api/health").permitAll()
                .antMatchers("/error").permitAll()
                .antMatchers("/ws/**").permitAll()
                .antMatchers("/collab/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/backup/**").hasRole("ADMIN")
                .antMatchers("/api/monitoring/**").hasRole("ADMIN")

                // H2控制台（仅开发环境）
                .antMatchers("/h2-console/**").hasRole("ADMIN")

                // Swagger UI（开发调试可访问）
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/swagger-ui/**", "/api/v3/api-docs/**")
                .hasRole("ADMIN")

                // 其他所有请求需要认证
                .anyRequest().authenticated()
                .and()
                .headers()
                .contentTypeOptions()
                .and()
                .xssProtection().block(true)
                .and()
                .frameOptions().sameOrigin()
                .and()
                .exceptionHandling()
                .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(403, "Forbidden"))
                .authenticationEntryPoint((request, response, authException) -> response.sendError(401, "Unauthorized"))
                .and()
                // 在 filterChain 中添加 JWT 解析过滤器
                .addFilterBefore(ipRateLimitFilter, org.springframework.web.filter.CorsFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.web.filter.CorsFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityHardeningProperties.getCors().getAllowedOrigins());
        if (!securityHardeningProperties.getCors().getAllowedOriginPatterns().isEmpty()) {
            configuration.setAllowedOriginPatterns(securityHardeningProperties.getCors().getAllowedOriginPatterns());
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Cache-Control",
                "Pragma"));
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}