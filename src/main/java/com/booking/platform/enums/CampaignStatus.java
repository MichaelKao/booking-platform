package com.booking.platform.enums;

/**
 * 行銷活動狀態列舉
 *
 * @author Developer
 * @since 1.0.0
 */
public enum CampaignStatus {

    /**
     * 草稿
     */
    DRAFT,

    /**
     * 排程中（等待開始）
     */
    SCHEDULED,

    /**
     * 進行中
     */
    ACTIVE,

    /**
     * 已暫停
     */
    PAUSED,

    /**
     * 已結束
     */
    ENDED,

    /**
     * 已取消
     */
    CANCELLED
}
