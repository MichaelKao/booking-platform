package com.booking.platform.entity.product;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.InventoryActionType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 庫存異動記錄 Entity
 *
 * <p>記錄每次庫存調整的歷史紀錄
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "inventory_logs",
        indexes = {
                @Index(name = "idx_inventory_logs_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_inventory_logs_product", columnList = "product_id, created_at"),
                @Index(name = "idx_inventory_logs_created", columnList = "tenant_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog extends BaseEntity {

    /**
     * 商品 ID
     */
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /**
     * 商品名稱（快照）
     */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /**
     * 異動類型
     */
    @Column(name = "action_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InventoryActionType actionType;

    /**
     * 異動數量（正數增加，負數減少）
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 異動前庫存
     */
    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    /**
     * 異動後庫存
     */
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    /**
     * 異動原因
     */
    @Column(name = "reason", length = 200)
    private String reason;

    /**
     * 關聯訂單 ID（如有）
     */
    @Column(name = "order_id", length = 36)
    private String orderId;

    /**
     * 操作者 ID
     */
    @Column(name = "operator_id", length = 36)
    private String operatorId;

    /**
     * 操作者名稱
     */
    @Column(name = "operator_name", length = 50)
    private String operatorName;
}
