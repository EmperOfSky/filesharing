package com.filesharing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityHardeningProperties {

    @Valid
    @NotNull
    private Cors cors = new Cors();

    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Valid
    @NotNull
    private Upload upload = new Upload();

    @Data
    public static class Cors {
        @NotEmpty
        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> allowedOriginPatterns = new ArrayList<>();
    }

    @Data
    public static class RateLimit {
        @Min(1)
        private int globalPerSecond;

        private Map<String, Integer> endpointLimits = new HashMap<>();
    }

    @Data
    public static class Upload {
        @Min(1)
        private long maxFileSizeBytes;

        private boolean enableMalwareScan;

        @NotEmpty
        private Set<String> allowedExtensions = new HashSet<>();

        @NotNull
        private Set<String> blockedExtensions = new HashSet<>();
    }
}
