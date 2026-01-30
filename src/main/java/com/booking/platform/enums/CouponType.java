package com.booking.platform.enums;

/**
 * 票券類型列舉
 *
 * @author Developer
 * @since 1.0.0
 */
public enum CouponType {

    /**
     * 折價券（固定金額）
     */
    DISCOUNT_AMOUNT,

    /**
     * 折扣券（百分比）
     */
    DISCOUNT_PERCENT,

    /**
     * 兌換券
     */
    GIFT,

    /**
     * 加價購券
     */
    ADDON
}
