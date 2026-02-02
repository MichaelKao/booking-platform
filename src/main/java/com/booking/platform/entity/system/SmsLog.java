package com.booking.platform.entity.system;

import com.booking.platform.enums.SmsStatus;
import com.booking.platform.enums.SmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SMS 發送記錄
 *
 * <p>資料表：sms_logs
 *
 * <p>記錄所有 SMS 發送記錄，用於追蹤和統計
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "sms_logs",
        indexes = {
                @Index(name = "idx_sms_logs_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_sms_logs_phone", columnList = "phone_number"),
                @Index(name = "idx_sms_logs_status", columnList = "status"),
                @Index(name = "idx_sms_logs_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsLog {

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
    // 發送資訊
    // ========================================

    /**
     * 手機號碼
     */
    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    /**
     * 簡訊內容
     */
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * SMS 類型
     */
    @Column(name = "sms_type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private SmsType smsType;

    /**
     * 發送狀態
     */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SmsStatus status = SmsStatus.PENDING;

    // ========================================
    // 關聯資訊
    // ========================================

    /**
     * 關聯的預約 ID（若為預約相關通知）
     */
    @Column(name = "booking_id", length = 36)
    private String bookingId;

    /**
     * 關聯的顧客 ID
     */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    // ========================================
    // 供應商回應
    // ========================================

    /**
     * 供應商訊息 ID
     */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    /**
     * 供應商回應碼
     */
    @Column(name = "provider_response_code", length = 20)
    private String providerResponseCode;

    /**
     * 供應商回應訊息
     */
    @Column(name = "provider_response_message", length = 500)
    private String providerResponseMessage;

    // ========================================
    // 時間欄位
    // ========================================

    /**
     * 發送時間
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================================
    // 生命週期回調
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 標記發送成功
     */
    public void markSuccess(String providerMessageId) {
        this.status = SmsStatus.SUCCESS;
        this.providerMessageId = providerMessageId;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 標記發送失敗
     */
    public void markFailed(String responseCode, String responseMessage) {
        this.status = SmsStatus.FAILED;
        this.providerResponseCode = responseCode;
        this.providerResponseMessage = responseMessage;
        this.sentAt = LocalDateTime.now();
    }
}
