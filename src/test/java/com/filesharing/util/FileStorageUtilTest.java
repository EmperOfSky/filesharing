package com.filesharing.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageUtilTest {

    @TempDir
    Path tempDir;

    private FileStorageUtil fileStorageUtil;

    @BeforeEach
    void setUp() {
        fileStorageUtil = new FileStorageUtil();
        ReflectionTestUtils.setField(fileStorageUtil, "uploadPath", tempDir.resolve("uploads").toString());
        ReflectionTestUtils.setField(fileStorageUtil, "tempPath", tempDir.resolve("temp").toString());
        ReflectionTestUtils.setField(fileStorageUtil, "storageType", "local");
        ReflectionTestUtils.setField(fileStorageUtil, "minioEndpoint", "");
        ReflectionTestUtils.setField(fileStorageUtil, "minioAccessKey", "");
        ReflectionTestUtils.setField(fileStorageUtil, "minioSecretKey", "");
        ReflectionTestUtils.setField(fileStorageUtil, "minioBucket", "filesharing");
        ReflectionTestUtils.setField(fileStorageUtil, "minioBasePath", "");
        fileStorageUtil.init();
    }

    @Test
    void shouldSaveReadCopyAndDeleteInLocalMode() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello-storage".getBytes(StandardCharsets.UTF_8)
        );

        String storageName = fileStorageUtil.saveFile(file);
        assertNotNull(storageName);
        assertTrue(fileStorageUtil.fileExists(storageName));

        byte[] bytes = fileStorageUtil.readFile(storageName);
        assertEquals("hello-storage", new String(bytes, StandardCharsets.UTF_8));

        String copiedStorageName = "copied-" + storageName;
        fileStorageUtil.copyFile(storageName, copiedStorageName, null);
        assertTrue(fileStorageUtil.fileExists(copiedStorageName));

        byte[] copied = fileStorageUtil.readFile(copiedStorageName);
        assertArrayEquals(bytes, copied);

        Resource resource = fileStorageUtil.loadAsResource(storageName, null);
        assertNotNull(resource);
        assertTrue(resource.exists());
        try (InputStream inputStream = resource.getInputStream()) {
            assertArrayEquals(bytes, StreamUtils.copyToByteArray(inputStream));
        }

        assertTrue(fileStorageUtil.deleteFile(storageName));
        assertTrue(fileStorageUtil.deleteFile(copiedStorageName));
        assertFalse(fileStorageUtil.fileExists(storageName));
        assertFalse(fileStorageUtil.fileExists(copiedStorageName));
    }

    @Test
    void shouldFallbackToLocalWhenMinioConfigIncomplete() throws IOException {
        FileStorageUtil fallbackUtil = new FileStorageUtil();
        Path fallbackUpload = tempDir.resolve("fallback-uploads");
        Path fallbackTemp = tempDir.resolve("fallback-temp");

        ReflectionTestUtils.setField(fallbackUtil, "uploadPath", fallbackUpload.toString());
        ReflectionTestUtils.setField(fallbackUtil, "tempPath", fallbackTemp.toString());
        ReflectionTestUtils.setField(fallbackUtil, "storageType", "minio");
        ReflectionTestUtils.setField(fallbackUtil, "minioEndpoint", "");
        ReflectionTestUtils.setField(fallbackUtil, "minioAccessKey", "");
        ReflectionTestUtils.setField(fallbackUtil, "minioSecretKey", "");
        ReflectionTestUtils.setField(fallbackUtil, "minioBucket", "");
        ReflectionTestUtils.setField(fallbackUtil, "minioBasePath", "");
        fallbackUtil.init();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fallback.txt",
                "text/plain",
                "fallback-local".getBytes(StandardCharsets.UTF_8)
        );

        String storageName = fallbackUtil.saveFile(file);
        assertNotNull(storageName);
        assertTrue(fallbackUtil.fileExists(storageName));

        Path localPath = fallbackUpload.resolve(storageName);
        assertTrue(Files.exists(localPath));
        assertEquals("fallback-local", Files.readString(localPath));
    }
}
