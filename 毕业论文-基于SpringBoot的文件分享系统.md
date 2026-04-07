# 基于Spring Boot的文件分享系统

## 摘要

随着高校、企业及个人用户对在线文件存储、共享协作和跨终端访问需求的不断增长，传统依赖局域网共享目录或即时通信工具传输文件的方式逐渐暴露出权限控制粗放、文件版本管理薄弱、传输效率低和可扩展性不足等问题。针对上述问题，本文结合一个实际开发完成的全栈项目，对基于 Spring Boot 的文件分享系统进行了分析、设计与实现。该系统以后端 Spring Boot 2.7.5 为核心，前端采用 Vue 3、Vite、TypeScript 与 Pinia 构建交互式工作台，数据层采用 MySQL 8，文件存储支持本地存储与 MinIO 对象存储切换，并通过 JWT 完成无状态认证授权。

本文在扫描项目目录 `src/main/java`、`src/main/resources`、`frontend/src`、`docker`、`scripts`、`src/test` 以及接口清单文件的基础上，对系统的功能需求、总体架构、数据库设计、关键模块实现和测试结果进行了系统整理。当前项目包含 221 个后端源码文件、39 个前端源码文件、18 个控制器、33 个实体类、32 个仓储接口、47 个服务接口与实现类、19 个前端页面和 65 个可扫描接口。系统已实现用户认证、文件上传下载、秒传与分片上传、短链分享、快传中心、回收站、搜索与推荐、数据备份恢复、系统监控、协作文档以及 WebSocket 实时协作等功能。

在实现层面，系统通过文件 MD5 校验与并发锁机制支持秒传与防重复入库；通过分片上传记录表与临时目录管理支持大文件断点续传；通过分享记录与取件码记录实现公共分享和快传提取；通过备份元数据、JSON 导出和 ZIP 归档实现全量备份、增量备份和恢复校验；通过内存态文档状态管理、操作历史与块锁机制实现协作文档的实时编辑。测试结果表明，后端 `mvn test` 共执行 30 项测试且全部通过，接口扫描结果显示 65 个接口在后端直连与前端代理双通道下均可达；但前端 `npm run type-check` 仍存在若干未使用变量与导入，说明工程质量仍有进一步优化空间。

本文所完成的系统具备较强的工程实践价值，可为中小规模文件共享平台、课程设计项目和毕业设计实现提供参考，也为后续引入全文检索、分布式协作算法、对象存储多云适配和更细粒度权限控制奠定了基础。

**关键词：** Spring Boot；文件分享系统；Vue 3；JWT；WebSocket；MinIO

## Abstract

With the rapid growth of online file storage, sharing and collaborative editing requirements in universities, enterprises and individual users, traditional file transfer methods based on LAN shared folders or instant messaging tools are no longer sufficient. They often suffer from weak permission control, limited version management, low transfer efficiency and poor scalability. To address these problems, this thesis analyzes, designs and summarizes a practical file sharing system based on Spring Boot.

The system adopts Spring Boot 2.7.5 as the backend framework, Vue 3 with Vite and TypeScript as the frontend stack, MySQL 8 as the relational database, and supports both local storage and MinIO object storage. JWT is used to implement stateless authentication and authorization. Based on a full scan of the project folders, including backend source code, frontend source code, resource configuration, deployment scripts and test cases, this thesis presents the functional requirements, architecture design, database schema, core implementation and testing results of the system.

The project currently contains 221 backend source files, 39 frontend source files, 18 controllers, 33 entity classes, 32 repositories, 47 service interfaces and implementations, 19 frontend pages and 65 scanned API endpoints. The implemented functions include user authentication, file upload and download, instant upload based on MD5, chunk upload and resume, short-link sharing, quick transfer with pickup codes, recycle bin management, search and recommendation, backup and recovery, monitoring, collaborative documents and WebSocket-based real-time collaboration.

In implementation, the system uses MD5 checking and concurrent locks to avoid duplicate file records, chunk upload records and temporary storage to support resumable large-file uploads, share records and pickup-code records to support public access, metadata and checksum verification to support backup and restore, and in-memory document states with operation histories and block locking to support real-time collaboration. Test results show that all 30 backend tests passed successfully. The API inventory also indicates that 65 endpoints are reachable through both direct backend access and frontend proxy. However, the frontend type-check process still reports unused imports and variables, which indicates that maintainability and code hygiene can be further improved.

This system has good practical value for graduation projects and small to medium file-sharing platforms, and provides a useful basis for future work such as full-text search, distributed collaboration algorithms, multi-cloud object storage integration and finer-grained access control.

**Keywords:** Spring Boot; File Sharing System; Vue 3; JWT; WebSocket; MinIO

## 第1章 绪论

### 1.1 研究背景

在数字化学习、远程办公和团队协同日益普及的背景下，文件已成为信息交换和知识沉淀的重要载体。无论是课程资料、实验数据、项目文档，还是图片、视频和代码文件，都需要通过统一的平台实现上传、管理、共享、检索与备份。传统的 FTP、网络共享盘或基于聊天工具的文件传输虽然能够满足基本交换需求，但通常存在以下问题：

1. 文件权限控制粒度较粗，难以同时满足公开分享、私有管理与临时授权下载等多种场景。
2. 面向 Web 的使用体验不足，缺少统一的工作台、回收站、协作编辑与统计分析能力。
3. 大文件上传稳定性不高，网络中断后往往需要重新传输，带来较高的时间与带宽成本。
4. 缺少灵活的存储后端切换能力，难以兼容本地磁盘和对象存储等多种部署模式。
5. 缺少备份恢复与监控能力，系统在出现误删或故障时不易恢复。

Spring Boot 以其约定优于配置、快速集成和生态完善等特点，已成为 Java Web 应用开发的重要技术路线。将 Spring Boot 与前后端分离架构、JWT 鉴权、对象存储、WebSocket 和现代前端框架相结合，可以较好地构建具备实用性和扩展性的文件分享平台。因此，围绕“基于 Spring Boot 的文件分享系统”开展设计与实现具有明确的现实意义和教学价值。

### 1.2 研究意义

本课题的研究意义主要体现在以下三个方面：

1. 工程实践意义。通过实现从用户认证、文件管理到分享、协作和运维的完整业务链路，可以验证 Spring Boot 在中等复杂度业务系统中的适用性。
2. 教学训练意义。文件分享系统涉及数据库设计、权限控制、文件存储、前后端接口设计、实时通信和测试验证等多个知识点，能够较全面地体现计算机科学与技术专业本科阶段的综合能力。
3. 扩展应用意义。该系统不仅适用于课程资料共享和实验文件管理，也可以作为小型团队协作平台、校内资源流转平台或企业内部轻量文件中台的雏形。

### 1.3 国内外相关研究现状

从系统形态看，早期文件共享平台更多依赖 FTP、SMB/NFS 等协议，重点解决网络传输和目录共享问题，但在 Web 化操作体验、精细化权限控制和在线协作方面能力有限。随着浏览器应用生态成熟，越来越多的文件管理系统开始采用前后端分离架构，以 RESTful API 为交互基础，并引入对象存储、令牌鉴权、预签名上传和实时协作机制。

国外相关产品通常在大规模对象存储接入、协作编辑、审计与安全策略方面较为成熟，强调多租户、高可用和云原生部署。国内同类系统则更重视私有化部署、本地化运维、与组织内部账号体系集成以及符合本地网络环境的传输体验。总体来看，现代文件分享平台的发展趋势主要表现为：

1. 由单纯的文件上传下载向“文件管理 + 分享分发 + 团队协作 + 运维监控”综合平台演进。
2. 由本地磁盘存储向对象存储、云存储及多级存储抽象演进。
3. 由同步表单上传向秒传、分片上传、断点续传和直传代理等高效传输模式演进。
4. 由静态文档管理向协作文档、在线评论、实时消息和编辑冲突处理演进。

但在高校毕业设计场景中，许多项目仍停留在基础的注册登录、文件上传下载和简单分享层面，对备份恢复、推荐、监控、协作等功能覆盖不足。本文所分析的项目在这些方面具备较完整的实现，因而具有较强的研究与总结价值。

### 1.4 研究内容

本文围绕一个实际完成的工程项目展开，研究内容包括：

1. 对文件分享系统的业务场景与功能需求进行分析。
2. 基于 Spring Boot、Vue 3、MySQL、MinIO 和 WebSocket 设计系统总体架构。
3. 从用户、文件、文件夹、分享、取件码、回收站、协作项目、协作文档等角度设计数据库结构。
4. 对认证授权、文件上传、秒传、分片上传、短链分享、快传中心、搜索推荐、备份恢复和实时协作等关键模块进行实现分析。
5. 基于项目已有测试代码和实际执行结果，对系统进行功能与工程质量评估，并总结不足与改进方向。

### 1.5 论文结构

全文共分为八章。第一章介绍研究背景、意义、现状与研究内容；第二章介绍系统涉及的关键技术；第三章完成需求分析；第四章给出系统总体设计；第五章对关键功能实现进行分析；第六章介绍系统测试与结果；第七章总结系统不足并提出改进方向；第八章给出全文总结。

## 第2章 相关技术

### 2.1 Spring Boot 技术

Spring Boot 是基于 Spring 生态构建的快速开发框架，能够通过自动配置机制简化项目搭建过程。本文项目以后端 `file-sharing-system` 为核心，采用 Spring Boot 2.7.5 组织 Web、JPA、Security、Validation、WebSocket、Actuator 等模块，使系统能够在较少样板配置下快速形成完整的企业级应用骨架。

在本系统中，Spring Boot 主要承担以下职责：

1. 提供 RESTful 接口开发能力，支撑前后端分离。
2. 通过依赖注入组织控制器、服务层、仓储层与工具类。
3. 借助配置文件和环境变量完成数据库、HTTPS、对象存储、备份路径等参数注入。
4. 通过集成测试生态支持控制器、服务和工具类验证。

### 2.2 Spring Security 与 JWT

Spring Security 用于实现系统访问控制，JWT 用于支撑无状态认证。系统登录成功后，由 `JwtUtil` 生成带有用户 ID 和用户名信息的令牌，前端通过 Axios 请求拦截器在请求头中自动附带 `Authorization: Bearer <token>`。后端 `SecurityConfig` 采用 `SessionCreationPolicy.STATELESS` 关闭会话状态管理，并对公开接口与受保护接口进行区分。

这种设计的优点在于：

1. 无需在服务端维护会话，适合前后端分离和容器化部署。
2. 扩展性较好，后续可以接入刷新令牌、黑名单和更细粒度角色控制。
3. 与前端单页应用天然契合，用户状态可在本地持久化。

### 2.3 Spring Data JPA 与 MySQL

系统数据层采用 MySQL 8 作为主数据库，ORM 框架使用 Spring Data JPA。项目中共存在 33 个实体类和 32 个仓储接口，分别覆盖用户、文件、文件夹、分享、搜索记录、回收站、推荐、协作文档、云存储配置等核心数据对象。JPA 提供了实体映射、分页查询和事务支持，使业务层能够更专注于领域逻辑。

### 2.4 Vue 3、Vite 与 Pinia

前端采用 Vue 3 组合式 API 构建，构建工具为 Vite 5，状态管理使用 Pinia，路由管理使用 Vue Router 4。系统当前实现 19 个页面，覆盖登录注册、文件管理、快传中心、协作工作区、备份页面、搜索推荐等业务界面。Axios 统一封装于 `http.ts` 中，实现了令牌注入、统一响应解包和 401 自动跳转登录。

### 2.5 WebSocket 技术

实时协作模块采用 WebSocket 技术实现。系统在 `/ws/collaboration` 路径注册 WebSocket 处理器，通过握手拦截器解析 JWT 信息，以此建立用户身份与文档会话之间的映射关系。相比纯轮询方式，WebSocket 在多人协作编辑、光标广播、聊天通知和状态同步方面具有更低的延迟和更高的通信效率。

### 2.6 MinIO 与 Docker

为提升部署灵活性，系统在文件存储层实现了本地磁盘与 MinIO 的双模式切换。`FileStorageUtil` 根据配置判断当前采用本地存储还是 MinIO 对象存储，并对文件保存、读取、删除和分片合并进行统一抽象。系统同时提供 `docker-compose.yml`，可一键启动 MySQL、MinIO、后端服务和前端静态站点，便于开发测试和演示部署。

## 第3章 系统需求分析

### 3.1 可行性分析

#### 3.1.1 技术可行性

系统所采用的 Spring Boot、Vue 3、MySQL、JWT、WebSocket 与 MinIO 均为成熟技术，社区生态完善、资料丰富，能够满足毕业设计在时间与复杂度上的平衡要求。项目已经具备完整源码、配置、Docker 编排与测试代码，说明从技术实现角度是可行的。

#### 3.1.2 经济可行性

该系统可在普通开发机上完成运行与测试。MySQL、MinIO、Node.js、Maven 及相关依赖均可开源获取，部署成本较低。若应用于学校实验环境或中小团队内部场景，不需要高昂的商业软件授权费用。

#### 3.1.3 操作可行性

系统提供图形化 Web 界面，前端路由清晰，用户无需掌握复杂命令即可完成大部分操作。对管理员而言，也可以通过统一界面完成快传配置、记录审计、备份管理和监控查看，因此具有较好的操作可行性。

### 3.2 功能需求分析

结合代码扫描结果，系统功能需求可归纳如下：

1. 用户与认证管理。支持用户注册、登录、获取个人资料、修改资料和修改密码。
2. 文件与文件夹管理。支持文件上传、下载、预览、重命名、移动、复制、公开设置、收藏、版本查看以及文件夹的创建、查看、重命名和删除。
3. 高效上传。支持基于 MD5 的秒传和大文件分片上传、断点续传、上传进度查询与过期清理。
4. 分享与快传。支持生成短链分享、访问密码、下载授权、分享监控，以及基于取件码的文本快传、文件快传和预签名上传。
5. 搜索与推荐。支持关键词搜索、搜索建议、热词、搜索历史和基于多策略的个性化推荐。
6. 回收站管理。支持删除文件或文件夹到回收站、恢复、批量恢复、永久删除和过期清理。
7. 备份与恢复。支持全量备份、增量备份、异步备份、备份验证、恢复、配置导入导出和统计分析。
8. 协作与实时通信。支持协作项目、成员管理、协作文档、评论、快照恢复、实时编辑、在线协作者和聊天。
9. 运维与监控。支持系统健康检查、指标统计、告警记录和报告查看。
10. 存储配置。支持云存储配置管理、连接测试、能力查询和默认配置切换。

### 3.3 非功能需求分析

除功能要求外，系统还需满足以下非功能需求：

1. 安全性。用户密码应加密存储，接口需要鉴权，重要文件下载应受权限控制。
2. 可扩展性。系统应支持本地存储与对象存储切换，并保留对旧接口的兼容能力。
3. 可维护性。系统应采用分层结构与模块化设计，便于后续添加功能。
4. 可用性。应提供统一前端界面和合理的错误提示。
5. 性能与稳定性。需支持并发上传、断点续传、异步备份和实时消息通信。

### 3.4 用户角色分析

结合当前实现，系统主要存在三类角色：

1. 普通用户。完成文件上传下载、个人管理、搜索、分享和协作文档使用。
2. 管理员。除普通用户能力外，还可以进行快传配置、记录审计等管理功能。
3. 游客用户。在快传模块开启游客上传时，可参与文本或文件快传及取件码提取；在短链分享场景下，可访问公开分享页面。

## 第4章 系统总体设计

### 4.1 系统架构设计

系统采用典型的前后端分离与分层架构，可概括为“表示层 + 接口层 + 业务层 + 数据层 + 存储层”五层结构：

```text
浏览器/客户端
    |
Vue 3 + Element Plus + Pinia
    |
Axios / WebSocket
    |
Spring Boot Controller 层
    |
Service 业务层
    |
Repository / JPA 数据访问层
    |
MySQL 8 数据库
    |
本地磁盘存储 或 MinIO 对象存储
```

该架构具有以下优点：

1. 前后端职责清晰，便于分工与迭代。
2. 控制器层负责参数接收与响应封装，服务层负责业务逻辑，仓储层负责数据持久化，分层边界明确。
3. 存储层被工具类与服务层抽象封装，后续可扩展第三方云存储。
4. WebSocket 与 HTTP API 并存，既支持传统业务请求，也支持实时协作。

### 4.2 模块划分设计

根据目录扫描结果，后端主要模块包括：

1. `controller`：18 个控制器，负责认证、文件、文件夹、分享、搜索、推荐、回收站、备份、协作、云存储、预览和兼容接口。
2. `service` 与 `service/impl`：47 个服务接口与实现类，承担业务逻辑。
3. `repository`：32 个数据仓储接口。
4. `entity`：33 个实体类，对应主要数据库表。
5. `config`：包含安全、HTTPS 重定向、WebSocket、定时任务等配置。
6. `backup`、`upload`、`websocket`、`util`：承担专项业务能力。

前端模块则以路由为核心，共实现 19 个页面，包括工作台总览、文件管理、文件预览、快传中心、取件空间、分享管理、搜索、推荐、回收站、备份、协作项目、协作工作区和个人中心等。

### 4.3 数据库设计

系统既包含初始化 SQL 文件，也包含基于 JPA 的实体映射。结合 `setup_mysql.sql` 与实体类设计，核心数据表如下：

| 表名 | 关键字段 | 主要作用 |
| --- | --- | --- |
| `users` | `username`、`email`、`password`、`storage_quota` | 存储用户基础信息、角色和存储配额 |
| `folders` | `folder_name`、`parent_id`、`owner_id`、`folder_path` | 维护文件夹层级结构 |
| `files` | `storage_name`、`original_name`、`md5_hash`、`uploader_id`、`folder_id` | 维护文件元数据、状态和统计字段 |
| `share_records` | `share_key`、`file_id`、`folder_id`、`expire_time`、`password` | 实现短链分享和访问控制 |
| `pickup_code_records` | `code`、`share_type`、`storage_path`、`expire_at` | 实现快传取件码和临时分享 |
| `chunk_upload_records` | `upload_id`、`total_chunks`、`uploaded_chunks`、`status` | 管理分片上传会话 |
| `recycle_bin` | `item_id`、`item_type`、`deleted_by`、`expire_at` | 实现逻辑删除与恢复 |
| `collaboration_projects` | `project_name`、`owner_id`、`member_count` | 实现协作项目 |
| `collaborative_documents` | `project_id`、`document_name`、`content`、`version` | 实现协作文档与版本状态 |
| `backup_task_entity` | `task_id`、`backup_type`、`status` | 记录异步备份任务状态 |

在实体关系方面，`User` 与 `FileEntity`、`Folder`、`ShareRecord`、`CollaborationProject` 等形成一对多关系；`Folder` 与自身形成父子层级结构；`CollaborativeDocument` 依附于 `CollaborationProject`；`ShareRecord` 可关联文件或文件夹；`RecycleBin` 则记录被删除项目的原始位置信息。此设计能够较好支撑系统业务。

### 4.4 接口与安全设计

系统接口以 REST 风格为主，基础路径包括 `/api/auth`、`/api/files`、`/api/folders`、`/api/shares`、`/api/public`、`/api/backup`、`/api/collaboration` 等。根据接口扫描结果，系统总计 65 个可扫描接口。`SecurityConfig` 中对认证、公开分享、快传公共接口和健康检查等路径进行了放行，其余业务接口需要通过 JWT 鉴权访问。

安全设计要点如下：

1. 登录后签发 JWT，避免服务端维护会话状态。
2. 用户密码通过 `BCryptPasswordEncoder` 加密存储。
3. 系统支持 HTTPS 启动，并通过 `HttpsRedirectConfig` 将 HTTP 端口重定向到 HTTPS 端口。
4. WebSocket 握手阶段通过令牌解析用户身份，实现协作文档访问控制。

### 4.5 部署设计

系统支持两种部署方式：

1. 本地开发部署。后端默认以 HTTPS `8443` 启动，并提供 HTTP `8080` 重定向；前端通过 Vite 在 `3000` 端口运行。
2. Docker Compose 部署。通过 `docker-compose.yml` 一键启动 `mysql`、`minio`、`minio-init`、`backend` 和 `frontend` 五个服务，形成可运行演示环境。

这种设计兼顾了开发调试便利性与演示部署完整性。

## 第5章 系统详细实现

### 5.1 用户认证与个人管理实现

用户认证模块由 `AuthController`、`UserServiceImpl`、`JwtUtil`、`JwtAuthenticationFilter` 与 `SecurityConfig` 等类共同构成。其实现流程如下：

1. 用户注册时，系统检查用户名和邮箱唯一性，并将密码通过 BCrypt 加密后保存。
2. 用户登录时，系统支持通过用户名或邮箱进行身份识别，验证通过后写入最后登录时间，并生成 JWT 令牌。
3. 前端在 `auth.ts` 中管理用户令牌和个人状态，在 `http.ts` 中通过请求拦截器自动拼接令牌。
4. 后端通过过滤器解析令牌中的用户 ID，并按接口权限规则决定是否放行。

这种实现方式结构清晰，满足基本安全要求，同时便于后续扩展多角色授权和单点登录能力。

### 5.2 文件管理模块实现

文件管理是系统的核心业务之一，对应 `FileController`、`FolderController` 和 `FileServiceImpl` 等类。其主要特点如下：

1. 文件上传支持普通上传和秒传。`FileServiceImpl` 在上传前先计算 MD5 值，并调用 `checkFileExists` 判断系统中是否已存在相同内容文件。
2. 为避免并发条件下同一文件重复落库，服务层使用 `ConcurrentMap<String, ReentrantLock>` 构建上传锁，在第一次检查未命中后加锁复查。
3. 文件删除采用元数据逻辑删除与底层文件删除结合的方式，同时更新用户已使用空间。
4. 文件支持重命名、移动、复制、公开访问、收藏和版本恢复等操作。
5. 文件查询支持短时缓存，降低频繁列表请求的代价。

该模块在工程实现上兼顾了功能完整性和一定程度的并发安全，是本文系统较为成熟的部分之一。

### 5.3 文件存储与分片上传实现

#### 5.3.1 本地与 MinIO 双模式存储

`FileStorageUtil` 封装了文件保存、读取、删除、复制、分片保存和分片合并等能力，并在初始化阶段根据配置决定使用本地磁盘还是 MinIO。若 MinIO 配置不完整，系统会自动回退到本地存储模式，这使系统能够适应不同的运行环境。

#### 5.3.2 秒传机制

秒传机制的核心思路是对上传文件计算 MD5 值，并将其与已存在文件记录进行比较。如果发现相同哈希值的可用文件，则无需再次写入物理存储，仅复用已有文件元数据或快速建立新的逻辑记录，从而降低重复上传带来的磁盘和网络开销。

#### 5.3.3 分片上传机制

分片上传由 `ChunkUploadController`、`ChunkUploadServiceImpl` 与 `ChunkUploadRecord` 协作完成。其处理流程为：

1. 客户端调用初始化接口，系统生成 `uploadId` 并写入上传记录。
2. 客户端按分片顺序上传文件块，服务端将分片写入临时目录。
3. 当已上传分片数达到总分片数后，服务端将所有分片合并为完整文件。
4. 合并完成后创建文件实体记录，并更新上传状态为完成。
5. 定时任务或清理逻辑可以对过期上传会话进行清除。

该机制解决了大文件在不稳定网络环境下的上传问题，提高了系统的容错性和可用性。

### 5.4 分享与快传模块实现

#### 5.4.1 短链分享

短链分享功能主要由 `ShareController` 和 `ShareServiceImpl` 提供。系统使用安全随机字符集生成长度为 8 的分享标识 `shareKey`，支持以下能力：

1. 文件分享和文件夹分享两种模式。
2. 可设置过期时间、最大访问次数和访问密码。
3. 支持启用、停用、删除和“我的分享”列表查看。
4. 支持对访问日志进行 PV、UV 与独立 IP 数统计。

访问密码采用加密存储，公共访问时需经过状态校验与密码校验，体现了较为完整的分享访问控制流程。

#### 5.4.2 快传中心

快传中心由 `FileCodeBoxController`、`FileCodeBoxAdminController` 及相关 DTO、实体与服务组成，是区别于普通短链分享的独立业务域。其特点包括：

1. 支持文本快传和文件快传。
2. 支持取件码提取、授权下载和游客上传开关。
3. 支持预签名上传初始化、代理上传与确认流程。
4. 支持管理员查看记录、导出记录和修改记录状态。
5. 保留旧版接口兼容能力，便于旧客户端迁移。

这一模块使系统不仅能管理个人文件，也能承担临时资料流转平台的角色。

### 5.5 搜索、推荐与回收站实现

#### 5.5.1 搜索实现

搜索模块由 `SearchController` 与 `SearchServiceImpl` 实现，支持按文件名、内容兼容模式和高级条件进行查询。当前实现中，“内容搜索”尚未接入真正的全文索引，而是以文件名匹配作为兼容实现，同时记录搜索关键词、耗时、筛选条件、客户端 IP 和用户代理信息，并提供搜索建议、热词、历史和统计能力。

#### 5.5.2 推荐实现

推荐模块由 `SmartRecommendationServiceImpl` 实现，采用多策略融合方式生成个性化推荐，主要包括：

1. 基于用户行为相似性的协同过滤推荐。
2. 基于文件内容和标签的相似性推荐。
3. 基于热点趋势的推荐。
4. 基于标签关联的推荐。
5. 基于时间上下文的推荐。

系统会对推荐结果进行去重、排序、过期控制，并统计查看率和采纳率。虽然推荐算法仍偏规则化，但相比单一热门推荐更具系统性。

#### 5.5.3 回收站实现

回收站模块通过 `RecycleBinController` 与 `RecycleBin` 实体实现。用户删除文件或文件夹后，系统先记录其原始名称、路径、父级位置、删除用户和过期时间，而非立即永久删除。用户可在一定时间窗口内完成恢复、恢复到指定位置、批量恢复或彻底清除，降低误操作损失。

### 5.6 备份恢复与监控实现

#### 5.6.1 备份恢复实现

`DataBackupService` 是本系统中工程化程度较高的模块之一，具备以下能力：

1. 创建全量备份。将数据库关键数据导出为 JSON，并可选地将文件归档为 ZIP。
2. 创建增量备份。根据指定时间点筛选新增或变更数据。
3. 支持异步备份。通过 `CompletableFuture` 启动后台任务，并持久化任务状态。
4. 生成备份元数据。记录备份类型、时间、校验和等信息。
5. 支持 SHA-256 校验与恢复。恢复时先核验备份完整性，再执行数据库和文件恢复。

这一实现使系统具备基本的灾备能力，不再只是教学演示级别的占位接口。

#### 5.6.2 系统监控实现

系统结合 `MonitoringController`、`SystemMonitoringService` 和 Actuator/Micrometer 提供指标、健康状态、告警与报告接口，可用于观察系统运行状况。配合文件统计与分享监控，可在一定程度上支撑系统管理和运维分析。

### 5.7 协作与实时通信实现

协作模块是系统的特色功能之一。后端 `CollaborationController` 代码行数接近 800 行，提供项目管理、成员管理、文档、快照、评论、实时会话和块级操作等大量接口；`RealTimeCollaborationServiceImpl` 则接近 700 行，承担实时文档状态维护。

其关键实现机制包括：

1. 以 `documentStates` 保存当前文档状态，以 `documentCollaborators` 保存在线协作者信息。
2. 以 `editHistories` 和 `operationHistories` 记录编辑操作与已应用版本，支持客户端版本落后时进行自动合并。
3. 通过 `DocumentLockManager` 和块级锁接口控制文档或区块编辑冲突。
4. 在编辑操作后将文档状态持久化回数据库，避免刷新丢失。
5. 通过 `CollaborationWebSocketHandler` 广播加入、离开、编辑、光标、聊天和同步消息。

这一设计虽然尚未达到 CRDT 或 OT 级别的分布式协同算法复杂度，但已经具备了多人实时协作系统的基本骨架和较强演示价值。

## 第6章 系统测试与结果分析

### 6.1 测试环境与测试范围

系统测试主要基于源码仓库中的已有测试类和本次执行结果完成，测试范围包括：

1. 控制器测试：如 `BackupControllerTest`、`ShareControllerTest`。
2. 服务测试：如 `StatisticsServiceTest`、`CloudStorageCapabilityServiceTest`。
3. 工具类测试：如 `FileStorageUtilTest`、`PasswordEncoderTest`。
4. 集成层测试：如 `ServiceIntegrationTest`。
5. 接口清单扫描：基于 `api_inventory_readme.md` 对 65 个接口进行可达性整理。
6. 前端工程检查：执行 TypeScript 类型检查，验证前端代码规范性。

### 6.2 后端测试结果

本次在项目根目录执行 `mvn test`，结果如下：

1. 共执行 30 项测试。
2. 失败数 0，错误数 0，跳过数 0。
3. 测试覆盖控制器、服务、工具类和基础集成验证。

从输出结果看，备份控制器、分享控制器、密码加密、统计服务与文件存储工具等关键模块均通过了现有测试，说明系统后端在当前版本下具有较好的可运行性。

### 6.3 接口扫描结果分析

根据 `api_inventory_readme.md`，系统共扫描到 65 个接口，且在后端直连和前端代理两种联调通道下均可达，双通道可达率为 65/65。需要说明的是，其中大量接口返回 401 或 403，并不表示接口异常，而是说明接口路径和权限策略生效。对毕业设计而言，这表明系统的接口组织较为完整，且安全控制逻辑已经实际参与运行。

### 6.4 前端工程检查结果

在前端目录执行 `npm run type-check` 后，当前版本未完全通过。错误集中于若干页面存在未使用的导入与变量，例如：

1. `CollaborationWorkspacePage.vue` 中存在未使用的组件导入和方法。
2. `FileCodeBoxRecordsPage.vue` 中存在未使用变量。
3. `RecommendationPage.vue` 中存在未使用图标导入和分页方法。
4. `ShareAccessPage.vue` 与 `ShareManagePage.vue` 中存在未使用图标或组件。

这些问题不会否定系统的总体实现，但说明前端代码在发布前还需要做静态检查修复和工程清理。

### 6.5 功能与质量分析

结合测试结果与代码结构，系统目前表现出以下特点：

1. 功能覆盖较广。相比普通毕业设计仅实现基础上传下载，本系统已拓展到快传、协作、备份、监控和推荐等多个业务域。
2. 后端稳定性较好。已有单元测试和控制器测试能够覆盖部分核心流程。
3. 工程完整度较高。项目同时包含前端、后端、Docker、SQL 脚本、接口清单和测试代码。
4. 仍存在工程细节问题。前端类型检查未通过，部分模块仍有简化实现，不宜直接视为生产级系统。

## 第7章 系统存在的问题与改进方向

尽管系统已经实现了较为完整的功能集合，但从工程完善度和产品化角度看，仍存在以下不足：

### 7.1 全文检索能力不足

当前 `SearchServiceImpl` 的“内容搜索”实际上仍以文件名匹配作为兼容实现，尚未接入 Elasticsearch、Lucene 等全文检索引擎。因此，在大规模文档内容搜索场景下，系统的准确性和扩展性仍有限。

### 7.2 云存储能力仍偏简化

`CloudStorageServiceImpl` 已实现配置管理、默认配置切换和连接测试等基础能力，但部分上传、签名 URL 和迁移逻辑仍是简化实现，说明系统的云存储抽象层已经建立，但具体 provider 的生产级接入仍需继续补完。

### 7.3 实时协作尚未引入成熟分布式协同算法

当前协作模块主要基于内存态文档状态、版本比较和自动合并策略实现，适用于单体应用和中等并发演示场景。若面向真正的大规模多人同时编辑，仍需要引入 OT、CRDT 或分布式消息总线机制，以提升跨实例协作的一致性。

### 7.4 前端工程规范仍需加强

前端类型检查中存在未使用变量和导入，说明组件演化过程中存在清理不彻底的问题。后续应补充 ESLint、统一代码规范，并将静态检查纳入持续集成流程。

### 7.5 安全默认值需要收敛

当前配置文件中仍包含开发环境默认口令、广泛放开的 CORS 和固定 JWT 密钥等设置。这些配置适合教学和本地测试，但若实际部署，需要进一步强化安全基线。

### 7.6 后续改进方向

围绕上述问题，本文认为系统可从以下方向继续优化：

1. 接入全文检索引擎，实现真正的内容级搜索与高亮。
2. 完善对象存储 provider 适配，实现真实签名、迁移与回调链路。
3. 为协作模块引入 CRDT 或 OT 算法，并支持多实例部署。
4. 加强权限系统，细化到文件夹级、项目级和成员角色级授权。
5. 完善前端静态检查、端到端测试与 CI/CD 流程。
6. 收紧开发默认配置，增加审计日志与风控策略。

## 第8章 总结

本文基于一个真实完成的全栈工程项目，对“基于 Spring Boot 的文件分享系统”进行了系统化梳理与总结。通过对项目全部有效工程目录的扫描分析，本文明确了系统的业务目标、技术路线、架构设计、数据库结构和关键模块实现，并结合测试结果对系统当前状态进行了客观评估。

研究表明，基于 Spring Boot、Vue 3、MySQL、JWT、MinIO 和 WebSocket 的技术组合，能够较好地支撑中小规模文件分享系统的建设需求。系统不仅完成了传统的文件上传下载和用户认证，还扩展出秒传、分片上传、短链分享、快传中心、回收站、个性化推荐、备份恢复、运维监控和实时协作等功能，体现了较强的综合设计与实现能力。

同时，本文也指出了系统在全文检索、云存储深度适配、前端工程规范和协作算法等方面的不足。总体而言，该系统已具备毕业设计项目应有的完整性、可运行性和扩展潜力。未来若在搜索、协作、安全和云化方面继续演进，可进一步发展为更具实用价值的文件共享与协作平台。

## 参考文献

[1] Johnson R. Expert One-on-One J2EE Design and Development[M]. Indianapolis: Wrox Press, 2002.

[2] Walls C. Spring Boot in Action[M]. Shelter Island: Manning Publications, 2016.

[3] Fielding R T. Architectural Styles and the Design of Network-based Software Architectures[D]. University of California, Irvine, 2000.

[4] Kleppmann M. Designing Data-Intensive Applications[M]. Sebastopol: O'Reilly Media, 2017.

[5] Oracle. Java Platform, Standard Edition Documentation[EB/OL].

[6] Oracle. MySQL 8.0 Reference Manual[EB/OL].

[7] Jones M, Bradley J, Sakimura N. RFC 7519: JSON Web Token (JWT)[S]. IETF, 2015.

[8] Vue.js Team. Vue.js Documentation[EB/OL].

[9] 萨师煊, 王珊. 数据库系统概论[M]. 北京: 高等教育出版社.

[10] Spring Team. Spring Framework and Spring Boot Reference Documentation[EB/OL].

## 致谢

在本次毕业设计的实现与论文整理过程中，Spring Boot、Vue 3、MySQL、MinIO 等开源技术生态为系统搭建提供了良好的技术基础。项目源码中已有的前后端实现、接口清单、测试代码和部署脚本为本文撰写提供了直接依据。谨向指导教师在选题、架构思路和论文写作方面的指导表示感谢，也向所有为开源社区贡献文档、框架与工具的开发者表示感谢。

