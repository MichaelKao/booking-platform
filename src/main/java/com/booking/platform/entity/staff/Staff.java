package com.booking.platform.entity.staff;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.StaffStatus;
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

/**
 * 員工
 *
 * <p>資料表：staffs
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_staffs_tenant_status - 列表查詢</li>
 *   <li>idx_staffs_tenant_deleted - 軟刪除過濾</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "staffs",
        indexes = {
                @Index(name = "idx_staffs_tenant_status", columnList = "tenant_id, status, created_at"),
                @Index(name = "idx_staffs_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff extends BaseEntity {

    // ========================================
    // 基本資料欄位
    // ========================================

    /**
     * 員工姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 暱稱/顯示名稱
     */
    @Column(name = "display_name", length = 50)
    private String displayName;

    /**
     * 員工照片 URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 員工簡介
     */
    @Column(name = "bio", length = 500)
    private String bio;

    // ========================================
    // 聯絡資訊
    // ========================================

    /**
     * 手機號碼
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 電子郵件
     */
    @Column(name = "email", length = 100)
    private String email;

    // ========================================
    // 狀態欄位
    // ========================================

    /**
     * 員工狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    /**
     * 是否可被預約
     */
    @Column(name = "is_bookable", nullable = false)
    @Builder.Default
    private Boolean isBookable = true;

    /**
     * 是否在 LINE 顯示
     */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    /** 同一時段最大同時預約數 */
    @Column(name = "max_concurrent_bookings")
    @Builder.Default
    private Integer maxConcurrentBookings = 1;

    // ========================================
    // 排序
    // ========================================

    /**
     * 排序權重（數字越小排越前面）
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查員工是否可預約
     */
    public boolean isAvailable() {
        return StaffStatus.ACTIVE.equals(this.status)
                && Boolean.TRUE.equals(this.isBookable)
                && !this.isDeleted();
    }

    /**
     * 取得顯示名稱（優先使用 displayName）
     */
    public String getEffectiveDisplayName() {
        return this.displayName != null ? this.displayName : this.name;
    }
}
