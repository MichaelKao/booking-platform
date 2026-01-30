package com.booking.platform.common.exception;

import lombok.Getter;

/**
 * 資源不存在例外
 *
 * <p>當查詢的資源不存在時拋出，例如：
 * <ul>
 *   <li>預約不存在</li>
 *   <li>顧客不存在</li>
 *   <li>服務項目不存在</li>
 * </ul>
 *
 * <p>使用範例：
 * <pre>{@code
 * throw new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND, "找不到預約 ID：" + id);
 * }</pre>
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    // ========================================
    // 欄位
    // ========================================

    /**
     * 錯誤代碼
     */
    private final ErrorCode errorCode;

    /**
     * 錯誤代碼字串
     */
    private final String code;

    // ========================================
    // 建構子
    // ========================================

    /**
     * 使用錯誤代碼建立例外
     *
     * @param errorCode 錯誤代碼
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    /**
     * 使用錯誤代碼和自訂訊息建立例外
     *
     * @param errorCode 錯誤代碼
     * @param message 自訂錯誤訊息
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }
}
