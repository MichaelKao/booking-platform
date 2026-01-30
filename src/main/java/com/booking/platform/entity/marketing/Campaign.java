package com.booking.platform.entity.marketing;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行銷活動 Entity
 *
 * <p>記錄店家建立的行銷活動
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "campaigns",
        indexes = {
                @Index(name = "idx_campaigns_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_campaigns_status", columnList = "tenant_id, status, deleted_at"),
                @Index(name = "idx_campaigns_type", columnList = "tenant_id, type, deleted_at"),
                @Index(name = "idx_campaigns_dates", columnList = "tenant_id, start_at, end_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign extends BaseEntity {

    /**
     * 活動名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 活動描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 活動類型
     */
    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CampaignType type;

    /**
     * 活動狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    /**
     * 活動開始時間
     */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /**
     * 活動結束時間
     */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /**
     * 活動圖片 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 觸發條件 - 消費門檻金額
     */
    @Column(name = "threshold_amount", precision = 10, scale = 2)
    private BigDecimal thresholdAmount;

    /**
     * 觸發條件 - 久未到店天數
     */
    @Column(name = "recall_days")
    private Integer recallDays;

    /**
     * 關聯的票券定義 ID
     */
    @Column(name = "coupon_id", length = 36)
    private String couponId;

    /**
     * 贈送點數
     */
    @Column(name = "bonus_points")
    private Integer bonusPoints;

    /**
     * 推播訊息內容
     */
    @Column(name = "push_message", length = 1000)
    private String pushMessage;

    /**
     * 是否自動觸發
     */
    @Column(name = "is_auto_trigger")
    private Boolean isAutoTrigger;

    /**
     * 參與人數
     */
    @Column(name = "participant_count")
    private Integer participantCount;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = CampaignStatus.DRAFT;
        }
        if (this.participantCount == null) {
            this.participantCount = 0;
        }
        if (this.isAutoTrigger == null) {
            this.isAutoTrigger = false;
        }
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 發布活動
     */
    public void publish() {
        this.status = CampaignStatus.ACTIVE;
    }

    /**
     * 暫停活動
     */
    public void pause() {
        this.status = CampaignStatus.PAUSED;
    }

    /**
     * 恢復活動
     */
    public void resume() {
        this.status = CampaignStatus.ACTIVE;
    }

    /**
     * 結束活動
     */
    public void end() {
        this.status = CampaignStatus.ENDED;
    }

    /**
     * 取消活動
     */
    public void cancel() {
        this.status = CampaignStatus.CANCELLED;
    }

    /**
     * 增加參與人數
     */
    public void incrementParticipant() {
        this.participantCount = (this.participantCount != null ? this.participantCount : 0) + 1;
    }

    /**
     * 檢查活動是否有效（進行中且在有效期內）
     */
    public boolean isEffective() {
        if (status != CampaignStatus.ACTIVE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }
        return true;
    }
}
