package com.booking.platform.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 基礎 Entity
 *
 * <p>所有業務 Entity 都要繼承此類別，自動擁有：
 * <ul>
 *   <li>id - UUID 主鍵</li>
 *   <li>tenantId - 多租戶識別</li>
 *   <li>createdAt, createdBy - 建立資訊</li>
 *   <li>updatedAt, updatedBy - 更新資訊</li>
 *   <li>deletedAt - 軟刪除時間</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    // ========================================
    // 主鍵
    // ========================================

    /**
     * 主鍵（UUID）
     */
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    // ========================================
    // 多租戶
    // ========================================

    /**
     * 租戶 ID
     */
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    // ========================================
    // 審計欄位
    // ========================================

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 建立者
     */
    @Column(name = "created_by", length = 36)
    private String createdBy;

    /**
     * 更新時間
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 更新者
     */
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    // ========================================
    // 軟刪除
    // ========================================

    /**
     * 刪除時間（null 表示未刪除）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // 生命週期回調
    // ========================================

    /**
     * 新增前自動設定 ID 和建立時間
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新前自動設定更新時間
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查是否已刪除
     *
     * @return true 表示已刪除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 執行軟刪除
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 恢復刪除
     */
    public void restore() {
        this.deletedAt = null;
    }
}
