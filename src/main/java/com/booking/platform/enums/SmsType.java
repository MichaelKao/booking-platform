package com.booking.platform.enums;

/**
 * SMS 類型
 *
 * @author Developer
 * @since 1.0.0
 */
public enum SmsType {

    /**
     * 預約確認
     */
    BOOKING_CONFIRMATION("預約確認"),

    /**
     * 預約提醒
     */
    BOOKING_REMINDER("預約提醒"),

    /**
     * 預約取消
     */
    BOOKING_CANCELLED("預約取消"),

    /**
     * 行銷推播
     */
    MARKETING("行銷推播"),

    /**
     * 驗證碼
     */
    VERIFICATION("驗證碼"),

    /**
     * 其他
     */
    OTHER("其他");

    private final String description;

    SmsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
