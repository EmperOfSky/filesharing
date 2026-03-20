# 文件共享系统 (File Sharing System)

一个功能完整的企业级文件共享平台，基于Spring Boot 2.7.5开发，提供文件管理、协作办公、AI智能分析等核心功能。

## 🌟 系统特性

### 🔧 核心功能
- **文件管理**: 上传、下载、预览、分享、版本控制
- **文件夹管理**: 层级目录结构、权限控制
- **用户系统**: 注册登录、权限管理、个人中心
- **协作办公**: 项目管理、文档协作、实时评论
- **AI智能**: 内容分类、图像识别、智能推荐
- **移动端优化**: 专为移动设备设计的API接口

### 🛡️ 安全特性
- JWT Token认证
- 细粒度权限控制
- 数据加密存储
- 操作日志记录
- XSS/CSRF防护

### 📊 系统监控
- 实时性能监控
- 用户行为分析
- 文件使用统计
- 系统健康检查

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- 内存 2GB+

### 启动步骤

1. **克隆项目**
```bash
git clone <项目地址>
cd filesharing
```

2. **编译项目**
```bash
mvn clean compile
```

3. **启动应用**
```bash
mvn spring-boot:run
```

4. **访问系统**
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html
- H2控制台: http://localhost:8080/h2-console

### 测试API
```powershell
# Windows PowerShell
.\api-test.ps1
```

## 📁 项目结构

```
src/main/java/com/filesharing/
├── controller/          # 控制层
 │   ├── AuthController.java         # 认证接口
│   ├── FileController.java         # 文件管理接口
│   ├── mobile/                     # 移动端接口
│   └── AIController.java           # AI功能接口
├── service/             # 服务层
│   ├── impl/                       # 服务实现
│   ├── FileService.java            # 文件服务接口
│   └── AIService.java              # AI服务接口
├── repository/          # 数据访问层
│   ├── FileRepository.java         # 文件仓储
│   └── AIModelRepository.java      # AI模型仓储
├── entity/              # 实体层
│   ├── User.java                   # 用户实体
│   ├── FileEntity.java             # 文件实体
│   └── AIModel.java                # AI模型实体
├── dto/                 # 数据传输对象
│   ├── request/                    # 请求DTO
│   └── response/                   # 响应DTO
└── config/              # 配置类
    ├── SecurityConfig.java         # 安全配置
    └── DatabaseConfig.java         # 数据库配置
```

## 🛠️ 核心模块

### 1. 文件管理模块
```java
// 文件上传
POST /api/files/upload

// 文件下载
GET /api/files/{fileId}/download

// 文件预览
GET /api/files/{fileId}/preview

// 文件分享
POST /api/files/{fileId}/share
```

### 2. 协作办公模块
```java
// 创建协作项目
POST /api/collaboration/projects

// 添加项目成员
POST /api/collaboration/projects/{projectId}/members

// 创建协作文档
POST /api/collaboration/documents

// 添加评论
POST /api/collaboration/documents/{documentId}/comments
```

### 3. AI智能模块
```java
// 文本内容分类
POST /api/ai/classify-text

// 文件内容分析
POST /api/ai/analyze-file

// 图像内容识别
POST /api/ai/recognize-image

// 智能标签推荐
POST /api/ai/recommend-tags
```

### 4. 移动端API
```java
// 获取最近文件
GET /api/mobile/files/recent

// 获取收藏文件
GET /api/mobile/files/favorites

// 分片上传
POST /api/mobile/upload/chunk

// 文件夹树结构
GET /api/mobile/folders
```

## 🔧 配置说明

### application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

jwt:
  secret: your-secret-key
  expiration: 86400000

file:
  upload:
    path: ./uploads/
    max-size: 100MB
```

## 📊 API接口文档

### 认证接口
| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/me` | GET | 获取当前用户信息 |

### 文件接口
| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/files/upload` | POST | 上传文件 |
| `/api/files/{id}` | GET | 获取文件信息 |
| `/api/files/{id}/download` | GET | 下载文件 |
| `/api/files/{id}/preview` | GET | 预览文件 |

### AI接口
| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/ai/classify-text` | POST | 文本分类 |
| `/api/ai/analyze-file` | POST | 文件分析 |
| `/api/ai/smart-search` | GET | 智能搜索 |
| `/api/ai/models` | GET | AI模型列表 |

## 🔒 安全说明

### 认证机制
- 使用JWT Token进行身份认证
- Token有效期24小时
- 支持Token刷新机制

### 权限控制
- 基于角色的访问控制(RBAC)
- 文件级别的权限管理
- 操作日志审计

### 数据保护
- 敏感信息加密存储
- SQL注入防护
- XSS攻击防护

## 📈 性能优化

### 缓存策略
- Redis缓存热点数据
- 本地缓存常用配置
- CDN加速静态资源

### 数据库优化
- 索引优化
- 查询优化
- 连接池配置

### 文件存储
- 分片上传大文件
- 支持云存储集成
- 自动清理临时文件

## 🐛 常见问题

### 启动问题
**Q: 端口被占用怎么办？**
A: 修改application.yml中的server.port配置

**Q: 数据库连接失败？**
A: 检查H2数据库配置，确保驱动版本兼容

### 功能问题
**Q: 文件上传失败？**
A: 检查文件大小限制和存储路径权限

**Q: AI功能不可用？**
A: 确保AI模型配置正确，网络连接正常

## 📞 技术支持

### 开发环境
- IDE: IntelliJ IDEA / Eclipse
- 构建工具: Maven
- 版本控制: Git

### 依赖管理
```xml
<!-- 主要依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## 📄 许可证

本项目采用MIT许可证，详情请查看LICENSE文件。

## 🙏 致谢

感谢以下开源项目的支持：
- Spring Boot
- Spring Security
- Hibernate
- H2 Database
- Lombok
- Apache Tika

---

**文件共享系统** - 让文件协作变得更简单！ 🚀

## 🔗 前端集成与示例代码

本项目后端采用 JWT 鉴权，所有需要用户身份的请求必须在 HTTP Header 中携带：

- Authorization: Bearer <token>

下列为前端需要关注的后端接口（与示例）：

1) 分片上传（Chunked Upload）
- 初始化分片上传：POST /api/upload/chunk/init  (参数: filename) -> 返回 { uploadId }
- 上传分片：POST /api/upload/chunk (参数: uploadId, chunkIndex, chunk MultipartFile)
- 完成合并：POST /api/upload/chunk/complete (参数: uploadId, filename, 可选 contentType)

示例 JavaScript（浏览器端）:

```javascript
// 简化示例：按顺序上传分片（不处理断点续传/重试）
async function uploadFileInChunks(file, token) {
  const filename = file.name;
  // init
  const initResp = await fetch('/api/upload/chunk/init', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ filename })
  });
  const initJson = await initResp.json();
  const uploadId = initJson.uploadId;

  const chunkSize = 5 * 1024 * 1024; // 前端可与后端配置对齐
  let chunkIndex = 0;
  for (let start = 0; start < file.size; start += chunkSize) {
    const end = Math.min(start + chunkSize, file.size);
    const part = file.slice(start, end);
    const form = new FormData();
    form.append('uploadId', uploadId);
    form.append('chunkIndex', chunkIndex);
    form.append('chunk', part, filename);

    await fetch('/api/upload/chunk', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token },
      body: form
    });

    chunkIndex++;
  }

  // complete
  const completeForm = new FormData();
  completeForm.append('uploadId', uploadId);
  completeForm.append('filename', filename);
  completeForm.append('contentType', file.type || 'application/octet-stream');

  const completeResp = await fetch('/api/upload/chunk/complete', {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + token },
    body: completeForm
  });

  return await completeResp.json();
}
```

2) 常用前端接口使用约定
- 需在每次请求中带上 Authorization: Bearer <token>。
- 上传时请先确认后端配置：
  - upload.chunk.max-size（单分片上限）
  - upload.file.max-size（单文件上限）
  - upload.allowed-types-csv（允许的 MIME 类型，逗号分隔）

3) 推荐实践
- 在上传时做分片重试与并发控制（例如最多并发 3-4 个分片）。
- 在合并完成后，前端可比较后端返回的 sha256 值进行完整性校验。
- 定期清理挂起的临时分片目录由后端的 UploadCleanupTask 负责（默认 24 小时）。

## ✅ 运行与验证（新增）

下面步骤可以帮助你在开发环境快速启动并验证系统的关键功能（包括分片上传与断点续传）。

1) 环境准备
- Java 17 或更高，Maven 3.6+。
- Node.js 16+（若前端需要本地开发）。
- 在项目根目录确保 `uploads/` 与 `temp/` 目录存在或可被应用创建，并赋予读写权限。
- 在 `src/main/resources/application.yml` 中配置一个稳定的 `jwt.secret`，例如：

```yaml
jwt:
  secret: change-this-to-a-strong-secret
  expiration: 86400000
```

2) 后端启动
- 在项目根：

```powershell
mvn clean compile
mvn spring-boot:run
```

- 后端默认运行在 http://localhost:8080

3) 前端启动（可选，本地开发）
- 进入 `frontend` 目录，安装依赖并启动：

```powershell
cd frontend
npm install
npm run dev
```

- 若前端通过 Vite 在其他端口运行，请确保已配置代理或后端允许 CORS。

4) 快速验证：认证与分片上传
- 使用 Postman / curl 或前端页面执行登录，获取 JWT token。
  - 登录接口：POST /api/auth/login，返回 token（确保 `jwt.secret` 一致）。
- 使用 token 调用初始化接口并检查已存在分片（支持断点续传）：

```powershell
# 假设 TOKEN 环境变量已设置
curl -X POST "http://localhost:8080/api/upload/chunk/init" -H "Authorization: Bearer $TOKEN" -d "filename=test.bin"
```

返回示例：

```json
{
  "uploadId": "...",
  "filename": "test.bin",
  "existingParts": [0,1,2]
}
```

- 按照返回的 uploadId 上传分片：

```powershell
# 上传第0片
curl -X POST "http://localhost:8080/api/upload/chunk" -H "Authorization: Bearer $TOKEN" -F "uploadId=<uploadId>" -F "chunkIndex=0" -F "chunk=@part0.bin"
```

- 上传所有需要的分片后调用合并接口：

```powershell
curl -X POST "http://localhost:8080/api/upload/chunk/complete" -H "Authorization: Bearer $TOKEN" -F "uploadId=<uploadId>" -F "filename=test.bin" -F "contentType=application/octet-stream"
```

返回示例包含 `fileId` 与可选的 `sha256` 校验值。

5) 常见问题与排查
- 无法认证/401：确认 `jwt.secret` 是否在 `application.yml` 中配置且服务已重启；前端需在每次请求头中带上 `Authorization: Bearer <token>`。
- 前端不带 token：项目中部分位置可能使用 `localStorage['token']` 或 `localStorage['auth-storage']` 存储 token，我已将关键上传路径使用 `token` 作为主键。建议全仓统一为 `token`。
- 临时分片未清理：UploadCleanupTask 默认每小时运行并删除超过配置期限的临时目录。你可以在 `application.yml` 中配置 `upload.cleanup.expire-hours`。
- 数据丢失：开发配置使用 H2 的 create-drop，生产环境请配置持久化数据库并使用迁移脚本（Flyway/Liquibase）。

---

## 全量 API 接口清单（自动扫描）

> 扫描来源：`src/main/java/**` 控制器与上传控制器注解。  
> 联调方式：后端直连 `http://localhost:8080` 与前端代理 `http://localhost:3001` 双通道冒烟。

- 本次扫描接口总数：104
- 双通道可达接口：104/104

| Method | Path | Controller | BackendStatus | ProxyStatus |
|---|---|---|---:|---:|
| DELETE | /api/backup/{backupPath} | BackupController | 403 | 403 |
| DELETE | /api/backup/cleanup | BackupController | 403 | 403 |
| DELETE | /api/cloud-storage/configs/{configId} | CloudStorageController | 403 | 403 |
| DELETE | /api/files/{fileId} | FileController | 403 | 403 |
| DELETE | /api/mobile/files/{fileId}/favorite | MobileFileController | 403 | 403 |
| DELETE | /api/monitoring/cleanup | MonitoringController | 403 | 403 |
| DELETE | /api/recommendations/cleanup | RecommendationController | 403 | 403 |
| GET | /api/ai/analyze-behavior | AIController | 403 | 403 |
| GET | /api/ai/models | AIController | 403 | 403 |
| GET | /api/ai/smart-search | AIController | 403 | 403 |
| GET | /api/auth/me | AuthController | 401 | 401 |
| GET | /api/backup/config/export | BackupController | 403 | 403 |
| GET | /api/backup/list | BackupController | 403 | 403 |
| GET | /api/backup/statistics | BackupController | 403 | 403 |
| GET | /api/backup/task/{taskId} | BackupController | 403 | 403 |
| GET | /api/cloud-storage/configs | CloudStorageController | 403 | 403 |
| GET | /api/cloud-storage/configs/{configId} | CloudStorageController | 403 | 403 |
| GET | /api/cloud-storage/configs/{configId}/usage | CloudStorageController | 403 | 403 |
| GET | /api/cloud-storage/configs/default | CloudStorageController | 403 | 403 |
| GET | /api/cloud-storage/configs/enabled | CloudStorageController | 403 | 403 |
| GET | /api/cloud-storage/configs/info | CloudStorageController | 403 | 403 |
| GET | /api/demo/health | DemoController | 403 | 403 |
| GET | /api/demo/info | DemoController | 403 | 403 |
| GET | /api/files/{fileId} | FileController | 403 | 403 |
| GET | /api/files/{fileId}/download | FileController | 403 | 403 |
| GET | /api/files/my | FileController | 403 | 403 |
| GET | /api/files/public | FileController | 200 | 200 |
| GET | /api/mobile/enhanced/activity/recent | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/announcements | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/config | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/files/{fileId}/preview-info | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/files/{fileId}/thumbnail-info | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/storage/usage | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/enhanced/version/check | MobileEnhancedController | 403 | 403 |
| GET | /api/mobile/files/favorites | MobileFileController | 403 | 403 |
| GET | /api/mobile/files/offline-available | MobileFileController | 403 | 403 |
| GET | /api/mobile/files/recent | MobileFileController | 403 | 403 |
| GET | /api/mobile/files/search | MobileFileController | 403 | 403 |
| GET | /api/mobile/folders | MobileFolderController | 403 | 403 |
| GET | /api/mobile/folders/{folderId}/breadcrumb | MobileFolderController | 403 | 403 |
| GET | /api/mobile/folders/{folderId}/subfolders | MobileFolderController | 403 | 403 |
| GET | /api/mobile/folders/quick-access | MobileFolderController | 403 | 403 |
| GET | /api/mobile/upload/progress/{uploadId} | MobileUploadController | 403 | 403 |
| GET | /api/monitoring/alerts | MonitoringController | 403 | 403 |
| GET | /api/monitoring/health | MonitoringController | 403 | 403 |
| GET | /api/monitoring/metrics | MonitoringController | 403 | 403 |
| GET | /api/monitoring/metrics/history | MonitoringController | 403 | 403 |
| GET | /api/monitoring/report | MonitoringController | 403 | 403 |
| GET | /api/monitoring/statistics | MonitoringController | 403 | 403 |
| GET | /api/preview/{fileId} | PreviewController | 403 | 403 |
| GET | /api/preview/{fileId}/content | PreviewController | 403 | 403 |
| GET | /api/preview/{fileId}/image | PreviewController | 403 | 403 |
| GET | /api/preview/{fileId}/pdf | PreviewController | 403 | 403 |
| GET | /api/preview/{fileId}/statistics | PreviewController | 403 | 403 |
| GET | /api/preview/{fileId}/text | PreviewController | 403 | 403 |
| GET | /api/preview/popular | PreviewController | 403 | 403 |
| GET | /api/preview/user/statistics | PreviewController | 403 | 403 |
| GET | /api/recommendations | RecommendationController | 403 | 403 |
| GET | /api/recommendations/analytics | RecommendationController | 403 | 403 |
| GET | /api/recommendations/similar/{itemId} | RecommendationController | 403 | 403 |
| GET | /api/users/profile | UserController | 403 | 403 |
| POST | /api/ai/analyze-file | AIController | 403 | 403 |
| POST | /api/ai/classify-text | AIController | 403 | 403 |
| POST | /api/ai/document-summary | AIController | 403 | 403 |
| POST | /api/ai/keywords | AIController | 403 | 403 |
| POST | /api/ai/question-answer | AIController | 403 | 403 |
| POST | /api/ai/recognize-image | AIController | 403 | 403 |
| POST | /api/ai/recommend-tags | AIController | 403 | 403 |
| POST | /api/ai/sentiment | AIController | 403 | 403 |
| POST | /api/ai/similarity | AIController | 403 | 403 |
| POST | /api/ai/summarize | AIController | 403 | 403 |
| POST | /api/ai/test-model/{modelId} | AIController | 403 | 403 |
| POST | /api/ai/text-correction | AIController | 403 | 403 |
| POST | /api/auth/debug-login | AuthController | 400 | 400 |
| POST | /api/auth/login | AuthController | 400 | 400 |
| POST | /api/auth/register | AuthController | 400 | 400 |
| POST | /api/backup/async | BackupController | 403 | 403 |
| POST | /api/backup/config/import | BackupController | 403 | 403 |
| POST | /api/backup/full | BackupController | 403 | 403 |
| POST | /api/backup/incremental | BackupController | 403 | 403 |
| POST | /api/backup/restore | BackupController | 403 | 403 |
| POST | /api/backup/validate | BackupController | 403 | 403 |
| POST | /api/cloud-storage/configs | CloudStorageController | 403 | 403 |
| POST | /api/cloud-storage/configs/{configId}/test-connection | CloudStorageController | 403 | 403 |
| POST | /api/demo/register | DemoController | 403 | 403 |
| POST | /api/files/upload | FileController | 403 | 403 |
| POST | /api/mobile/enhanced/feedback | MobileEnhancedController | 403 | 403 |
| POST | /api/mobile/enhanced/files/batch-operate | MobileEnhancedController | 403 | 403 |
| POST | /api/mobile/enhanced/sync | MobileEnhancedController | 403 | 403 |
| POST | /api/mobile/files/{fileId}/favorite | MobileFileController | 403 | 403 |
| POST | /api/mobile/upload | MobileUploadController | 403 | 403 |
| POST | /api/mobile/upload/chunk | MobileUploadController | 403 | 403 |
| POST | /api/mobile/upload/init-chunk | MobileUploadController | 403 | 403 |
| POST | /api/monitoring/alerts/test | MonitoringController | 403 | 403 |
| POST | /api/recommendations/generate | RecommendationController | 403 | 403 |
| POST | /api/upload/chunk | ChunkUploadController | 403 | 403 |
| POST | /api/upload/chunk/complete | ChunkUploadController | 403 | 403 |
| POST | /api/upload/chunk/init | ChunkUploadController | 403 | 403 |
| POST | /api/users/change-password | UserController | 403 | 403 |
| PUT | /api/cloud-storage/configs/{configId} | CloudStorageController | 403 | 403 |
| PUT | /api/monitoring/alerts/{alertId}/close | MonitoringController | 403 | 403 |
| PUT | /api/recommendations/{id}/adopt | RecommendationController | 403 | 403 |
| PUT | /api/recommendations/{id}/view | RecommendationController | 403 | 403 |
| PUT | /api/users/profile | UserController | 403 | 403 |