package com.booking.platform.enums;

/**
 * 租戶狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum TenantStatus {

    /**
     * 待審核
     */
    PENDING,

    /**
     * 啟用中
     */
    ACTIVE,

    /**
     * 已停用
     */
    SUSPENDED,

    /**
     * 已凍結（由超級管理員凍結）
     */
    FROZEN,

    /**
     * 已註銷
     */
    CANCELLED
}
