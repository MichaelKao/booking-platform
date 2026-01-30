package com.booking.platform.entity.customer;

import com.booking.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 會員等級
 *
 * <p>資料表：membership_levels
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "membership_levels",
        indexes = {
                @Index(name = "idx_membership_levels_tenant", columnList = "tenant_id, sort_order"),
                @Index(name = "idx_membership_levels_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipLevel extends BaseEntity {

    /**
     * 等級名稱
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 等級描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 等級圖示/顏色代碼
     */
    @Column(name = "badge_color", length = 20)
    private String badgeColor;

    /**
     * 升級門檻（累積消費金額）
     */
    @Column(name = "upgrade_threshold", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal upgradeThreshold = BigDecimal.ZERO;

    /**
     * 折扣比例（例如：0.95 表示 95 折）
     */
    @Column(name = "discount_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountRate = BigDecimal.ONE;

    /**
     * 點數回饋比例（例如：0.01 表示消費 1% 回饋點數）
     */
    @Column(name = "point_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal pointRate = BigDecimal.ZERO;

    /**
     * 是否為預設等級（新會員自動獲得）
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 是否啟用
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 排序權重（等級由低到高）
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 計算折扣後金額
     */
    public BigDecimal calculateDiscountedPrice(BigDecimal originalPrice) {
        return originalPrice.multiply(this.discountRate);
    }

    /**
     * 計算可獲得的點數
     */
    public int calculatePoints(BigDecimal amount) {
        return amount.multiply(this.pointRate).intValue();
    }
}
