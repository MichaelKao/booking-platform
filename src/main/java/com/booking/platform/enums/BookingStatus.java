package com.booking.platform.enums;

/**
 * 預約狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum BookingStatus {

    /**
     * 待確認（店家需審核）
     */
    PENDING,

    /**
     * 已確認
     */
    CONFIRMED,

    /**
     * 進行中
     */
    IN_PROGRESS,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 已取消（顧客取消）
     */
    CANCELLED,

    /**
     * 爽約（顧客未到）
     */
    NO_SHOW
}
