package com.booking.platform.common.exception;

import com.booking.platform.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全域例外處理器
 *
 * <p>統一處理所有例外，返回標準格式的錯誤回應
 *
 * @author Developer
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========================================
    // 業務例外
    // ========================================

    /**
     * 處理業務例外
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("業務例外：{} - {}", ex.getCode(), ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 處理資源不存在例外
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("資源不存在：{} - {}", ex.getCode(), ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    // ========================================
    // 認證授權例外
    // ========================================

    /**
     * 處理認證例外
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException ex) {
        log.warn("認證失敗：{}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.AUTH_UNAUTHORIZED.getCode(),
                ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage()
        );
    }

    /**
     * 處理授權例外
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("權限不足：{}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.AUTH_FORBIDDEN.getCode(),
                ErrorCode.AUTH_FORBIDDEN.getDefaultMessage()
        );
    }

    // ========================================
    // 驗證例外
    // ========================================

    /**
     * 處理參數驗證例外（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("參數驗證失敗：{}", errors);

        return ApiResponse.error(
                ErrorCode.SYS_VALIDATION_ERROR.getCode(),
                "參數驗證失敗",
                errors
        );
    }

    /**
     * 處理約束違反例外（@Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("約束違反：{}", errors);

        return ApiResponse.error(
                ErrorCode.SYS_VALIDATION_ERROR.getCode(),
                "參數驗證失敗",
                errors
        );
    }

    // ========================================
    // 請求例外
    // ========================================

    /**
     * 處理缺少請求參數例外
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParameterException(
            MissingServletRequestParameterException ex
    ) {
        log.warn("缺少請求參數：{}", ex.getParameterName());
        return ApiResponse.error(
                ErrorCode.SYS_PARAM_ERROR.getCode(),
                "缺少必要參數：" + ex.getParameterName()
        );
    }

    /**
     * 處理參數類型轉換例外
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex
    ) {
        log.warn("參數類型錯誤：{}", ex.getName());
        return ApiResponse.error(
                ErrorCode.SYS_PARAM_ERROR.getCode(),
                "參數類型錯誤：" + ex.getName()
        );
    }

    /**
     * 處理請求體解析例外
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        log.warn("請求體解析失敗：{}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.SYS_PARAM_ERROR.getCode(),
                "請求體格式錯誤"
        );
    }

    /**
     * 處理不支援的請求方法例外
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex
    ) {
        log.warn("不支援的請求方法：{}", ex.getMethod());
        return ApiResponse.error(
                ErrorCode.SYS_METHOD_NOT_ALLOWED.getCode(),
                "不支援的請求方法：" + ex.getMethod()
        );
    }

    /**
     * 處理不支援的媒體類型例外
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex
    ) {
        log.warn("不支援的媒體類型：{}", ex.getContentType());
        return ApiResponse.error(
                ErrorCode.SYS_MEDIA_TYPE_NOT_SUPPORTED.getCode(),
                "不支援的媒體類型"
        );
    }

    /**
     * 處理找不到處理器例外
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("找不到處理器：{} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ApiResponse.error(
                ErrorCode.SYS_NOT_FOUND.getCode(),
                "找不到請求的資源"
        );
    }

    // ========================================
    // 未預期例外
    // ========================================

    /**
     * 處理所有未捕獲的例外
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("系統錯誤", ex);
        return ApiResponse.error(
                ErrorCode.SYS_ERROR.getCode(),
                "系統錯誤，請稍後再試"
        );
    }
}
