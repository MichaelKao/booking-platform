package com.booking.platform.entity.marketing;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.MarketingPushStatus;
import com.booking.platform.enums.MarketingPushTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 行銷推播 Entity
 *
 * <p>記錄店家建立的 LINE 行銷推播
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "marketing_pushes",
        indexes = {
                @Index(name = "idx_marketing_pushes_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_marketing_pushes_status", columnList = "tenant_id, status, deleted_at"),
                @Index(name = "idx_marketing_pushes_scheduled", columnList = "tenant_id, status, scheduled_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingPush extends BaseEntity {

    /**
     * 推播標題
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 推播內容
     */
    @Column(name = "content", length = 2000)
    private String content;

    /**
     * 推播圖片 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 目標類型
     */
    @Column(name = "target_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MarketingPushTargetType targetType;

    /**
     * 目標值（會員等級 ID 或標籤名稱，依 targetType 決定）
     */
    @Column(name = "target_value", length = 200)
    private String targetValue;

    /**
     * 自訂名單（LINE User ID 列表，JSON 格式）
     */
    @Column(name = "custom_targets", columnDefinition = "TEXT")
    private String customTargets;

    /**
     * 預計發送人數
     */
    @Column(name = "estimated_count")
    private Integer estimatedCount;

    /**
     * 推播狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MarketingPushStatus status = MarketingPushStatus.DRAFT;

    /**
     * 排程發送時間（null 表示立即發送）
     */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * 實際發送時間
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 發送完成時間
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 成功發送數量
     */
    @Column(name = "sent_count")
    @Builder.Default
    private Integer sentCount = 0;

    /**
     * 失敗數量
     */
    @Column(name = "failed_count")
    @Builder.Default
    private Integer failedCount = 0;

    /**
     * 錯誤訊息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = MarketingPushStatus.DRAFT;
        }
        if (this.sentCount == null) {
            this.sentCount = 0;
        }
        if (this.failedCount == null) {
            this.failedCount = 0;
        }
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 排程推播
     */
    public void schedule(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
        this.status = MarketingPushStatus.SCHEDULED;
    }

    /**
     * 開始發送
     */
    public void startSending() {
        this.status = MarketingPushStatus.SENDING;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 完成發送
     */
    public void complete(int successCount, int failCount) {
        this.status = MarketingPushStatus.COMPLETED;
        this.sentCount = successCount;
        this.failedCount = failCount;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 標記失敗
     */
    public void markFailed(String errorMessage) {
        this.status = MarketingPushStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 取消推播
     */
    public void cancel() {
        this.status = MarketingPushStatus.CANCELLED;
    }

    /**
     * 檢查是否可取消
     */
    public boolean isCancellable() {
        return MarketingPushStatus.DRAFT.equals(this.status)
                || MarketingPushStatus.SCHEDULED.equals(this.status);
    }

    /**
     * 檢查是否可發送
     */
    public boolean isSendable() {
        return MarketingPushStatus.DRAFT.equals(this.status)
                || MarketingPushStatus.SCHEDULED.equals(this.status);
    }

    /**
     * 增加發送計數
     */
    public void incrementSentCount() {
        this.sentCount = (this.sentCount != null ? this.sentCount : 0) + 1;
    }

    /**
     * 增加失敗計數
     */
    public void incrementFailedCount() {
        this.failedCount = (this.failedCount != null ? this.failedCount : 0) + 1;
    }
}
