package com.booking.platform.common.exception;

import lombok.Getter;

/**
 * 業務例外
 *
 * <p>用於拋出業務邏輯錯誤，例如：
 * <ul>
 *   <li>名稱重複</li>
 *   <li>狀態不正確</li>
 *   <li>配額超限</li>
 * </ul>
 *
 * <p>使用範例：
 * <pre>{@code
 * throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "該時段已被預約");
 * }</pre>
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

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
    public BusinessException(ErrorCode errorCode) {
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
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    /**
     * 使用錯誤代碼、自訂訊息和原因建立例外
     *
     * @param errorCode 錯誤代碼
     * @param message 自訂錯誤訊息
     * @param cause 原因例外
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }
}
