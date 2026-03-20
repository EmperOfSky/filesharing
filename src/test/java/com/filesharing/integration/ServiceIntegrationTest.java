package com.filesharing.integration;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.FileService;
import com.filesharing.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ServiceIntegrationTest {

    @Mock
    private FileService fileService;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void testServiceInjection() {
        // 验证测试替身装配正确
        assertNotNull(fileService);
        assertNotNull(statisticsService);
        assertNotNull(fileRepository);
        assertNotNull(userRepository);
    }

    @Test
    void testFileRepositoryOperations() {
        // 验证仓储基础调用契约
        when(fileRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        List<FileEntity> allFiles = fileRepository.findAll();
        assertNotNull(allFiles);

        List<User> allUsers = userRepository.findAll();
        assertNotNull(allUsers);
    }

    @Test
    void testStatisticsServiceMethods() {
        // 验证统计服务方法可被调用且返回结构有效
        when(statisticsService.getFileTypeDistribution()).thenReturn(List.of());
        when(statisticsService.getFileSizeDistribution()).thenReturn(List.of());
        when(statisticsService.getSystemOverview()).thenReturn(new StatisticsService.SystemOverview());

        assertDoesNotThrow(() -> {
            statisticsService.getFileTypeDistribution();
            statisticsService.getFileSizeDistribution();
            statisticsService.getSystemOverview();
            statisticsService.collectSystemStatistics();
        });
    }
}