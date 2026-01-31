package com.booking.platform.enums.line;

/**
 * LINE 設定狀態
 *
 * <p>表示店家 LINE Bot 設定的狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum LineConfigStatus {

    /**
     * 待設定
     * <p>尚未填寫 LINE Channel 資訊
     */
    PENDING,

    /**
     * 啟用中
     * <p>已設定且驗證通過，正常運作中
     */
    ACTIVE,

    /**
     * 已停用
     * <p>店家主動停用 LINE Bot 功能
     */
    INACTIVE,

    /**
     * 設定無效
     * <p>Channel Secret 或 Access Token 驗證失敗
     */
    INVALID,

    /**
     * 驗證中
     * <p>正在驗證 LINE 設定
     */
    VERIFYING
}
