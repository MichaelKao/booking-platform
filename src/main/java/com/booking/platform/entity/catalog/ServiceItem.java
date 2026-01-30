package com.booking.platform.entity.catalog;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 服務項目
 *
 * <p>資料表：service_items
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "service_items",
        indexes = {
                @Index(name = "idx_service_items_tenant_status", columnList = "tenant_id, status, sort_order"),
                @Index(name = "idx_service_items_tenant_category", columnList = "tenant_id, category_id"),
                @Index(name = "idx_service_items_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItem extends BaseEntity {

    // ========================================
    // 基本資料欄位
    // ========================================

    /**
     * 服務名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 服務描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 服務圖片 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ========================================
    // 分類
    // ========================================

    /**
     * 分類 ID
     */
    @Column(name = "category_id", length = 36)
    private String categoryId;

    // ========================================
    // 價格與時間
    // ========================================

    /**
     * 服務價格
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 服務時長（分鐘）
     */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /**
     * 預約間隔時間（分鐘）- 服務前後的緩衝時間
     */
    @Column(name = "buffer_time")
    @Builder.Default
    private Integer bufferTime = 0;

    // ========================================
    // 狀態欄位
    // ========================================

    /**
     * 服務狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    /**
     * 是否在 LINE 顯示
     */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    /**
     * 是否需要指定員工
     */
    @Column(name = "requires_staff", nullable = false)
    @Builder.Default
    private Boolean requiresStaff = true;

    // ========================================
    // 排序
    // ========================================

    /**
     * 排序權重
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查服務是否可預約
     */
    public boolean isAvailable() {
        return ServiceStatus.ACTIVE.equals(this.status) && !this.isDeleted();
    }

    /**
     * 取得總服務時間（含緩衝）
     */
    public int getTotalDuration() {
        return this.duration + (this.bufferTime != null ? this.bufferTime : 0);
    }
}
