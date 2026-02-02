package com.booking.platform.enums;

/**
 * 支付狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum PaymentStatus {

    /**
     * 待付款
     */
    PENDING("待付款"),

    /**
     * 處理中
     */
    PROCESSING("處理中"),

    /**
     * 付款成功
     */
    SUCCESS("付款成功"),

    /**
     * 付款失敗
     */
    FAILED("付款失敗"),

    /**
     * 已退款
     */
    REFUNDED("已退款"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
