package com.filesharing.config;

import com.filesharing.service.ChunkUploadService;
import com.filesharing.service.FileCodeBoxService;
import com.filesharing.service.FileService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置类
 * 负责定期清理临时文件和过期上传记录
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {
    
    private final FileStorageUtil fileStorageUtil;
    private final ChunkUploadService chunkUploadService;
    private final FileService fileService;
    private final FileCodeBoxService fileCodeBoxService;
    
    /**
     * 每天凌晨2点清理临时文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempFiles() {
        try {
            log.info("开始清理临时文件...");
            fileStorageUtil.cleanupTempFiles();
            log.info("临时文件清理完成");
        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
    }
    
    /**
     * 每小时清理过期的分片上传记录
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredUploads() {
        try {
            log.debug("开始清理过期上传记录...");
            chunkUploadService.cleanupExpiredUploads();
            log.debug("过期上传记录清理完成");
        } catch (Exception e) {
            log.error("清理过期上传记录失败", e);
        }
    }
    
    /**
     * 每天凌晨3点清理已删除的文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupDeletedFiles() {
        try {
            log.info("开始清理已删除文件...");
            fileService.cleanupDeletedFiles();
            log.info("已删除文件清理完成");
        } catch (Exception e) {
            log.error("清理已删除文件失败", e);
        }
    }

    /**
     * 每 30 分钟清理一次 FileCodeBox 过期数据和临时会话。
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanupFileCodeBoxData() {
        try {
            fileCodeBoxService.cleanupExpiredData();
        } catch (Exception e) {
            log.error("清理 FileCodeBox 过期数据失败", e);
        }
    }
}