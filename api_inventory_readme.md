## 全量 API 接口清单（自动扫描）

> 扫描来源：`src/main/java/**` 控制器与上传控制器注解。
> 联调方式：后端直连 `http://localhost:8080` 与前端代理 `http://localhost:3001` 双通道冒烟。

- 本次扫描接口总数：65
- 双通道可达接口：65/65

| Method | Path | Controller | BackendStatus | ProxyStatus |
|---|---|---|---:|---:|
| DELETE | /api/backup/{backupPath} | BackupController | 403 | 403 |
| DELETE | /api/backup/cleanup | BackupController | 403 | 403 |
| DELETE | /api/cloud-storage/configs/{configId} | CloudStorageController | 403 | 403 |
| DELETE | /api/files/{fileId} | FileController | 403 | 403 |
| DELETE | /api/monitoring/cleanup | MonitoringController | 403 | 403 |
| DELETE | /api/recommendations/cleanup | RecommendationController | 403 | 403 |
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
