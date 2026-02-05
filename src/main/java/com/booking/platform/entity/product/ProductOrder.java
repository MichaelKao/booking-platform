package com.booking.platform.entity.product;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.ProductOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 商品訂單 Entity
 *
 * <p>記錄 LINE 商品購買訂單
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "product_orders",
        indexes = {
                @Index(name = "idx_product_orders_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_product_orders_customer", columnList = "customer_id, created_at"),
                @Index(name = "idx_product_orders_status", columnList = "tenant_id, status, created_at"),
                @Index(name = "idx_product_orders_order_no", columnList = "order_no", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOrder extends BaseEntity {

    /**
     * 訂單編號
     */
    @Column(name = "order_no", nullable = false, length = 20, unique = true)
    private String orderNo;

    /**
     * 顧客 ID
     */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    /**
     * 顧客名稱
     */
    @Column(name = "customer_name", length = 50)
    private String customerName;

    /**
     * LINE 用戶 ID
     */
    @Column(name = "line_user_id", length = 50)
    private String lineUserId;

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
     * 單價（快照）
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 數量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 總金額
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 訂單狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductOrderStatus status;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * 取貨時間
     */
    @Column(name = "pickup_at")
    private java.time.LocalDateTime pickupAt;

    /**
     * 取消時間
     */
    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    /**
     * 取消原因
     */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 確認訂單
     */
    public void confirm() {
        this.status = ProductOrderStatus.CONFIRMED;
    }

    /**
     * 標記已取貨
     */
    public void pickup() {
        this.status = ProductOrderStatus.COMPLETED;
        this.pickupAt = java.time.LocalDateTime.now();
    }

    /**
     * 取消訂單
     */
    public void cancel(String reason) {
        this.status = ProductOrderStatus.CANCELLED;
        this.cancelledAt = java.time.LocalDateTime.now();
        this.cancelReason = reason;
    }
}
