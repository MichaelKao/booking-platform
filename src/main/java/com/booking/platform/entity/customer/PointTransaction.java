package com.booking.platform.entity.customer;

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
 * 點數交易紀錄
 *
 * <p>資料表：point_transactions
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "point_transactions",
        indexes = {
                @Index(name = "idx_point_transactions_tenant_customer", columnList = "tenant_id, customer_id, created_at"),
                @Index(name = "idx_point_transactions_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction extends BaseEntity {

    /**
     * 顧客 ID
     */
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    /**
     * 交易類型（EARN:獲得, REDEEM:兌換, EXPIRE:過期, ADJUST:調整）
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 點數變動量（正數為增加，負數為減少）
     */
    @Column(name = "points", nullable = false)
    private Integer points;

    /**
     * 交易後餘額
     */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /**
     * 關聯訂單 ID（如果有）
     */
    @Column(name = "order_id", length = 36)
    private String orderId;

    /**
     * 說明
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 過期時間（如果有）
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
