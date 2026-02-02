package com.booking.platform.enums;

/**
 * 行銷推播狀態列舉
 *
 * @author Developer
 * @since 1.0.0
 */
public enum MarketingPushStatus {

    /**
     * 草稿
     */
    DRAFT,

    /**
     * 排程中（等待發送）
     */
    SCHEDULED,

    /**
     * 發送中
     */
    SENDING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 發送失敗
     */
    FAILED
}
