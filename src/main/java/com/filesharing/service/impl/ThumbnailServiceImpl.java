package com.filesharing.service.impl;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Thumbnail;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.ThumbnailRepository;
import com.filesharing.service.ThumbnailService;
import com.filesharing.util.ThumbnailGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 缩略图服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ThumbnailServiceImpl implements ThumbnailService {
    
    private final ThumbnailRepository thumbnailRepository;
    private final FileRepository fileRepository;
    private final ThumbnailGenerator thumbnailGenerator;
    
    @Override
    public void generateThumbnails(FileEntity file) {
        if (file == null) {
            return;
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

        if (contentType.startsWith("image/")) {
            generateImageThumbnail(file, Thumbnail.SizeSpec.SMALL);
            generateImageThumbnail(file, Thumbnail.SizeSpec.MEDIUM);
            generateImageThumbnail(file, Thumbnail.SizeSpec.LARGE);
        } else if (contentType.startsWith("video/")) {
            generateVideoThumbnail(file, Thumbnail.SizeSpec.MEDIUM);
        } else if ("application/pdf".equals(contentType)) {
            generatePdfThumbnails(file, 3);
        } else if (isOfficeType(contentType)) {
            generateOfficeThumbnail(file, Thumbnail.SizeSpec.MEDIUM);
        } else {
            // 对不支持的类型生成通用占位图，避免前端拿到空结果。
            createPlaceholderThumbnail(file, Thumbnail.SizeSpec.MEDIUM, Thumbnail.ThumbnailType.DOCUMENT_PREVIEW, "FILE");
        }

        log.info("为文件生成缩略图：文件 ID={}, 类型={}", file.getId(), file.getContentType());
    }
    
    @Override
    public Thumbnail generateImageThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec) {
        log.info("生成图片缩略图：文件ID={}, 尺寸规格={}", file.getId(), sizeSpec);

        return thumbnailRepository.findByFileAndSizeSpec(file, sizeSpec)
                .filter(t -> t.getStatus() == Thumbnail.GenerationStatus.SUCCESS)
                .orElseGet(() -> {
                    try {
                        Path sourcePath = Paths.get(file.getFilePath());
                        if (!Files.exists(sourcePath)) {
                            throw new BusinessException("源文件不存在，无法生成缩略图");
                        }

                        int[] dimensions = resolveDimensions(sizeSpec);
                        byte[] sourceBytes = Files.readAllBytes(sourcePath);
                        byte[] thumbnailBytes = thumbnailGenerator.generateImageThumbnail(
                                sourceBytes,
                                dimensions[0],
                                dimensions[1],
                                true
                        );

                        return saveThumbnail(file, sizeSpec, Thumbnail.ThumbnailType.IMAGE,
                                thumbnailBytes, dimensions[0], dimensions[1], "image/jpeg", null);
                    } catch (Exception ex) {
                        log.error("生成图片缩略图失败: 文件ID={}, 规格={}, 错误={}",
                                file.getId(), sizeSpec, ex.getMessage());
                        return saveFailedThumbnail(file, sizeSpec, Thumbnail.ThumbnailType.IMAGE, ex.getMessage());
                    }
                });
    }
    
    @Override
    public Thumbnail generateVideoThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec) {
        // 当前未接入视频帧提取器，先返回可用占位图，避免接口空返回。
        log.info("生成视频缩略图：文件ID={}, 尺寸规格={}", file.getId(), sizeSpec);
        return createPlaceholderThumbnail(file, sizeSpec, Thumbnail.ThumbnailType.VIDEO_FRAME, "VIDEO");
    }
    
    @Override
    public List<Thumbnail> generatePdfThumbnails(FileEntity file, int maxPages) {
        int pages = Math.max(Math.min(maxPages, 5), 1);
        log.info("生成 PDF 缩略图：文件 ID={}, 最大页数={}", file.getId(), maxPages);

        List<Thumbnail> thumbnails = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            thumbnails.add(createPlaceholderThumbnail(
                    file,
                    Thumbnail.SizeSpec.MEDIUM,
                    Thumbnail.ThumbnailType.PDF_PAGE,
                    "PDF-" + page
            ));
        }
        return thumbnails;
    }
    
    @Override
    public Thumbnail generateOfficeThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec) {
        // 当前未接入 Office 转图链路，先返回占位缩略图。
        log.info("生成 Office 文档缩略图：文件 ID={}, 尺寸规格={}", file.getId(), sizeSpec);
        return createPlaceholderThumbnail(file, sizeSpec, Thumbnail.ThumbnailType.DOCUMENT_PREVIEW, "DOC");
    }
    
    @Override
    public Resource getThumbnail(Long fileId, Thumbnail.SizeSpec sizeSpec) {
        log.info("获取缩略图：文件 ID={}, 尺寸规格={}", fileId, sizeSpec);

        FileEntity file = getFileById(fileId);
        Thumbnail.SizeSpec actualSize = sizeSpec == null ? Thumbnail.SizeSpec.MEDIUM : sizeSpec;

        Thumbnail thumbnail = thumbnailRepository.findByFileAndSizeSpec(file, actualSize)
                .orElseGet(() -> {
                    generateThumbnails(file);
                    return thumbnailRepository.findByFileAndSizeSpec(file, actualSize)
                            .orElseThrow(() -> new BusinessException("缩略图不存在"));
                });

        if (thumbnail.getThumbnailPath() == null || thumbnail.getThumbnailPath().isBlank()) {
            throw new BusinessException("缩略图路径无效");
        }

        try {
            Path path = Paths.get(thumbnail.getThumbnailPath());
            if (!Files.exists(path)) {
                throw new BusinessException("缩略图文件不存在");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("获取缩略图资源失败");
        }
    }
    
    @Override
    public List<ThumbnailInfo> getFileThumbnails(Long fileId) {
        FileEntity file = getFileById(fileId);
        return thumbnailRepository.findByFile(file).stream()
            .sorted(Comparator.comparing(Thumbnail::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(t -> new ThumbnailInfo(
                t.getId(),
                "/api/preview/" + fileId + "/image?size=" + (t.getSizeSpec() == null ? "MEDIUM" : t.getSizeSpec().name()),
                t.getSizeSpec(),
                t.getWidth(),
                t.getHeight(),
                t.getFileSize(),
                t.getContentType(),
                t.getStatus()
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public void batchGenerateThumbnails(List<FileEntity> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        int success = 0;
        for (FileEntity file : files) {
            try {
                generateThumbnails(file);
                success++;
            } catch (Exception e) {
                log.warn("批量生成缩略图单文件失败: 文件ID={}, 错误={}", file.getId(), e.getMessage());
            }
        }
        log.info("批量生成缩略图完成：总数={}, 成功={}", files.size(), success);
    }
    
    @Override
    public void regenerateFailedThumbnails() {
        List<Thumbnail> failed = thumbnailRepository.findFailedRetriableThumbnails();
        for (Thumbnail item : failed) {
            FileEntity file = item.getFile();
            if (file == null) {
                continue;
            }

            item.setStatus(Thumbnail.GenerationStatus.PROCESSING);
            item.setRetryCount((item.getRetryCount() == null ? 0 : item.getRetryCount()) + 1);
            thumbnailRepository.save(item);

            try {
                switch (item.getThumbnailType()) {
                    case IMAGE -> generateImageThumbnail(file, item.getSizeSpec());
                    case VIDEO_FRAME -> generateVideoThumbnail(file, item.getSizeSpec());
                    case PDF_PAGE -> generatePdfThumbnails(file, 1);
                    case DOCUMENT_PREVIEW -> generateOfficeThumbnail(file, item.getSizeSpec());
                    default -> createPlaceholderThumbnail(file, item.getSizeSpec(), item.getThumbnailType(), "FILE");
                }
            } catch (Exception e) {
                item.setStatus(Thumbnail.GenerationStatus.FAILED);
                item.setErrorMessage(e.getMessage());
                thumbnailRepository.save(item);
            }
        }
        log.info("重新生成失败缩略图：数量={}", failed.size());
    }
    
    @Override
    public void cleanupInvalidThumbnails() {
        List<Thumbnail> unrecoverable = thumbnailRepository.findUnrecoverableThumbnails();
        for (Thumbnail thumbnail : unrecoverable) {
            deleteThumbnailFileQuietly(thumbnail.getThumbnailPath());
        }
        if (!unrecoverable.isEmpty()) {
            thumbnailRepository.deleteAll(unrecoverable);
        }
        log.info("清理无效缩略图：删除数量={}", unrecoverable.size());
    }
    
    @Override
    public ThumbnailStatistics getThumbnailStatistics() {
        ThumbnailStatistics statistics = new ThumbnailStatistics();

        List<Object[]> statusRows = thumbnailRepository.getGenerationStatistics();
        Map<String, Long> statusMap = statusRows.stream().collect(Collectors.toMap(
            row -> row[0].toString(),
            row -> ((Number) row[1]).longValue()
        ));

        long success = statusMap.getOrDefault(Thumbnail.GenerationStatus.SUCCESS.name(), 0L);
        long failed = statusMap.getOrDefault(Thumbnail.GenerationStatus.FAILED.name(), 0L);
        long pending = statusMap.getOrDefault(Thumbnail.GenerationStatus.PENDING.name(), 0L);
        long processing = statusMap.getOrDefault(Thumbnail.GenerationStatus.PROCESSING.name(), 0L);
        long total = success + failed + pending + processing;

        statistics.setTotalThumbnails(total);
        statistics.setSuccessfulCount(success);
        statistics.setFailedCount(failed);
        statistics.setPendingCount(pending);
        statistics.setProcessingCount(processing);
        statistics.setSuccessRate(total > 0 ? (double) success / total * 100 : 0.0);

        List<ThumbnailStatistics.TypeStat> typeStats = thumbnailRepository.getTypeStatistics().stream()
            .map(row -> {
                String type = row[0].toString();
                long count = ((Number) row[1]).longValue();
                double percentage = total > 0 ? (double) count / total * 100 : 0.0;
                return new ThumbnailStatistics.TypeStat(type, count, percentage);
            })
            .collect(Collectors.toList());
        statistics.setTypeStats(typeStats);

        List<ThumbnailStatistics.SizeStat> sizeStats = thumbnailRepository.findAll().stream()
            .collect(Collectors.groupingBy(t -> t.getSizeSpec() == null ? "UNKNOWN" : t.getSizeSpec().name(), Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ThumbnailStatistics.SizeStat(
                e.getKey(),
                e.getValue(),
                total > 0 ? (double) e.getValue() / total * 100 : 0.0
            ))
            .collect(Collectors.toList());
        statistics.setSizeStats(sizeStats);

        return statistics;
    }
    
    @Override
    public void generateThumbnailsAsync(FileEntity file) {
        // 轻量同步降级：保证接口行为正确，不因异步线程配置缺失而失败。
        log.info("异步生成缩略图：文件 ID={}", file.getId());
        generateThumbnails(file);
    }
    
    public Thumbnail generateThumbnail(Long fileId, User user) {
        FileEntity file = getFileById(fileId);
        
        // 检查文件权限
        if (!file.getUploader().getId().equals(user.getId()) && !file.getIsPublic()) {
            throw new BusinessException("无权限生成此文件的缩略图");
        }
        
        try {
            generateThumbnails(file);
            Thumbnail savedThumbnail = thumbnailRepository.findByFileAndSizeSpec(file, Thumbnail.SizeSpec.MEDIUM)
                    .orElseThrow(() -> new BusinessException("生成缩略图失败"));
            log.info("生成缩略图: 文件ID={}, 用户={}", fileId, user.getUsername());
            
            return savedThumbnail;
            
        } catch (Exception e) {
            log.error("生成缩略图失败: 文件ID={}, 错误={}", fileId, e.getMessage());
            throw new BusinessException("生成缩略图失败: " + e.getMessage());
        }
    }
    

    
    @Transactional(readOnly = true)
    public Resource getThumbnailResource(Long fileId) {
        try {
            FileEntity file = getFileById(fileId);
            Thumbnail thumbnail = thumbnailRepository.findByFileAndSizeSpec(file, Thumbnail.SizeSpec.MEDIUM)
                    .orElseThrow(() -> new BusinessException("缩略图不存在"));
            Path path = Paths.get(thumbnail.getThumbnailPath());
            if (!Files.exists(path)) {
                throw new BusinessException("缩略图文件不存在");
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("获取缩略图资源失败");
        }
    }
    
    public void deleteThumbnail(Long fileId, User user) {
        FileEntity file = getFileById(fileId);
        boolean isOwner = file.getUploader() != null && Objects.equals(file.getUploader().getId(), user.getId());
        boolean isAdmin = user.getRole() == User.UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new BusinessException("无权限删除缩略图");
        }

        List<Thumbnail> thumbnails = thumbnailRepository.findByFile(file);
        for (Thumbnail thumbnail : thumbnails) {
            deleteThumbnailFileQuietly(thumbnail.getThumbnailPath());
        }
        if (!thumbnails.isEmpty()) {
            thumbnailRepository.deleteAll(thumbnails);
        }

        log.info("删除缩略图：文件 ID={}, 用户={}", fileId, user.getUsername());
    }
    
    @Transactional(readOnly = true)
    public boolean hasThumbnail(Long fileId) {
        FileEntity file = getFileById(fileId);
        return thumbnailRepository.findByFile(file).stream()
                .anyMatch(t -> t.getStatus() == Thumbnail.GenerationStatus.SUCCESS);
    }
    
    // ==================== 私有方法 ====================
    
    private FileEntity getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }

    private boolean isOfficeType(String contentType) {
        return contentType.contains("msword")
                || contentType.contains("officedocument")
                || contentType.contains("ms-excel")
                || contentType.contains("ms-powerpoint");
    }

    private int[] resolveDimensions(Thumbnail.SizeSpec sizeSpec) {
        Thumbnail.SizeSpec spec = sizeSpec == null ? Thumbnail.SizeSpec.MEDIUM : sizeSpec;
        return switch (spec) {
            case SMALL -> new int[]{128, 128};
            case LARGE -> new int[]{512, 512};
            case CUSTOM, MEDIUM -> new int[]{256, 256};
        };
    }

    private Thumbnail createPlaceholderThumbnail(
            FileEntity file,
            Thumbnail.SizeSpec sizeSpec,
            Thumbnail.ThumbnailType type,
            String label
    ) {
        try {
            int[] dimensions = resolveDimensions(sizeSpec);
            byte[] imageBytes = createPlaceholderImage(dimensions[0], dimensions[1], label);
            return saveThumbnail(file, sizeSpec, type, imageBytes, dimensions[0], dimensions[1], "image/jpeg", null);
        } catch (Exception e) {
            return saveFailedThumbnail(file, sizeSpec, type, e.getMessage());
        }
    }

    private byte[] createPlaceholderImage(int width, int height, String label) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(238, 240, 244));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(95, 103, 117));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, width / 10)));
        FontMetrics metrics = g.getFontMetrics();
        int x = Math.max((width - metrics.stringWidth(label)) / 2, 8);
        int y = Math.max((height + metrics.getAscent()) / 2, 16);
        g.drawString(label, x, y);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private Thumbnail saveThumbnail(
            FileEntity file,
            Thumbnail.SizeSpec sizeSpec,
            Thumbnail.ThumbnailType type,
            byte[] bytes,
            Integer width,
            Integer height,
            String contentType,
            String errorMessage
    ) {
        Thumbnail thumbnail = thumbnailRepository.findByFileAndSizeSpec(file, sizeSpec).orElseGet(Thumbnail::new);
        thumbnail.setFile(file);
        thumbnail.setSizeSpec(sizeSpec);
        thumbnail.setThumbnailType(type);
        thumbnail.setWidth(width);
        thumbnail.setHeight(height);
        thumbnail.setContentType(contentType);
        thumbnail.setFileSize((long) bytes.length);
        thumbnail.setStatus(Thumbnail.GenerationStatus.SUCCESS);
        thumbnail.setErrorMessage(errorMessage);
        thumbnail.setGeneratedAt(LocalDateTime.now());

        try {
            Path root = Paths.get(thumbnailGenerator.getThumbnailPath());
            Files.createDirectories(root);

            String fileName = String.format(
                    "%d_%s_%s_%s.jpg",
                    file.getId(),
                    sizeSpec.name().toLowerCase(Locale.ROOT),
                    type.name().toLowerCase(Locale.ROOT),
                    UUID.randomUUID().toString().substring(0, 8)
            );
            Path target = root.resolve(fileName);
            Files.write(target, bytes);

            thumbnail.setThumbnailName(fileName);
            thumbnail.setThumbnailPath(target.toString());
            return thumbnailRepository.save(thumbnail);
        } catch (IOException e) {
            throw new BusinessException("保存缩略图失败: " + e.getMessage());
        }
    }

    private Thumbnail saveFailedThumbnail(
            FileEntity file,
            Thumbnail.SizeSpec sizeSpec,
            Thumbnail.ThumbnailType type,
            String reason
    ) {
        Thumbnail thumbnail = thumbnailRepository.findByFileAndSizeSpec(file, sizeSpec).orElseGet(Thumbnail::new);
        thumbnail.setFile(file);
        thumbnail.setSizeSpec(sizeSpec);
        thumbnail.setThumbnailType(type);
        thumbnail.setStatus(Thumbnail.GenerationStatus.FAILED);
        thumbnail.setErrorMessage(reason);
        thumbnail.setGeneratedAt(LocalDateTime.now());
        return thumbnailRepository.save(thumbnail);
    }

    private void deleteThumbnailFileQuietly(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception e) {
            log.warn("删除缩略图文件失败: path={}, error={}", path, e.getMessage());
        }
    }
}