package com.filesharing.service;

import com.filesharing.dto.request.CloudStorageAsyncProbeRequest;
import com.filesharing.dto.request.CloudStorageValidationRequest;
import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Capability layer inspired by Python async/storage stack.
 *
 * - strict payload validation (schema-style)
 * - async self checks for file I/O + HTTP + cloud connection
 * - provider capability metadata for S3/OneDrive/OpenDAL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageCapabilityService {

    private final CloudStorageService cloudStorageService;

    public Map<String, Object> getCapabilities() {
        Map<String, Object> result = new HashMap<>();

        result.put("validation", List.of(
                "Bean Validation schema checks",
                "Provider-specific required field checks",
                "Secret masking in validation response"
        ));

        result.put("async", List.of(
                "async file read/write probe (aiofiles equivalent)",
                "async http probe (aiohttp equivalent)",
                "async cloud connection test (aioboto3 equivalent)"
        ));

        List<Map<String, Object>> providers = new ArrayList<>();
        providers.add(provider("AWS_S3", List.of("endpoint", "accessKeyId", "accessKeySecret", "bucketName")));
        providers.add(provider("MINIO", List.of("endpoint", "accessKeyId", "accessKeySecret", "bucketName")));
        providers.add(provider("ONE_DRIVE", List.of("oneDriveDomain", "oneDriveClientId", "oneDriveUsername", "oneDrivePassword")));
        providers.add(provider("OPENDAL", List.of("openDalScheme")));
        result.put("providers", providers);

        return result;
    }

    public Map<String, Object> validateConfig(CloudStorageValidationRequest request) {
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        CloudStorageConfig.ProviderType providerType = request.getProviderType();
        if (providerType == null) {
            throw new BusinessException("providerType cannot be null");
        }

        switch (providerType) {
            case AWS_S3:
            case MINIO:
            case ALIYUN_OSS:
            case TENCENT_COS:
            case QINIU_KODO:
                require(request.getEndpoint(), "endpoint", missing);
                require(request.getAccessKeyId(), "accessKeyId", missing);
                require(request.getAccessKeySecret(), "accessKeySecret", missing);
                require(request.getBucketName(), "bucketName", missing);
                if (providerType == CloudStorageConfig.ProviderType.AWS_S3
                        || providerType == CloudStorageConfig.ProviderType.ALIYUN_OSS
                        || providerType == CloudStorageConfig.ProviderType.TENCENT_COS) {
                    require(request.getRegion(), "region", missing);
                }
                break;
            case ONE_DRIVE:
                require(request.getOneDriveDomain(), "oneDriveDomain", missing);
                require(request.getOneDriveClientId(), "oneDriveClientId", missing);
                require(request.getOneDriveUsername(), "oneDriveUsername", missing);
                require(request.getOneDrivePassword(), "oneDrivePassword", missing);
                break;
            case OPENDAL:
                require(request.getOpenDalScheme(), "openDalScheme", missing);
                if (isBlank(request.getOpenDalOptionsJson())) {
                    warnings.add("openDalOptionsJson is empty, runtime may still require backend-specific options");
                }
                break;
            case LOCAL:
                warnings.add("LOCAL provider does not require cloud credentials");
                break;
            default:
                throw new BusinessException("unsupported providerType: " + providerType);
        }

        if (!missing.isEmpty()) {
            throw new BusinessException("missing required fields: " + String.join(", ", missing));
        }

        Map<String, Object> normalized = new HashMap<>();
        normalized.put("providerType", providerType.name());
        normalized.put("endpoint", trimToNull(request.getEndpoint()));
        normalized.put("bucketName", trimToNull(request.getBucketName()));
        normalized.put("region", trimToNull(request.getRegion()));
        normalized.put("basePath", trimToNull(request.getBasePath()));
        normalized.put("accessKeyIdMasked", mask(request.getAccessKeyId()));
        normalized.put("hasAccessKeySecret", !isBlank(request.getAccessKeySecret()));
        normalized.put("hasOneDrivePassword", !isBlank(request.getOneDrivePassword()));
        normalized.put("openDalScheme", trimToNull(request.getOpenDalScheme()));

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("normalized", normalized);
        result.put("warnings", warnings);
        return result;
    }

    public Map<String, Object> runAsyncProbe(CloudStorageAsyncProbeRequest request) {
        int sampleBytes = request.getSampleBytes() == null ? 256 : Math.max(1, request.getSampleBytes());

        CompletableFuture<Map<String, Object>> fileFuture = CompletableFuture.supplyAsync(() -> runFileProbe(sampleBytes));
        CompletableFuture<Map<String, Object>> storageFuture = CompletableFuture.supplyAsync(() -> runStorageProbe(request));
        CompletableFuture<Map<String, Object>> httpFuture = runHttpProbe(request);

        CompletableFuture.allOf(fileFuture, storageFuture, httpFuture).join();

        Map<String, Object> fileResult = fileFuture.join();
        Map<String, Object> storageResult = storageFuture.join();
        Map<String, Object> httpResult = httpFuture.join();

        boolean fileOk = Boolean.TRUE.equals(fileResult.get("success"));
        boolean storageOk = Boolean.TRUE.equals(storageResult.get("success")) || "skipped".equals(storageResult.get("status"));
        boolean httpOk = Boolean.TRUE.equals(httpResult.get("success"));

        Map<String, Object> result = new HashMap<>();
        result.put("success", fileOk && storageOk && httpOk);
        result.put("fileProbe", fileResult);
        result.put("storageProbe", storageResult);
        result.put("httpProbe", httpResult);
        return result;
    }

    private CompletableFuture<Map<String, Object>> runHttpProbe(CloudStorageAsyncProbeRequest request) {
        String probeUrl = trimToNull(request.getProbeUrl());
        if (probeUrl == null && request.getEndpoint() != null && request.getEndpoint().startsWith("http")) {
            probeUrl = request.getEndpoint();
        }
        if (probeUrl == null) {
            probeUrl = "http://localhost:18080/api/health";
        }
        final String resolvedProbeUrl = probeUrl;

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(resolvedProbeUrl))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        long start = System.currentTimeMillis();
        return client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((resp, ex) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("url", resolvedProbeUrl);
                    map.put("elapsedMs", System.currentTimeMillis() - start);
                    if (ex != null) {
                        map.put("success", false);
                        map.put("message", "http probe failed: " + ex.getMessage());
                    } else {
                        int statusCode = resp.statusCode();
                        map.put("statusCode", statusCode);
                        map.put("success", statusCode >= 200 && statusCode < 500);
                        map.put("message", "http probe finished");
                    }
                    return map;
                });
    }

    private Map<String, Object> runFileProbe(int sampleBytes) {
        long start = System.currentTimeMillis();
        Map<String, Object> map = new HashMap<>();
        try {
            Path dir = Path.of("temp", "async-probe").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path file = dir.resolve("probe.bin");

            byte[] data = new byte[sampleBytes];
            for (int i = 0; i < sampleBytes; i++) {
                data[i] = (byte) (i % 127);
            }
            Files.write(file, data);
            byte[] read = Files.readAllBytes(file);

            map.put("success", read.length == data.length);
            map.put("bytes", read.length);
            map.put("path", file.toString());
            map.put("elapsedMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "file probe failed: " + e.getMessage());
            map.put("elapsedMs", System.currentTimeMillis() - start);
        }
        return map;
    }

    private Map<String, Object> runStorageProbe(CloudStorageAsyncProbeRequest request) {
        long start = System.currentTimeMillis();
        Map<String, Object> map = new HashMap<>();
        try {
            if (request.getConfigId() == null) {
                map.put("status", "skipped");
                map.put("success", true);
                map.put("message", "configId not provided, skipped provider connection test");
                map.put("elapsedMs", System.currentTimeMillis() - start);
                return map;
            }

            CloudStorageService.ConnectionTestResult test = cloudStorageService.testConnection(request.getConfigId());
            map.put("status", "tested");
            map.put("success", Boolean.TRUE.equals(test.getSuccess()));
            map.put("message", test.getMessage());
            map.put("endpoint", test.getEndpoint());
            map.put("responseTime", test.getResponseTime());
            map.put("elapsedMs", System.currentTimeMillis() - start);
            return map;
        } catch (Exception e) {
            map.put("status", "failed");
            map.put("success", false);
            map.put("message", "storage probe failed: " + e.getMessage());
            map.put("elapsedMs", System.currentTimeMillis() - start);
            return map;
        }
    }

    private Map<String, Object> provider(String providerType, List<String> requiredFields) {
        Map<String, Object> map = new HashMap<>();
        map.put("providerType", providerType);
        map.put("requiredFields", requiredFields);
        return map;
    }

    private void require(String value, String field, List<String> missing) {
        if (isBlank(value)) {
            missing.add(field);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    private String mask(String value) {
        String text = trimToNull(value);
        if (text == null || text.length() <= 4) {
            return text == null ? null : "****";
        }
        return text.substring(0, 2) + "****" + text.substring(text.length() - 2);
    }
}
