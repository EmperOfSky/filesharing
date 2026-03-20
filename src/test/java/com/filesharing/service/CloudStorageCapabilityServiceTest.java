package com.filesharing.service;

import com.filesharing.dto.request.CloudStorageAsyncProbeRequest;
import com.filesharing.dto.request.CloudStorageValidationRequest;
import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudStorageCapabilityServiceTest {

    @Mock
    private CloudStorageService cloudStorageService;

    @InjectMocks
    private CloudStorageCapabilityService capabilityService;

    @Test
    void getCapabilities_shouldContainExpectedProviders() {
        Map<String, Object> result = capabilityService.getCapabilities();
        assertNotNull(result.get("validation"));
        assertNotNull(result.get("async"));
        assertNotNull(result.get("providers"));
    }

    @Test
    void validateConfig_shouldFailWhenS3FieldsMissing() {
        CloudStorageValidationRequest req = new CloudStorageValidationRequest();
        req.setConfigName("s3-test");
        req.setProviderType(CloudStorageConfig.ProviderType.AWS_S3);

        BusinessException ex = assertThrows(BusinessException.class, () -> capabilityService.validateConfig(req));
        assertTrue(ex.getMessage().contains("missing required fields"));
    }

    @Test
    void validateConfig_shouldPassForOneDrive() {
        CloudStorageValidationRequest req = new CloudStorageValidationRequest();
        req.setConfigName("onedrive-test");
        req.setProviderType(CloudStorageConfig.ProviderType.ONE_DRIVE);
        req.setOneDriveDomain("contoso.onmicrosoft.com");
        req.setOneDriveClientId("client-id");
        req.setOneDriveUsername("user@contoso.com");
        req.setOneDrivePassword("secret");

        Map<String, Object> result = capabilityService.validateConfig(req);
        assertEquals(Boolean.TRUE, result.get("valid"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void asyncProbe_shouldRunStorageProbeWhenConfigIdProvided() {
        CloudStorageService.ConnectionTestResult testResult = new CloudStorageService.ConnectionTestResult();
        testResult.setSuccess(true);
        testResult.setMessage("ok");
        testResult.setEndpoint("http://s3.local");
        testResult.setResponseTime(12L);

        when(cloudStorageService.testConnection(1L)).thenReturn(testResult);

        CloudStorageAsyncProbeRequest req = new CloudStorageAsyncProbeRequest();
        req.setConfigId(1L);
        req.setProbeUrl("http://127.0.0.1:9");
        req.setSampleBytes(32);

        Map<String, Object> result = capabilityService.runAsyncProbe(req);
        Map<String, Object> storageProbe = (Map<String, Object>) result.get("storageProbe");

        assertEquals("tested", storageProbe.get("status"));
        assertEquals(Boolean.TRUE, storageProbe.get("success"));
        assertEquals("ok", storageProbe.get("message"));
    }
}
