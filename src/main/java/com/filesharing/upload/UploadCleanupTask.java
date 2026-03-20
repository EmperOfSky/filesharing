package com.filesharing.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCleanupTask {

    @Value("${file.upload.temp-path:./temp/}")
    private String tempBasePath;

    @Value("${upload.cleanup.expire-hours:24}")
    private long expireHours;

    // 每小时执行一次
    @Scheduled(cron = "0 0 * * * *")
    public void cleanup() {
        Path base = Paths.get(tempBasePath).toAbsolutePath().normalize();
        if (!Files.exists(base)) return;

        Instant cutoff = Instant.now().minus(expireHours, ChronoUnit.HOURS);
        try (Stream<Path> dirs = Files.list(base)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                try {
                    // 如果目录最后修改时间早于 cutoff，则删除目录及其内容
                    FileTime ft = Files.getLastModifiedTime(dir);
                    if (ft.toInstant().isBefore(cutoff)) {
                        Files.walk(dir)
                                .sorted((a, b) -> b.compareTo(a))
                                .forEach(p -> {
                                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                                });
                        log.info("已清理过期临时上传目录: {}", dir);
                    }
                } catch (Exception e) {
                    log.warn("清理上传临时目录失败: {}", dir, e);
                }
            });
        } catch (IOException e) {
            log.warn("扫描上传临时目录失败: {}", base, e);
        }
    }
}
