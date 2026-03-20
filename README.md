# File Sharing System

一个基于 `Spring Boot 2.7 + Vue 3 + TypeScript` 的文件共享与协作平台，包含文件管理、短链分享、快传中心、回收站、备份恢复、实时协作和对象存储接入。

## 功能概览

- 用户认证：注册、登录、JWT 鉴权、个人资料管理
- 文件管理：上传、下载、预览、重命名、移动、复制、收藏
- 分片上传：断点续传、分片合并、上传校验
- 分享能力：短链分享、提取码、一次性下载令牌、公开访问页
- 快传中心：文本快传、文件快传、预签名上传、取件空间
- 协作模块：项目、成员、文档、评论、WebSocket 实时协作
- 运维能力：备份/恢复、监控指标、健康检查、告警报告
- 存储抽象：支持 `local` / `minio`

## 技术栈

### 后端

- Java 17
- Spring Boot 2.7.5
- Spring Security + JWT
- Spring Data JPA
- MySQL 8
- WebSocket
- Actuator + Micrometer
- MinIO Java SDK

### 前端

- Vue 3
- TypeScript
- Vite 5
- Vue Router 4
- Pinia
- Element Plus

## 项目结构

```text
.
├─ src/main/java/com/filesharing/   # 后端主代码
├─ src/main/resources/              # application.yml、证书等配置
├─ src/test/                        # 后端测试
├─ frontend/                        # Vue 前端
├─ docker/minio/                    # MinIO 初始化脚本
├─ scripts/                         # 辅助 SQL / 脚本
├─ docker-compose.yml               # 一键启动编排
├─ Dockerfile                       # 后端镜像构建
└─ README.md
```

## 快速开始（本地开发）

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8+

### 2. 初始化数据库

执行以下任一 SQL 文件：

- `setup_mysql.sql`
- `database_setup.sql`

默认数据库连接（`src/main/resources/application.yml`）：

- database: `filesharing`
- username: `root`
- password: `123456`

### 3. 启动后端

```powershell
mvn spring-boot:run
```

默认行为：

- HTTPS: `https://localhost:8443`
- HTTP: `http://localhost:8080`（重定向到 8443）

如需仅使用 HTTP（本地调试更简单）：

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.ssl.enabled=false --server.port=8080"
```

### 4. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:3000`

`frontend/vite.config.ts` 默认代理到 `http://localhost:8080`。如果后端使用 HTTPS（8443），建议先设置：

```powershell
$env:VITE_API_PROXY_TARGET="https://localhost:8443"
npm run dev
```

### 5. 常用开发命令

后端：

```powershell
mvn clean compile
mvn test
```

前端：

```powershell
cd frontend
npm run type-check
npm run build
```

## Docker 一键启动

项目已提供前后端 + MySQL + MinIO 的编排。

```powershell
docker compose up -d --build
```

默认访问：

- 前端：`http://localhost:3000`
- 后端 API：`http://localhost:8080`
- MinIO API：`http://localhost:9000`
- MinIO Console：`http://localhost:9001`

查看关键日志：

```powershell
docker compose logs -f minio-init backend
```

停止：

```powershell
docker compose down
```

## 关键配置

来自 `src/main/resources/application.yml`（可被环境变量覆盖）：

- `HTTPS_ENABLED`（默认 `true`）
- `HTTPS_PORT`（默认 `8443`）
- `HTTP_PORT`（默认 `8080`）
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `STORAGE_TYPE`（默认 `minio`）
- `STORAGE_MINIO_ENDPOINT` / `STORAGE_MINIO_ACCESS_KEY` / `STORAGE_MINIO_SECRET_KEY` / `STORAGE_MINIO_BUCKET`
- `FILE_UPLOAD_PATH` / `FILE_UPLOAD_TEMP_PATH`
- `BACKUP_BASE_PATH`

## API 与调试资产

仓库内已包含接口清单与测试资产：

- `api_endpoints.md`
- `api_endpoints.json`
- `api_endpoints.tsv`
- `api_inventory_readme.md`
- `postman_collection.json`
- `comprehensive_api_test.ps1`
- `scripts/api_smoke_test.ps1`

## 安全提醒（开发环境）

当前默认配置包含开发态凭据，仅适合本地或测试环境：

- MySQL: `root / 123456`
- MinIO: `minioadmin / minioadmin`
- HTTPS keystore password: `changeit`
- JWT secret 已写入配置文件

上线前请务必替换所有默认密钥与口令，并收紧 CORS/公开接口策略。
