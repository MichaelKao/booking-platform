package com.booking.platform.enums;

/**
 * 儲值申請狀態列舉
 *
 * @author Developer
 * @since 1.0.0
 */
public enum TopUpStatus {

    /**
     * 待審核
     */
    PENDING,

    /**
     * 已通過
     */
    APPROVED,

    /**
     * 已駁回
     */
    REJECTED,

    /**
     * 已取消
     */
    CANCELLED
}
