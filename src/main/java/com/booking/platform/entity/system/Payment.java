package com.booking.platform.entity.system;

import com.booking.platform.enums.PaymentStatus;
import com.booking.platform.enums.PaymentType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付記錄
 *
 * <p>資料表：payments
 *
 * <p>記錄所有支付交易，支援 ECPay 金流
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_payments_merchant_trade_no", columnList = "merchant_trade_no", unique = true),
                @Index(name = "idx_payments_ecpay_trade_no", columnList = "ecpay_trade_no"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

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
    // 租戶資訊
    // ========================================

    /**
     * 租戶 ID
     */
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    // ========================================
    // 交易資訊
    // ========================================

    /**
     * 商店訂單編號（唯一）
     */
    @Column(name = "merchant_trade_no", length = 50, nullable = false, unique = true)
    private String merchantTradeNo;

    /**
     * 交易金額
     */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * 支付方式
     */
    @Column(name = "payment_type", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    /**
     * 支付狀態
     */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * 交易描述
     */
    @Column(name = "description", length = 200)
    private String description;

    // ========================================
    // 關聯資訊
    // ========================================

    /**
     * 關聯的預約 ID
     */
    @Column(name = "booking_id", length = 36)
    private String bookingId;

    /**
     * 關聯的顧客 ID
     */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    /**
     * 關聯的點數儲值 ID
     */
    @Column(name = "topup_id", length = 36)
    private String topupId;

    // ========================================
    // ECPay 回應
    // ========================================

    /**
     * ECPay 交易編號
     */
    @Column(name = "ecpay_trade_no", length = 50)
    private String ecpayTradeNo;

    /**
     * ECPay 回應碼
     */
    @Column(name = "ecpay_response_code", length = 10)
    private String ecpayResponseCode;

    /**
     * ECPay 回應訊息
     */
    @Column(name = "ecpay_response_message", length = 200)
    private String ecpayResponseMessage;

    /**
     * 付款銀行代碼
     */
    @Column(name = "payment_bank_code", length = 20)
    private String paymentBankCode;

    /**
     * 信用卡末四碼
     */
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    // ========================================
    // 時間欄位
    // ========================================

    /**
     * 付款時間
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 退款時間
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

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

    /**
     * 軟刪除時間
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========================================
    // 生命週期回調
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 標記付款成功
     */
    public void markSuccess(String ecpayTradeNo, PaymentType paymentType) {
        this.status = PaymentStatus.SUCCESS;
        this.ecpayTradeNo = ecpayTradeNo;
        this.paymentType = paymentType;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 標記付款失敗
     */
    public void markFailed(String responseCode, String responseMessage) {
        this.status = PaymentStatus.FAILED;
        this.ecpayResponseCode = responseCode;
        this.ecpayResponseMessage = responseMessage;
    }

    /**
     * 標記已退款
     */
    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 標記已取消
     */
    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }

    /**
     * 是否可退款
     */
    public boolean isRefundable() {
        return PaymentStatus.SUCCESS.equals(this.status);
    }
}
