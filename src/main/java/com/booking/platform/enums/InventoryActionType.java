package com.booking.platform.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 庫存異動類型
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum InventoryActionType {

    /**
     * 進貨入庫
     */
    STOCK_IN("進貨入庫"),

    /**
     * 銷售出庫
     */
    SALE_OUT("銷售出庫"),

    /**
     * 手動調整
     */
    ADJUSTMENT("手動調整"),

    /**
     * 盤點調整
     */
    INVENTORY_CHECK("盤點調整"),

    /**
     * 損耗報廢
     */
    DAMAGE("損耗報廢"),

    /**
     * 退貨入庫
     */
    RETURN_IN("退貨入庫"),

    /**
     * 訂單取消（回補庫存）
     */
    ORDER_CANCEL("訂單取消");

    private final String description;
}
