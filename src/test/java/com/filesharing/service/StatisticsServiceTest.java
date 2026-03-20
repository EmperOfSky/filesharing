package com.filesharing.service;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.ShareRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateFileStatistics_Download() {
        // 准备测试数据
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setDownloadCount(5);
        
        // 执行测试
        statisticsService.updateFileStatistics(file, StatisticsService.StatisticAction.DOWNLOAD);
        
        // 验证结果
        assertEquals(6, file.getDownloadCount());
        assertNotNull(file.getLastDownloadAt());
        verify(fileRepository).save(file);
    }

    @Test
    void testUpdateFileStatistics_Preview() {
        // 准备测试数据
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setPreviewCount(3);
        
        // 执行测试
        statisticsService.updateFileStatistics(file, StatisticsService.StatisticAction.PREVIEW);
        
        // 验证结果
        assertEquals(4, file.getPreviewCount());
        assertNotNull(file.getLastPreviewAt());
        verify(fileRepository).save(file);
    }

    @Test
    void testUpdateFileStatistics_Share() {
        // 准备测试数据
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setShareCount(2);
        
        // 执行测试
        statisticsService.updateFileStatistics(file, StatisticsService.StatisticAction.SHARE);
        
        // 验证结果
        assertEquals(3, file.getShareCount());
        verify(fileRepository).save(file);
    }

    @Test
    void testGetFileTypeDistribution() {
        // 准备测试数据
        FileEntity file1 = new FileEntity();
        file1.setExtension("pdf");
        file1.setFileSize(1024L);
        
        FileEntity file2 = new FileEntity();
        file2.setExtension("pdf");
        file2.setFileSize(2048L);
        
        FileEntity file3 = new FileEntity();
        file3.setExtension("docx");
        file3.setFileSize(512L);
        
        when(fileRepository.findAll()).thenReturn(Arrays.asList(file1, file2, file3));
        
        // 执行测试
        List<StatisticsService.FileTypeDistribution> result = statisticsService.getFileTypeDistribution();
        
        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> "pdf".equals(d.getFileType()) && d.getFileCount() == 2 && d.getTotalSize() == 3072L));
        assertTrue(result.stream().anyMatch(d -> "docx".equals(d.getFileType()) && d.getFileCount() == 1 && d.getTotalSize() == 512L));
    }

    @Test
    void testGetFileSizeDistribution() {
        // 准备测试数据
        FileEntity smallFile = new FileEntity();
        smallFile.setFileSize(512L * 1024); // 512KB
        
        FileEntity mediumFile = new FileEntity();
        mediumFile.setFileSize(5L * 1024 * 1024); // 5MB
        
        FileEntity largeFile = new FileEntity();
        largeFile.setFileSize(50L * 1024 * 1024); // 50MB
        
        FileEntity hugeFile = new FileEntity();
        hugeFile.setFileSize(150L * 1024 * 1024); // 150MB
        
        when(fileRepository.findAll()).thenReturn(Arrays.asList(smallFile, mediumFile, largeFile, hugeFile));
        
        // 执行测试
        List<StatisticsService.FileSizeDistribution> result = statisticsService.getFileSizeDistribution();
        
        // 验证结果
        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(d -> "0-1MB".equals(d.getSizeRange()) && d.getFileCount() == 1));
        assertTrue(result.stream().anyMatch(d -> "1-10MB".equals(d.getSizeRange()) && d.getFileCount() == 1));
        assertTrue(result.stream().anyMatch(d -> "10-100MB".equals(d.getSizeRange()) && d.getFileCount() == 1));
        assertTrue(result.stream().anyMatch(d -> "100MB+".equals(d.getSizeRange()) && d.getFileCount() == 1));
    }

    @Test
    void testGetSystemOverview() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setFileSize(1024L);
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));
        when(fileRepository.findAll()).thenReturn(Arrays.asList(file));
        when(folderRepository.findAll()).thenReturn(Arrays.asList());
        when(shareRepository.findAll()).thenReturn(Arrays.asList());
        
        // 执行测试
        StatisticsService.SystemOverview result = statisticsService.getSystemOverview();
        
        // 验证结果
        assertEquals(1L, result.getTotalUsers());
        assertEquals(1L, result.getTotalFiles());
        assertEquals(0L, result.getTotalFolders());
        assertEquals(0L, result.getTotalShares());
        assertEquals(1024L, result.getTotalStorageUsed());
    }

    @Test
    void testUpdateUserBehavior_Login() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setLastLoginTime(null);
        
        // 执行测试
        statisticsService.updateUserBehaviorStatistics(user, StatisticsService.BehaviorAction.LOGIN);
        
        // 验证结果
        assertNotNull(user.getLastLoginTime());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUserBehavior_StorageChange() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setUsedStorage(100L);
        
        // 执行测试
        statisticsService.updateUserBehaviorStatistics(user, StatisticsService.BehaviorAction.STORAGE_CHANGE, 500L);
        
        // 验证结果
        assertEquals(500L, user.getUsedStorage());
        verify(userRepository).save(user);
    }

    @Test
    void testCollectSystemStatistics() {
        // 执行测试
        assertDoesNotThrow(() -> statisticsService.collectSystemStatistics());
        // 验证没有异常抛出即可
    }
}