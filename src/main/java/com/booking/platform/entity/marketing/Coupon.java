package com.booking.platform.entity.marketing;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.CouponType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票券定義 Entity
 *
 * <p>定義票券的類型、面額、使用條件等
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_coupons_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_coupons_status", columnList = "tenant_id, status, deleted_at"),
                @Index(name = "idx_coupons_type", columnList = "tenant_id, type, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @Version
    private Long version;

    /**
     * 票券名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 票券描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 票券類型
     */
    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CouponType type;

    /**
     * 票券狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    /**
     * 折扣金額（DISCOUNT_AMOUNT 時使用）
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * 折扣比例（DISCOUNT_PERCENT 時使用，0.1 = 9折）
     */
    @Column(name = "discount_percent", precision = 5, scale = 4)
    private BigDecimal discountPercent;

    /**
     * 最低消費金額
     */
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    /**
     * 最高折抵金額
     */
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * 兌換項目（GIFT 時使用）
     */
    @Column(name = "gift_item", length = 200)
    private String giftItem;

    /**
     * 發行數量（null 表示不限）
     */
    @Column(name = "total_quantity")
    private Integer totalQuantity;

    /**
     * 已發出數量
     */
    @Column(name = "issued_quantity")
    private Integer issuedQuantity;

    /**
     * 已使用數量
     */
    @Column(name = "used_quantity")
    private Integer usedQuantity;

    /**
     * 每人限領數量
     */
    @Column(name = "limit_per_customer")
    private Integer limitPerCustomer;

    /**
     * 有效期天數（從發放日起算）
     */
    @Column(name = "valid_days")
    private Integer validDays;

    /**
     * 固定有效起始日
     */
    @Column(name = "valid_start_at")
    private LocalDateTime validStartAt;

    /**
     * 固定有效結束日
     */
    @Column(name = "valid_end_at")
    private LocalDateTime validEndAt;

    /**
     * 適用服務 ID（逗號分隔，null 表示全部適用）
     */
    @Column(name = "applicable_services", length = 1000)
    private String applicableServices;

    /**
     * 票券圖片 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 使用說明
     */
    @Column(name = "terms", length = 1000)
    private String terms;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = CouponStatus.DRAFT;
        }
        if (this.issuedQuantity == null) {
            this.issuedQuantity = 0;
        }
        if (this.usedQuantity == null) {
            this.usedQuantity = 0;
        }
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 發布票券
     */
    public void publish() {
        this.status = CouponStatus.PUBLISHED;
    }

    /**
     * 暫停票券
     */
    public void pause() {
        this.status = CouponStatus.PAUSED;
    }

    /**
     * 恢復票券
     */
    public void resume() {
        this.status = CouponStatus.PUBLISHED;
    }

    /**
     * 結束票券
     */
    public void end() {
        this.status = CouponStatus.ENDED;
    }

    /**
     * 檢查是否可發放
     */
    public boolean canIssue() {
        if (status != CouponStatus.PUBLISHED) {
            return false;
        }
        if (totalQuantity != null && issuedQuantity >= totalQuantity) {
            return false;
        }
        return true;
    }

    /**
     * 發放票券（增加已發出數量）
     */
    public void issue() {
        this.issuedQuantity = (this.issuedQuantity != null ? this.issuedQuantity : 0) + 1;
    }

    /**
     * 使用票券（增加已使用數量）
     */
    public void use() {
        this.usedQuantity = (this.usedQuantity != null ? this.usedQuantity : 0) + 1;
    }

    /**
     * 取得剩餘可發放數量
     */
    public Integer getRemainingQuantity() {
        if (totalQuantity == null) {
            return null; // 不限
        }
        return totalQuantity - (issuedQuantity != null ? issuedQuantity : 0);
    }
}
