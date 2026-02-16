package com.booking.platform.enums;

/**
 * 推薦狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum ReferralStatus {

    /**
     * 待完成（新租戶已註冊但尚未完成條件）
     */
    PENDING,

    /**
     * 已完成（雙方已發放獎勵）
     */
    COMPLETED,

    /**
     * 已過期
     */
    EXPIRED
}
