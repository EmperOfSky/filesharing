package com.filesharing.exception;

import com.filesharing.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.NestedServletException;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * 保留 ResponseStatusException 的原始状态码，避免被 RuntimeException 兜底为 500。
         */
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponse<String>> handleResponseStatusException(ResponseStatusException e) {
                HttpStatus status = HttpStatus.resolve(e.getStatus().value());
                if (status == null) {
                        status = HttpStatus.INTERNAL_SERVER_ERROR;
                }
                String message = e.getReason() != null ? e.getReason() : "请求处理失败";
                log.warn("请求状态异常: status={}, message={}", status.value(), message);
                return ResponseEntity.status(status)
                                .body(ApiResponse.error("REQUEST_ERROR", message));
        }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<String>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");
        
        log.warn("参数验证异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<String>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        
        log.warn("参数绑定异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("BIND_ERROR", message));
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse("约束验证失败");
        
        log.warn("约束违反异常: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("CONSTRAINT_VIOLATION", message));
    }

        /**
         * 处理不支持的 HTTP 方法，避免被兜底为 500。
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<String>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
                String message = "请求方法不支持";
                if (e.getMethod() != null) {
                        message = "请求方法不支持: " + e.getMethod();
                }
                log.warn("方法不支持异常: {}", message);
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.error("METHOD_NOT_ALLOWED", message));
        }

        /**
         * 处理 multipart 请求异常（比如 boundary 缺失）。
         */
        @ExceptionHandler(MultipartException.class)
        public ResponseEntity<ApiResponse<String>> handleMultipartException(MultipartException e) {
                log.warn("文件上传请求格式异常: {}", e.getMessage());
                return ResponseEntity.badRequest()
                                .body(ApiResponse.error("MULTIPART_ERROR", "上传请求格式错误，请重试"));
        }

        /**
         * 处理文件缺失异常。
         */
        @ExceptionHandler(MissingServletRequestPartException.class)
        public ResponseEntity<ApiResponse<String>> handleMissingPartException(MissingServletRequestPartException e) {
                log.warn("缺少上传文件字段: {}", e.getRequestPartName());
                return ResponseEntity.badRequest()
                                .body(ApiResponse.error("FILE_MISSING", "未检测到上传文件"));
        }

        /**
         * 处理上传文件过大异常。
         */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiResponse<String>> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
                log.warn("上传文件超出大小限制: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                                .body(ApiResponse.error("FILE_TOO_LARGE", "文件大小超出限制"));
        }

        /**
         * 处理被包装的 multipart 异常（某些容器会包成 NestedServletException）。
         */
        @ExceptionHandler(NestedServletException.class)
        public ResponseEntity<ApiResponse<String>> handleNestedServletException(NestedServletException e) {
                Throwable cause = e.getCause();
                if (cause instanceof MultipartException) {
                        log.warn("文件上传请求格式异常(包装): {}", cause.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("MULTIPART_ERROR", "上传请求格式错误，请重试"));
                }
                log.error("Servlet包装异常: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException e) {
                String msg = e.getMessage();
                if (msg != null) {
                        String lower = msg.toLowerCase();
                        if (lower.contains("multipart") || lower.contains("boundary") || lower.contains("fileupload")) {
                                log.warn("运行时multipart异常: {}", msg);
                                return ResponseEntity.badRequest()
                                                .body(ApiResponse.error("MULTIPART_ERROR", "上传请求格式错误，请重试"));
                        }
                }
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "服务器内部错误"));
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("UNKNOWN_ERROR", "系统异常，请稍后重试"));
    }
}