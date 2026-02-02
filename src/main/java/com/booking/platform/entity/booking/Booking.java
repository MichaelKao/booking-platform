package com.booking.platform.entity.booking;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 預約
 *
 * <p>資料表：bookings
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_bookings_tenant_date - 按日期查詢預約</li>
 *   <li>idx_bookings_tenant_status - 按狀態查詢</li>
 *   <li>idx_bookings_tenant_customer - 按顧客查詢</li>
 *   <li>idx_bookings_tenant_staff - 按員工查詢</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_tenant_date", columnList = "tenant_id, booking_date, start_time"),
                @Index(name = "idx_bookings_tenant_status", columnList = "tenant_id, status, booking_date"),
                @Index(name = "idx_bookings_tenant_customer", columnList = "tenant_id, customer_id"),
                @Index(name = "idx_bookings_tenant_staff", columnList = "tenant_id, staff_id, booking_date"),
                @Index(name = "idx_bookings_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    // ========================================
    // 預約時間
    // ========================================

    /**
     * 預約日期
     */
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    /**
     * 開始時間
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 結束時間
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // ========================================
    // 關聯資料
    // ========================================

    /**
     * 顧客 ID
     */
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    /**
     * 顧客姓名（冗餘，方便查詢）
     */
    @Column(name = "customer_name", length = 50)
    private String customerName;

    /**
     * 顧客電話（冗餘）
     */
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /**
     * 員工 ID
     */
    @Column(name = "staff_id", length = 36)
    private String staffId;

    /**
     * 員工姓名（冗餘）
     */
    @Column(name = "staff_name", length = 50)
    private String staffName;

    /**
     * 服務項目 ID
     */
    @Column(name = "service_id", nullable = false, length = 36)
    private String serviceId;

    /**
     * 服務名稱（冗餘）
     */
    @Column(name = "service_name", length = 100)
    private String serviceName;

    // ========================================
    // 價格資訊
    // ========================================

    /**
     * 服務價格
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 服務時長（分鐘）
     */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    // ========================================
    // 狀態欄位
    // ========================================

    /**
     * 預約狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // ========================================
    // 備註
    // ========================================

    /**
     * 顧客備註
     */
    @Column(name = "customer_note", length = 500)
    private String customerNote;

    /**
     * 店家備註（內部）
     */
    @Column(name = "internal_note", length = 500)
    private String internalNote;

    // ========================================
    // 取消資訊
    // ========================================

    /**
     * 取消原因
     */
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    /**
     * 取消時間
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ========================================
    // 來源追蹤
    // ========================================

    /**
     * 預約來源（LINE, WEB, PHONE, WALK_IN）
     */
    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "LINE";

    // ========================================
    // 提醒與取消相關
    // ========================================

    /**
     * 是否已發送提醒
     */
    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    /**
     * 提醒發送時間
     */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    /**
     * 自助取消 Token（用於公開取消連結）
     */
    @Column(name = "cancel_token", length = 36)
    private String cancelToken;

    // ========================================
    // 修改相關
    // ========================================

    /**
     * 給顧客的店家備註
     */
    @Column(name = "store_note_to_customer", length = 500)
    private String storeNoteToCustomer;

    /**
     * 最後修改時間
     */
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    /**
     * 最後修改者
     */
    @Column(name = "last_modified_by", length = 36)
    private String lastModifiedBy;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 取得預約開始時間
     */
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(this.bookingDate, this.startTime);
    }

    /**
     * 取得預約結束時間
     */
    public LocalDateTime getEndDateTime() {
        return LocalDateTime.of(this.bookingDate, this.endTime);
    }

    /**
     * 確認預約
     */
    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * 開始服務
     */
    public void startService() {
        this.status = BookingStatus.IN_PROGRESS;
    }

    /**
     * 完成服務
     */
    public void complete() {
        this.status = BookingStatus.COMPLETED;
    }

    /**
     * 取消預約
     */
    public void cancel(String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 標記爽約
     */
    public void markNoShow() {
        this.status = BookingStatus.NO_SHOW;
    }

    /**
     * 檢查是否可取消
     */
    public boolean isCancellable() {
        return BookingStatus.PENDING.equals(this.status)
                || BookingStatus.CONFIRMED.equals(this.status);
    }

    /**
     * 檢查是否可修改
     */
    public boolean isModifiable() {
        return BookingStatus.PENDING.equals(this.status)
                || BookingStatus.CONFIRMED.equals(this.status);
    }

    /**
     * 標記已發送提醒
     */
    public void markReminderSent() {
        this.reminderSent = true;
        this.reminderSentAt = LocalDateTime.now();
    }
}
