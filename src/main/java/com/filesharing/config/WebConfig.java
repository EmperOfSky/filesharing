package com.filesharing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityHardeningProperties securityHardeningProperties;

    public WebConfig(SecurityHardeningProperties securityHardeningProperties) {
        this.securityHardeningProperties = securityHardeningProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = securityHardeningProperties.getCors().getAllowedOrigins();
        List<String> originPatterns = securityHardeningProperties.getCors().getAllowedOriginPatterns();

        var mapping = registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-XSRF-TOKEN",
                        "X-Requested-With",
                        "Accept",
                        "Origin",
                        "Cache-Control",
                        "Pragma")
                .allowCredentials(true)
                .maxAge(3600);

        if (!originPatterns.isEmpty()) {
            mapping.allowedOriginPatterns(originPatterns.toArray(new String[0]));
        }
    }
}