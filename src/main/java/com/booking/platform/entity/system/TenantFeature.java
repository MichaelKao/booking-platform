package com.booking.platform.entity.system;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.enums.FeatureStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 租戶功能訂閱 Entity
 *
 * <p>記錄每個店家啟用的功能及狀態
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "tenant_features",
        indexes = {
                @Index(name = "idx_tenant_features_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_tenant_features_code", columnList = "tenant_id, feature_code, deleted_at"),
                @Index(name = "idx_tenant_features_status", columnList = "tenant_id, status, deleted_at"),
                @Index(name = "idx_tenant_features_expires", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_feature", columnNames = {"tenant_id", "feature_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeature extends BaseEntity {

    /**
     * 功能代碼
     */
    @Column(name = "feature_code", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FeatureCode featureCode;

    /**
     * 功能狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FeatureStatus status;

    /**
     * 啟用時間
     */
    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    /**
     * 過期時間
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 特殊價格（覆蓋預設點數）
     */
    @Column(name = "custom_monthly_points")
    private Integer customMonthlyPoints;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * 由誰啟用
     */
    @Column(name = "enabled_by", length = 36)
    private String enabledBy;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查功能是否已啟用且有效
     */
    public boolean isEffective() {
        if (status != FeatureStatus.ENABLED) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }

    /**
     * 啟用功能
     */
    public void enable(String operatorId, LocalDateTime expiresAt) {
        this.status = FeatureStatus.ENABLED;
        this.enabledAt = LocalDateTime.now();
        this.enabledBy = operatorId;
        this.expiresAt = expiresAt;
    }

    /**
     * 停用功能
     */
    public void disable() {
        this.status = FeatureStatus.AVAILABLE;
        this.enabledAt = null;
        this.expiresAt = null;
    }

    /**
     * 凍結功能
     */
    public void suspend() {
        this.status = FeatureStatus.SUSPENDED;
    }

    /**
     * 解凍功能
     */
    public void unsuspend() {
        this.status = FeatureStatus.ENABLED;
    }
}
