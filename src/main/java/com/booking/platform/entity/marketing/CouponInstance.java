package com.booking.platform.entity.marketing;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.CouponInstanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 票券實例 Entity
 *
 * <p>記錄發給顧客的實際票券
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "coupon_instances",
        indexes = {
                @Index(name = "idx_coupon_instances_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_coupon_instances_customer", columnList = "tenant_id, customer_id, status, deleted_at"),
                @Index(name = "idx_coupon_instances_coupon", columnList = "tenant_id, coupon_id, deleted_at"),
                @Index(name = "idx_coupon_instances_code", columnList = "tenant_id, code", unique = true),
                @Index(name = "idx_coupon_instances_expires", columnList = "expires_at, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponInstance extends BaseEntity {

    /**
     * 票券定義 ID
     */
    @Column(name = "coupon_id", nullable = false, length = 36)
    private String couponId;

    /**
     * 顧客 ID
     */
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    /**
     * 票券代碼（唯一）
     */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    /**
     * 票券狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponInstanceStatus status;

    /**
     * 發放來源（活動 ID 或 manual）
     */
    @Column(name = "source", length = 50)
    private String source;

    /**
     * 來源說明
     */
    @Column(name = "source_description", length = 200)
    private String sourceDescription;

    /**
     * 有效起始日
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /**
     * 有效結束日
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 使用時間
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 使用於訂單 ID
     */
    @Column(name = "used_order_id", length = 36)
    private String usedOrderId;

    /**
     * 實際折扣金額（核銷時計算）
     */
    @Column(name = "actual_discount_amount", precision = 10, scale = 2)
    private java.math.BigDecimal actualDiscountAmount;

    /**
     * 作廢原因
     */
    @Column(name = "void_reason", length = 200)
    private String voidReason;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = CouponInstanceStatus.UNUSED;
        }
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 使用票券
     */
    public void use(String orderId) {
        this.status = CouponInstanceStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.usedOrderId = orderId;
    }

    /**
     * 作廢票券
     */
    public void voidCoupon(String reason) {
        this.status = CouponInstanceStatus.VOIDED;
        this.voidReason = reason;
    }

    /**
     * 標記為過期
     */
    public void expire() {
        this.status = CouponInstanceStatus.EXPIRED;
    }

    /**
     * 檢查是否可使用
     */
    public boolean canUse() {
        if (status != CouponInstanceStatus.UNUSED) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (expiresAt != null && now.isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * 檢查是否已過期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
