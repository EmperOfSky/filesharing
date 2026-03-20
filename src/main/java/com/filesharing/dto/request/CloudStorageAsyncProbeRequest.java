package com.filesharing.dto.request;

import com.filesharing.entity.CloudStorageConfig;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Request for async self-check/probe of cloud-storage capabilities.
 */
@Data
public class CloudStorageAsyncProbeRequest {

    @Min(value = 1, message = "configId must be >= 1")
    private Long configId;

    private CloudStorageConfig.ProviderType providerType;

    @Size(max = 200, message = "endpoint length must be <= 200")
    private String endpoint;

    @Pattern(regexp = "^(https?://).*$", message = "probeUrl must start with http:// or https://")
    private String probeUrl;

    @Min(value = 1, message = "sampleBytes must be >= 1")
    private Integer sampleBytes = 256;
}
