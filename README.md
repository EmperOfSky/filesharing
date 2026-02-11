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