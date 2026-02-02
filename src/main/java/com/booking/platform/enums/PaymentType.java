package com.booking.platform.enums;

/**
 * 支付方式
 *
 * @author Developer
 * @since 1.0.0
 */
public enum PaymentType {

    /**
     * 信用卡
     */
    CREDIT_CARD("信用卡"),

    /**
     * ATM 轉帳
     */
    ATM("ATM轉帳"),

    /**
     * 超商代碼
     */
    CVS("超商代碼"),

    /**
     * 超商條碼
     */
    BARCODE("超商條碼"),

    /**
     * 網路 ATM
     */
    WEB_ATM("網路ATM"),

    /**
     * LINE Pay
     */
    LINE_PAY("LINE Pay"),

    /**
     * Apple Pay
     */
    APPLE_PAY("Apple Pay");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
