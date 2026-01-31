package com.booking.platform.entity.line;

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

import java.time.LocalDateTime;

/**
 * LINE 用戶
 *
 * <p>資料表：line_users
 *
 * <p>設計說明：
 * <ul>
 *   <li>記錄通過 LINE 互動的用戶資訊</li>
 *   <li>一個 LINE 用戶在同一租戶下只有一筆記錄</li>
 *   <li>與 Customer 是一對一關聯（通過 customer_id）</li>
 * </ul>
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_lu_tenant_line_user - 租戶 + LINE User ID 查詢（唯一）</li>
 *   <li>idx_lu_tenant_customer - 租戶 + 顧客 ID 查詢</li>
 *   <li>idx_lu_tenant_followed - 租戶 + 追蹤狀態查詢</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "line_users",
        indexes = {
                @Index(name = "idx_lu_tenant_line_user", columnList = "tenant_id, line_user_id", unique = true),
                @Index(name = "idx_lu_tenant_customer", columnList = "tenant_id, customer_id"),
                @Index(name = "idx_lu_tenant_followed", columnList = "tenant_id, is_followed"),
                @Index(name = "idx_lu_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineUser extends BaseEntity {

    // ========================================
    // LINE 資訊
    // ========================================

    /**
     * LINE User ID
     * <p>由 LINE 平台提供的唯一識別碼
     */
    @Column(name = "line_user_id", nullable = false, length = 50)
    private String lineUserId;

    /**
     * LINE 顯示名稱
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * LINE 頭像 URL
     */
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    /**
     * LINE 狀態訊息
     */
    @Column(name = "status_message", length = 500)
    private String statusMessage;

    // ========================================
    // 關聯
    // ========================================

    /**
     * 顧客 ID
     * <p>關聯到 Customer Entity
     */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    // ========================================
    // 追蹤狀態
    // ========================================

    /**
     * 是否追蹤中
     */
    @Column(name = "is_followed", nullable = false)
    @Builder.Default
    private Boolean isFollowed = true;

    /**
     * 首次追蹤時間
     */
    @Column(name = "followed_at")
    private LocalDateTime followedAt;

    /**
     * 最後取消追蹤時間
     */
    @Column(name = "unfollowed_at")
    private LocalDateTime unfollowedAt;

    // ========================================
    // 統計資訊
    // ========================================

    /**
     * 互動次數（訊息往來次數）
     */
    @Column(name = "interaction_count")
    @Builder.Default
    private Integer interactionCount = 0;

    /**
     * 預約次數
     */
    @Column(name = "booking_count")
    @Builder.Default
    private Integer bookingCount = 0;

    /**
     * 最後互動時間
     */
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    // ========================================
    // 語言設定
    // ========================================

    /**
     * 用戶語言
     */
    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "zh-TW";

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 處理追蹤事件
     */
    public void handleFollow() {
        this.isFollowed = true;
        this.followedAt = LocalDateTime.now();
        this.unfollowedAt = null;
    }

    /**
     * 處理取消追蹤事件
     */
    public void handleUnfollow() {
        this.isFollowed = false;
        this.unfollowedAt = LocalDateTime.now();
    }

    /**
     * 增加互動次數
     */
    public void incrementInteractionCount() {
        this.interactionCount++;
        this.lastInteractionAt = LocalDateTime.now();
    }

    /**
     * 增加預約次數
     */
    public void incrementBookingCount() {
        this.bookingCount++;
    }

    /**
     * 更新個人資料
     *
     * @param displayName 顯示名稱
     * @param pictureUrl  頭像 URL
     * @param statusMessage 狀態訊息
     */
    public void updateProfile(String displayName, String pictureUrl, String statusMessage) {
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
        this.statusMessage = statusMessage;
    }

    /**
     * 綁定顧客
     *
     * @param customerId 顧客 ID
     */
    public void bindCustomer(String customerId) {
        this.customerId = customerId;
    }

    /**
     * 檢查是否已綁定顧客
     *
     * @return true 表示已綁定
     */
    public boolean isCustomerBound() {
        return this.customerId != null && !this.customerId.isEmpty();
    }

    /**
     * 檢查是否可以接收訊息
     *
     * @return true 表示可以接收
     */
    public boolean canReceiveMessage() {
        return this.isFollowed && !this.isDeleted();
    }
}
