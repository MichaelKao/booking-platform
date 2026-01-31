package com.booking.platform.entity.line;

import com.booking.platform.enums.line.LineConfigStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 店家 LINE 設定
 *
 * <p>資料表：tenant_line_configs
 *
 * <p>設計說明：
 * <ul>
 *   <li>每個租戶只有一筆 LINE 設定（一對一關聯）</li>
 *   <li>使用 tenant_id 作為主鍵</li>
 *   <li>Channel Secret 和 Access Token 使用 AES-256-GCM 加密儲存</li>
 * </ul>
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_tlc_status - 狀態查詢</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "tenant_line_configs",
        indexes = {
                @Index(name = "idx_tlc_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLineConfig {

    // ========================================
    // 主鍵（使用 tenant_id）
    // ========================================

    /**
     * 租戶 ID（作為主鍵）
     */
    @Id
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    // ========================================
    // LINE Channel 設定
    // ========================================

    /**
     * LINE Channel ID
     */
    @Column(name = "channel_id", length = 50)
    private String channelId;

    /**
     * LINE Channel Secret（加密儲存）
     */
    @Column(name = "channel_secret_encrypted", length = 500)
    private String channelSecretEncrypted;

    /**
     * LINE Channel Access Token（加密儲存）
     */
    @Column(name = "channel_access_token_encrypted", length = 1000)
    private String channelAccessTokenEncrypted;

    // ========================================
    // Webhook 設定
    // ========================================

    /**
     * Webhook URL（供店家確認用）
     */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /**
     * Webhook 是否已驗證
     */
    @Column(name = "webhook_verified", nullable = false)
    @Builder.Default
    private Boolean webhookVerified = false;

    /**
     * 最後驗證時間
     */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    // ========================================
    // 狀態
    // ========================================

    /**
     * 設定狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LineConfigStatus status = LineConfigStatus.PENDING;

    // ========================================
    // 訊息設定
    // ========================================

    /**
     * 歡迎訊息
     */
    @Column(name = "welcome_message", length = 1000)
    @Builder.Default
    private String welcomeMessage = "歡迎加入！請點選下方選單開始預約服務。";

    /**
     * 預設回覆訊息
     */
    @Column(name = "default_reply", length = 1000)
    @Builder.Default
    private String defaultReply = "抱歉，我不太理解您的意思。請點選下方選單或輸入「預約」開始預約服務。";

    /**
     * 是否啟用自動回覆
     */
    @Column(name = "auto_reply_enabled", nullable = false)
    @Builder.Default
    private Boolean autoReplyEnabled = true;

    /**
     * 是否啟用預約功能
     */
    @Column(name = "booking_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingEnabled = true;

    // ========================================
    // 額度管理
    // ========================================

    /**
     * 本月推送訊息數量
     */
    @Column(name = "monthly_push_count")
    @Builder.Default
    private Integer monthlyPushCount = 0;

    /**
     * 推送額度重置日期
     */
    @Column(name = "push_count_reset_at")
    private LocalDateTime pushCountResetAt;

    // ========================================
    // 審計欄位
    // ========================================

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // 生命週期回調
    // ========================================

    /**
     * 新增前自動設定建立時間
     */
    @PrePersist
    protected void onCreate() {
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
     * 檢查設定是否完整且可用
     *
     * @return true 表示可用
     */
    public boolean isConfigured() {
        return this.channelId != null
                && this.channelSecretEncrypted != null
                && this.channelAccessTokenEncrypted != null
                && LineConfigStatus.ACTIVE.equals(this.status);
    }

    /**
     * 標記為已驗證
     */
    public void markAsVerified() {
        this.webhookVerified = true;
        this.lastVerifiedAt = LocalDateTime.now();
        this.status = LineConfigStatus.ACTIVE;
    }

    /**
     * 標記為驗證失敗
     */
    public void markAsInvalid() {
        this.webhookVerified = false;
        this.status = LineConfigStatus.INVALID;
    }

    /**
     * 停用 LINE 設定
     */
    public void deactivate() {
        this.status = LineConfigStatus.INACTIVE;
    }

    /**
     * 啟用 LINE 設定
     */
    public void activate() {
        this.status = LineConfigStatus.ACTIVE;
    }

    /**
     * 增加推送計數
     */
    public void incrementPushCount() {
        this.monthlyPushCount++;
    }

    /**
     * 重置月度推送計數
     */
    public void resetMonthlyPushCount() {
        this.monthlyPushCount = 0;
        this.pushCountResetAt = LocalDateTime.now();
    }
}
