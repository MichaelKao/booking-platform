package com.booking.platform.entity.system;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.TopUpStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 點數儲值申請 Entity
 *
 * <p>記錄店家的點數儲值申請
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "point_topups",
        indexes = {
                @Index(name = "idx_point_topups_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_point_topups_status", columnList = "status, deleted_at"),
                @Index(name = "idx_point_topups_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTopUp extends BaseEntity {

    /**
     * 申請點數
     */
    @Column(name = "points", nullable = false)
    private Integer points;

    /**
     * 金額（新台幣）
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 申請狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TopUpStatus status;

    /**
     * 付款方式說明
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * 付款帳號後五碼
     */
    @Column(name = "payment_account", length = 5)
    private String paymentAccount;

    /**
     * 付款證明圖片 URL
     */
    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    /**
     * 申請備註（店家填寫）
     */
    @Column(name = "request_note", length = 500)
    private String requestNote;

    /**
     * 審核備註（管理員填寫）
     */
    @Column(name = "review_note", length = 500)
    private String reviewNote;

    /**
     * 審核人 ID
     */
    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    /**
     * 審核時間
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 儲值前餘額
     */
    @Column(name = "balance_before")
    private Integer balanceBefore;

    /**
     * 儲值後餘額
     */
    @Column(name = "balance_after")
    private Integer balanceAfter;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 審核通過
     */
    public void approve(String reviewerId, Integer balanceBefore, Integer balanceAfter, String note) {
        this.status = TopUpStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.reviewNote = note;
    }

    /**
     * 審核駁回
     */
    public void reject(String reviewerId, String reason) {
        this.status = TopUpStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = reason;
    }

    /**
     * 取消申請
     */
    public void cancel() {
        this.status = TopUpStatus.CANCELLED;
    }

    /**
     * 是否可審核
     */
    public boolean canReview() {
        return this.status == TopUpStatus.PENDING;
    }
}
