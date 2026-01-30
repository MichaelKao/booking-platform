package com.booking.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 統一 API 回應格式
 *
 * <p>所有 API 都使用此類別包裝回應
 *
 * <p>範例：
 * <pre>{@code
 * {
 *     "success": true,
 *     "code": "SUCCESS",
 *     "message": "操作成功",
 *     "data": { ... }
 * }
 * }</pre>
 *
 * @param <T> 回應資料類型
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // ========================================
    // 欄位
    // ========================================

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 回應代碼
     */
    private String code;

    /**
     * 回應訊息
     */
    private String message;

    /**
     * 回應資料
     */
    private T data;

    // ========================================
    // 成功回應
    // ========================================

    /**
     * 成功回應（無資料）
     */
    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .build();
    }

    /**
     * 成功回應（帶資料）
     *
     * @param data 回應資料
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 成功回應（自訂訊息）
     *
     * @param message 回應訊息
     * @param data 回應資料
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    // ========================================
    // 錯誤回應
    // ========================================

    /**
     * 錯誤回應
     *
     * @param code 錯誤代碼
     * @param message 錯誤訊息
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 錯誤回應（帶資料）
     *
     * @param code 錯誤代碼
     * @param message 錯誤訊息
     * @param data 錯誤詳情
     */
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
