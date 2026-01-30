package com.booking.platform.enums;

/**
 * 功能狀態列舉
 *
 * <p>定義店家功能的啟用狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum FeatureStatus {

    /**
     * 隱藏
     * 店家看不到此功能
     */
    HIDDEN,

    /**
     * 可見
     * 店家可以看到但不可申請
     */
    VISIBLE,

    /**
     * 可申請
     * 店家可以申請開通
     */
    AVAILABLE,

    /**
     * 待審核
     * 店家已申請，等待審核
     */
    PENDING,

    /**
     * 已啟用
     * 功能已開通
     */
    ENABLED,

    /**
     * 已凍結
     * 功能被凍結，暫時無法使用
     */
    SUSPENDED,

    /**
     * 已過期
     * 功能訂閱已過期
     */
    EXPIRED
}
