package com.filesharing.dto.request;

import com.filesharing.entity.CloudStorageConfig;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Cloud storage config validation request.
 *
 * This request is used for strict validation before persisting a cloud config,
 * similar to schema-first validation in Python examples.
 */
@Data
public class CloudStorageValidationRequest {

    @NotBlank(message = "configName cannot be blank")
    @Size(max = 100, message = "configName length must be <= 100")
    private String configName;

    @NotNull(message = "providerType cannot be null")
    private CloudStorageConfig.ProviderType providerType;

    @Size(max = 200, message = "endpoint length must be <= 200")
    private String endpoint;

    @Size(max = 100, message = "accessKeyId length must be <= 100")
    private String accessKeyId;

    @Size(max = 200, message = "accessKeySecret length must be <= 200")
    private String accessKeySecret;

    @Size(max = 100, message = "bucketName length must be <= 100")
    private String bucketName;

    @Size(max = 50, message = "region length must be <= 50")
    private String region;

    @Size(max = 200, message = "basePath length must be <= 200")
    private String basePath;

    @Size(max = 200, message = "oneDriveDomain length must be <= 200")
    private String oneDriveDomain;

    @Size(max = 100, message = "oneDriveClientId length must be <= 100")
    private String oneDriveClientId;

    @Size(max = 200, message = "oneDriveUsername length must be <= 200")
    private String oneDriveUsername;

    @Size(max = 200, message = "oneDrivePassword length must be <= 200")
    private String oneDrivePassword;

    @Size(max = 80, message = "openDalScheme length must be <= 80")
    private String openDalScheme;

    @Size(max = 2000, message = "openDalOptionsJson length must be <= 2000")
    private String openDalOptionsJson;

    @Pattern(regexp = "^(https?://).*$", message = "probeUrl must start with http:// or https://")
    private String probeUrl;
}
