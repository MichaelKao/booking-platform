package com.booking.platform.entity.system;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.ReferralStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 租戶推薦記錄 Entity
 *
 * <p>記錄店家之間的推薦關係
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "tenant_referrals",
        indexes = {
                @Index(name = "idx_referrals_referrer", columnList = "referrer_tenant_id"),
                @Index(name = "idx_referrals_referee", columnList = "referee_tenant_id"),
                @Index(name = "idx_referrals_code", columnList = "referral_code"),
                @Index(name = "idx_referrals_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantReferral extends BaseEntity {

    /**
     * 推薦人租戶 ID
     */
    @Column(name = "referrer_tenant_id", nullable = false, length = 36)
    private String referrerTenantId;

    /**
     * 被推薦人租戶 ID
     */
    @Column(name = "referee_tenant_id", nullable = false, length = 36)
    private String refereeTenantId;

    /**
     * 使用的推薦碼
     */
    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    /**
     * 推薦狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    /**
     * 推薦人獲得的獎勵點數
     */
    @Column(name = "referrer_bonus_points")
    @Builder.Default
    private Integer referrerBonusPoints = 500;

    /**
     * 被推薦人獲得的獎勵點數
     */
    @Column(name = "referee_bonus_points")
    @Builder.Default
    private Integer refereeBonusPoints = 500;

    /**
     * 獎勵發放時間
     */
    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 完成推薦，發放獎勵
     */
    public void complete() {
        this.status = ReferralStatus.COMPLETED;
        this.rewardedAt = LocalDateTime.now();
    }

    /**
     * 設為過期
     */
    public void expire() {
        this.status = ReferralStatus.EXPIRED;
    }
}
