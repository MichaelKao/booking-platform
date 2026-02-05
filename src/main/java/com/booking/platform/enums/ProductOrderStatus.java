package com.booking.platform.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 商品訂單狀態
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ProductOrderStatus {

    /**
     * 待確認
     */
    PENDING("待確認"),

    /**
     * 已確認
     */
    CONFIRMED("已確認"),

    /**
     * 已完成（已取貨）
     */
    COMPLETED("已完成"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String description;
}
