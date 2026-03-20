package com.filesharing.upload;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.util.FileStorageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class ChunkUploadController {

    @Value("${file.upload.temp-path:./temp/}")
    private String tempBasePath;

    // 上传校验配置
    @Value("${upload.chunk.max-size:5242880}") // 默认 5MB
    private long chunkMaxSize;

    @Value("${upload.file.max-size:10737418240}") // 默认 10GB
    private long fileMaxSize;

    @Value("${upload.allowed-types:}")
    private String allowedTypesCsv;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;

    /**
     * 初始化分片上传，返回 uploadId 和已存在分片索引（支持断点续传）
     */
    @PostMapping("/chunk/init")
    public ResponseEntity<Map<String, Object>> initUpload(
            @RequestParam("filename") String filename,
            @RequestParam(value = "uploadId", required = false) String uploadId
    ) throws IOException {
        // 鉴权：仅允许已认证用户发起
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证");
        }

        if (uploadId == null || uploadId.isBlank()) {
            uploadId = UUID.randomUUID().toString();
        }
        Path uploadDir = Paths.get(tempBasePath).toAbsolutePath().normalize().resolve(uploadId);
        Files.createDirectories(uploadDir);

        // 列出已有的分片索引，便于客户端断点续传
        Set<Integer> existing = new TreeSet<>();
        try (Stream<Path> s = Files.list(uploadDir)) {
            s.filter(p -> p.getFileName().toString().startsWith("part_") && p.getFileName().toString().endsWith(".part"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        // part_00001.part -> 提取中间数字
                        int start = "part_".length();
                        int end = name.length() - ".part".length();
                        if (end > start) {
                            String idxStr = name.substring(start, end);
                            try {
                                existing.add(Integer.parseInt(idxStr));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    });
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("uploadId", uploadId);
        resp.put("filename", filename);
        resp.put("existingParts", existing.stream().sorted().collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunk") MultipartFile chunk
    ) throws IOException {
        // 鉴权
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证");
        }

        if (chunkIndex == null || chunkIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的 chunkIndex");
        }

        if (chunk == null || chunk.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "空的分片");
        }

        if (chunk.getSize() > chunkMaxSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "分片大小超出限制: " + chunkMaxSize);
        }

        Path uploadDir = Paths.get(tempBasePath).toAbsolutePath().normalize().resolve(uploadId);
        if (!Files.exists(uploadDir)) {
            return ResponseEntity.badRequest().body(Map.of("error", "uploadId not found"));
        }

        Path partFile = uploadDir.resolve(String.format("part_%05d.part", chunkIndex));
        try (InputStream in = chunk.getInputStream(); OutputStream out = Files.newOutputStream(partFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("uploadId", uploadId);
        resp.put("chunkIndex", chunkIndex);
        resp.put("stored", true);
        return ResponseEntity.ok(resp);
    }

    /**
     * 完成分片上传并合并，同时可选保存文件元数据（传入 uploaderId）
     */
    @PostMapping("/chunk/complete")
    public ResponseEntity<Map<String, String>> completeUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("filename") String filename,
            @RequestParam(value = "contentType", required = false) String contentType
    ) throws IOException {
        // 鉴权并解析当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证");
        }

        String principalName = auth.getName();
        Optional<User> uploaderOpt = userRepository.findByIdentifier(principalName);
        if (!uploaderOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "找不到当前用户");
        }

        Path uploadDir = Paths.get(tempBasePath).toAbsolutePath().normalize().resolve(uploadId);
        if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
            return ResponseEntity.badRequest().body(Map.of("error", "uploadId not found"));
        }

        List<Path> parts;
        try (Stream<Path> stream = Files.list(uploadDir)) {
            parts = stream
                    .filter(p -> p.getFileName().toString().startsWith("part_"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (parts.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "no chunks uploaded"));
        }

        String storageName = UUID.randomUUID().toString();
        Path mergedTempFile = Paths.get(tempBasePath).toAbsolutePath().normalize()
                .resolve(uploadId + "_merged_" + UUID.randomUUID());

        try (OutputStream out = Files.newOutputStream(mergedTempFile)) {
            // 合并所有 part_*.part 文件按名称排序
            for (Path part : parts) {
                try (InputStream in = Files.newInputStream(part)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }

        // 合并后校验总大小
        long fileSize = Files.size(mergedTempFile);
        if (fileSize > fileMaxSize) {
            try { Files.deleteIfExists(mergedTempFile); } catch (IOException ignored) {}
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "文件大小超出限制: " + fileMaxSize);
        }

        // 校验 MIME 类型（如果前端传入）
        if (contentType != null && allowedTypesCsv != null && !allowedTypesCsv.isBlank()) {
            Set<String> allowed = Arrays.stream(allowedTypesCsv.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            if (!allowed.isEmpty() && !allowed.contains(contentType)) {
                try { Files.deleteIfExists(mergedTempFile); } catch (IOException ignored) {}
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不允许的文件类型: " + contentType);
            }
        }

        // 计算 SHA-256 返回给客户端，便于校验
        String sha256 = null;
        try {
            sha256 = computeSHA256(mergedTempFile);
        } catch (IOException e) {
            log.warn("计算文件 sha256 失败", e);
        }

        try (InputStream mergedInput = Files.newInputStream(mergedTempFile)) {
            fileStorageUtil.saveInputStreamAtPath(mergedInput, fileSize, storageName, contentType);
        } finally {
            try { Files.deleteIfExists(mergedTempFile); } catch (IOException ignored) {}
        }

        // 保存文件元数据，上传者为当前认证用户

        User uploader = uploaderOpt.get();

        FileEntity fe = new FileEntity();
        fe.setStorageName(storageName);
        fe.setOriginalName(filename);
        // 存储的 filePath 采用以 / 开头的相对路径，便于现有代码处理
        fe.setFilePath("/uploads/" + storageName);
        fe.setFileSize(fileSize);
        fe.setContentType(contentType);
        // 取扩展名
        String ext = null;
        int idx = filename.lastIndexOf('.');
        if (idx >= 0 && idx < filename.length() - 1) ext = filename.substring(idx + 1);
        fe.setExtension(ext);
        fe.setStatus(FileEntity.FileStatus.AVAILABLE);
        fe.setIsPublic(false);
        fe.setDownloadCount(0);
        fe.setPreviewCount(0);
        fe.setShareCount(0);
        fe.setUploader(uploader);

        fileRepository.save(fe);

        // 清理临时目录
        Files.list(uploadDir).forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
        });
        try { Files.deleteIfExists(uploadDir); } catch (IOException ignored) {}

        Map<String, String> resp = new HashMap<>();
        resp.put("storageName", storageName);
        resp.put("filePath", "/uploads/" + storageName);
        resp.put("originalFilename", filename);
        resp.put("fileId", fe.getId() != null ? fe.getId().toString() : "");
        if (sha256 != null) resp.put("sha256", sha256);
        return ResponseEntity.ok(resp);
    }

    private String computeSHA256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
