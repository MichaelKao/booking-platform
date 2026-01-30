package com.booking.platform.entity.system;

import com.booking.platform.enums.FeatureCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 功能定義 Entity
 *
 * <p>定義平台所有可用功能，由超級管理員管理
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "features",
        indexes = {
                @Index(name = "idx_features_code", columnList = "code", unique = true),
                @Index(name = "idx_features_is_active", columnList = "is_active")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature {

    /**
     * 主鍵 ID
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 功能代碼
     */
    @Column(name = "code", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FeatureCode code;

    /**
     * 功能名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 功能描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 是否為免費功能
     */
    @Column(name = "is_free")
    private Boolean isFree;

    /**
     * 每月消耗點數
     */
    @Column(name = "monthly_points")
    private Integer monthlyPoints;

    /**
     * 功能圖示
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * 功能分類
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 排序順序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否啟用（全域）
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 建立時間
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isFree == null) {
            this.isFree = false;
        }
        if (this.monthlyPoints == null) {
            this.monthlyPoints = 0;
        }
    }
}
