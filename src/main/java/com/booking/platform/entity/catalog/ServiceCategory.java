package com.booking.platform.entity.catalog;

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

/**
 * 服務分類
 *
 * <p>資料表：service_categories
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "service_categories",
        indexes = {
                @Index(name = "idx_service_categories_tenant", columnList = "tenant_id, sort_order"),
                @Index(name = "idx_service_categories_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory extends BaseEntity {

    /**
     * 分類名稱
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 分類描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 分類圖示 URL
     */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * 是否啟用
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 排序權重
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
