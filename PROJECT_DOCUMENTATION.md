项目说明文档（汇总）

概述
- 项目：File Sharing System（文件分享系统）
- 技术栈：Java 11、Spring Boot 2.7.x、Spring Data JPA、Spring Security（JWT）、Lombok、H2/MySQL（可切换）、前端：Vite + Vue3（位于 frontend/）

本次整理目的
1. 将项目当前状态、重要变更与运行说明整理为单一文档，便于交付与演示。
2. 清理仓库中不再必要的启动脚本与旧文档，减少干扰。

已完成的主要后端改进（摘要）
- DataBackupService：
  - 外部化备份相关配置（backup.base-path、backup.max-size、compression-level）。
  - 使用流式 I/O 打包文件（ZipOutputStream + InputStream），避免 OOM。
  - 路径规范化与目录穿越防护（resolve + normalize + startsWith）。
  - 备份任务持久化（BackupTaskEntity + BackupTaskRepository）。
  - SHA-256 校验和计算并写入 metadata.json，恢复时进行校验。
  - 实现恢复流程：文件解压到 storage/restored/ 时间戳目录，并恢复数据库元数据（用户、文件记录，事务性）。
- Chunked Upload：
  - 新增分片上传控制器（/api/upload/*）：init、upload chunk、complete（合并）。
  - 合并时写入 FileEntity，支持大文件上传。
  - 添加上传校验：单分片大小、总大小、MIME 白名单、合并后返回 SHA-256。
  - 强制使用 JWT 身份作为上传者（移除可由客户端传入的 uploaderId）。
- 安全与可运维性：
  - 增加 JwtAuthenticationFilter 以解析 Authorization: Bearer <token> 并设置 Authentication。
  - 在 SecurityConfig 启用方法级安全（@EnableGlobalMethodSecurity），并注册 JWT 过滤器。
  - 启用调度（@EnableScheduling）并新增临时分片清理任务 UploadCleanupTask（每小时运行，清理超过配置时长的临时目录）。
- 配置：在 application.yml 中新增上传/校验与清理相关的示例配置项。

运行说明（开发环境）
1. 构建并启动：
   - 使用 Maven： mvn spring-boot:run
2. 访问：
   - 默认端口： http://localhost:8080
   - H2 控制台： http://localhost:8080/h2-console （开发时可用）
3. 授权：
   - 本项目使用 JWT，HTTP 请求应在 Authorization 头中携带 Bearer <token>。
   - 注意：当前 JwtUtil 在构造器中生成随机密钥，重启后 token 无法继续使用。建议在 application.yml 中配置固定 secret 并将 JwtUtil 改为使用该 secret（见注意项）。

关键配置（示例）
- file.upload.path：文件存储根目录（默认 ./uploads/）
- file.upload.temp-path：分片临时目录（默认 ./temp/）
- upload.chunk.max-size：单分片最大字节数（默认 5242880 = 5MB）
- upload.file.max-size：单文件最大字节数（默认 10737418240 = 10GB）
- upload.allowed-types-csv：允许的 MIME 类型（逗号分隔）
- upload.cleanup.expire-hours：临时分片过期小时数（默认 24）

已归档/移除的文件（内容已替换为存档说明）
- 项目根下若干旧启动脚本与文档，示例：
  - *.bat / *.ps1: manual-start.bat, simple-start.bat, startup.bat, system-test.bat, diagnostic.bat, enhanced-demo.bat, api-test.ps1, simple-test.ps1, system-check.bat, system-status.sh
  - 旧文档: PROJECT_COMPLETION_SUMMARY.md, SERVICE_LAYER_ENHANCEMENT_REPORT.md, VUE3_FRONTEND_COMPLETION_REPORT.md, SYSTEM_COMPLETION_REPORT.md, SYSTEM_STARTUP_GUIDE.md, TROUBLESHOOTING_CHECKLIST.txt, USAGE_GUIDE.txt, file-sharing-system.iml
说明：这些文件内容已被替换为“已归档”的简要说明，避免误用。历史内容可在版本库历史中恢复（git log / git checkout）

待办（建议优先级）
1. 将 JwtUtil 改为使用 application.yml 中的 jwt.secret（高优先级）以保证 token 在重启后仍可验证。
2. 为备份表 BackupTask 添加 Flyway/Liquibase 迁移脚本（中高优先级）。
3. 为关键流程编写单元与集成测试（中优先级）。
4. 可选：将文件存储迁移到对象存储（S3 / MinIO）以支持更大规模存储与备份去重。

如果需要，我可以：
- 将 JwtUtil 修改为使用配置的 secret；
- 将原始被归档的文档合并到仓库的 archive/ 目录（保留历史），而非完全删除；
- 生成前端分片上传示例（HTML + JS）。

整理者：GitHub Copilot
日期：2026-03-13
