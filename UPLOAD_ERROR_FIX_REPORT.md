# 文件上传 UNKNOWN_ERROR 修复报告

## 问题诊断
用户上传文件时返回 `UNKNOWN_ERROR`，导致无法识别具体错误原因。根本原因是异常处理不完善，具体表现为：

1. **异常信息丢失**：GlobalExceptionHandler 返回通用的"系统异常"消息，没有返回实际的错误细节
2. **层层try-catch**：控制器中的过度try-catch阻挠了异常的正确传播
3. **日志不详细**：错误日志缺少完整的堆栈跟踪

## 修复清单

### 1. ✅ 改进 GlobalExceptionHandler (已完成)
**文件**: [src/main/java/com/filesharing/exception/GlobalExceptionHandler.java](src/main/java/com/filesharing/exception/GlobalExceptionHandler.java#L95-L115)

**改动**:
- 异常处理器现在返回实际的错误信息，而不是通用的"系统异常"
- 添加完整的堆栈跟踪日志到错误日志中
- 异常响应中包含具体的错误原因

```java
// 改进前
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
    log.error("未知异常: ", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("UNKNOWN_ERROR", "系统异常，请稍后重试"));
}

// 改进后
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
    log.error("未知异常: {}", e.getMessage(), e);  // 添加完整堆栈跟踪
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("UNKNOWN_ERROR", 
                e.getMessage() != null ? e.getMessage() : "系统异常，请稍后重试"));
}
```

### 2. ✅ 修复 FileController 异常处理 (已完成)
**文件**: [src/main/java/com/filesharing/controller/FileController.java](src/main/java/com/filesharing/controller/FileController.java#L45-L57)

**改动**:
- 移除冗余的try-catch块，让异常自然传播到GlobalExceptionHandler
- 添加基本的参数验证
- 异常处理由统一的全局处理器负责

```java
// 改进前
@PostMapping("/upload")
public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(...) {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("文件上传失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));  // 错误信息可能为null
    }
}

// 改进后
@PostMapping("/upload")
public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(...) {
    User currentUser = userService.getCurrentUser(request);
    
    if (file == null || file.isEmpty()) {
        throw new BusinessException("文件不能为空");
    }
    
    FileUploadResponse response = fileService.uploadFile(file, folderId, currentUser);
    return ResponseEntity.ok(ApiResponse.success("文件上传成功", response));
}
```

### 3. ✅ 改进 FileServiceImpl 错误处理 (已完成)
**文件**: [src/main/java/com/filesharing/service/impl/FileServiceImpl.java](src/main/java/com/filesharing/service/impl/FileServiceImpl.java#L120-L127)

**改动**:
- 分离业务异常和系统异常的处理
- 业务异常直接抛出，无需重新包装
- 系统异常记录完整的堆栈跟踪并包装为业务异常
- 异常链接(Throwable cause)保留，便于调试

```java
// 改进前
} catch (Exception e) {
    log.error("文件上传失败: {}", e.getMessage());
    throw new BusinessException("文件上传失败: " + e.getMessage());
}

// 改进后
} catch (BusinessException e) {
    log.error("业务异常 - 文件上传失败: {}", e.getMessage());
    throw e;  // 直接抛出，保留原始信息
} catch (Exception e) {
    log.error("文件上传失败: {}", e.getMessage(), e);  // 完整堆栈跟踪
    throw new BusinessException("文件上传失败: " + e.getMessage(), e);  // 保留cause
}
```

### 4. ✅ 改进 MobileUploadController (已完成)
**文件**: [src/main/java/com/filesharing/controller/mobile/MobileUploadController.java](src/main/java/com/filesharing/controller/mobile/MobileUploadController.java#L30-L47)

**改动**: 
- 与FileController相同的改进模式
- 移除过度的try-catch
- 让异常传播到全局处理器

## 错误追踪流程

### 修复前的问题流程：
```
FileController.uploadFile()
  ↓ 捕获所有Exception
  ↓ 返回e.getMessage()可能为null
  → HttpStatus.BAD_REQUEST 400
  → 隐藏真实错误，前端收到模糊的错误
```

### 修复后的改进流程：
```
FileController.uploadFile()
  ↓ 无try-catch，异常自然传播
  ↓ GlobalExceptionHandler 捕获
  ↓ 区分异常类型（BusinessException/其他Exception）
  ↓ 记录完整堆栈跟踪到日志
  ↓ 返回具体的错误信息和原因
  → HttpStatus.INTERNAL_SERVER_ERROR 500（如果是系统异常）
  → HttpStatus.BAD_REQUEST 400（如果是业务异常）
  → 前端收到详细的错误信息，便于诊断
```

## 常见问题诊断

### 如果看到以下错误，原因分析如下：

| 错误信息 | 可能原因 | 解决方案 |
|---------|--------|--------|
| `未获取到当前用户信息` | Authorization header 缺失或无效 | 检查前端是否发送Bearer token |
| `目标文件夹不存在` | folderId 指向不存在的文件夹 | 检查前端发送的folderId参数 |
| `无权上传到该文件夹` | 用户无权限写入该文件夹 | 检查权限配置或改用个人目录 |
| `文件上传失败: java.io.IOException` | 磁盘I/O问题或路径权限问题 | 检查uploads目录权限和磁盘空间 |
| `文件上传失败: 数据库异常` | 数据库连接或保存失败 | 检查数据库连接和磁盘空间 |

## 测试方法

### 方法1：使用Swagger UI查看错误详情
```
1. 启动应用：mvn spring-boot:run
2. 访问 http://localhost:8080/api/swagger-ui/
3. 在Files → POST /api/files/upload 中测试
4. 观察响应中的具体错误信息
```

### 方法2：查看应用日志
```
1. 检查console日志中是否有具体的异常堆栈跟踪
2. 错误日志现在包含完整信息，便于诊断问题根源
```

## 部署前检查清单
- [ ] 编译通过：`mvn -DskipTests compile`
- [ ] 测试通过：`mvn test`
- [ ] 日志中能看到详细的异常信息 
- [ ] 前端发送了正确的Authorization header
- [ ] uploads目录具有写权限
- [ ] 数据库连接正常

## 后续监控建议
1. 监控应用日志，使用搜索关键字"文件上传失败"追踪问题
2. 统计500错误的出现频率和错误类型分布
3. 对常见错误类型（如权限问题）进行优化提示
