package com.booking.platform.enums;

/**
 * SMS 發送狀態
 *
 * @author Developer
 * @since 1.0.0
 */
public enum SmsStatus {

    /**
     * 待發送
     */
    PENDING("待發送"),

    /**
     * 發送中
     */
    SENDING("發送中"),

    /**
     * 發送成功
     */
    SUCCESS("發送成功"),

    /**
     * 發送失敗
     */
    FAILED("發送失敗"),

    /**
     * 額度不足
     */
    QUOTA_EXCEEDED("額度不足");

    private final String description;

    SmsStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
