package com.booking.platform.entity.tenant;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.TenantStatus;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 租戶（店家）
 *
 * <p>資料表：tenants
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_tenants_code - 租戶代碼唯一索引</li>
 *   <li>idx_tenants_status - 狀態查詢</li>
 *   <li>idx_tenants_deleted - 軟刪除過濾</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "tenants",
        indexes = {
                @Index(name = "idx_tenants_code", columnList = "code", unique = true),
                @Index(name = "idx_tenants_status", columnList = "status"),
                @Index(name = "idx_tenants_deleted", columnList = "deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    // ========================================
    // 基本資料欄位
    // ========================================

    /**
     * 租戶代碼（唯一，用於 LINE Webhook URL）
     */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /**
     * 店家名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 店家描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 店家 Logo URL
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // ========================================
    // 聯絡資訊
    // ========================================

    /**
     * 聯絡電話
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 聯絡信箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 店家地址
     */
    @Column(name = "address", length = 200)
    private String address;

    // ========================================
    // 認證欄位
    // ========================================

    /**
     * 登入密碼（BCrypt 加密）
     */
    @Column(name = "password", length = 100)
    private String password;

    /**
     * 密碼重設 Token
     */
    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    /**
     * 密碼重設 Token 過期時間
     */
    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    // ========================================
    // 狀態欄位
    // ========================================

    /**
     * 租戶狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.PENDING;

    /**
     * 是否為測試帳號
     */
    @Column(name = "is_test_account", nullable = false)
    @Builder.Default
    private Boolean isTestAccount = false;

    // ========================================
    // LINE 設定
    // ========================================

    /**
     * LINE Channel ID
     */
    @Column(name = "line_channel_id", length = 50)
    private String lineChannelId;

    /**
     * LINE Channel Secret
     */
    @Column(name = "line_channel_secret", length = 100)
    private String lineChannelSecret;

    /**
     * LINE Channel Access Token
     */
    @Column(name = "line_channel_token", length = 200)
    private String lineChannelToken;

    // ========================================
    // 點數相關
    // ========================================

    /**
     * 點數餘額
     */
    @Column(name = "point_balance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pointBalance = BigDecimal.ZERO;

    // ========================================
    // 配額限制
    // ========================================

    /**
     * 員工數量上限（0 表示無限制）
     */
    @Column(name = "max_staff_count")
    @Builder.Default
    private Integer maxStaffCount = 3;

    /**
     * 每月推送訊息額度
     */
    @Column(name = "monthly_push_quota")
    @Builder.Default
    private Integer monthlyPushQuota = 100;

    /**
     * 本月已使用推送數量
     */
    @Column(name = "monthly_push_used")
    @Builder.Default
    private Integer monthlyPushUsed = 0;

    // ========================================
    // 時間欄位
    // ========================================

    /**
     * 啟用時間
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /**
     * 到期時間
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // ========================================
    // 營業設定
    // ========================================

    /**
     * 營業開始時間
     */
    @Column(name = "business_start_time")
    @Builder.Default
    private LocalTime businessStartTime = LocalTime.of(9, 0);

    /**
     * 營業結束時間
     */
    @Column(name = "business_end_time")
    @Builder.Default
    private LocalTime businessEndTime = LocalTime.of(21, 0);

    /**
     * 預約時段間隔（分鐘）
     */
    @Column(name = "booking_interval")
    @Builder.Default
    private Integer bookingInterval = 30;

    /**
     * 最大預約提前天數
     */
    @Column(name = "max_advance_booking_days")
    @Builder.Default
    private Integer maxAdvanceBookingDays = 30;

    /**
     * 公休日（JSON 格式，例如：[0,6] 表示週日和週六）
     */
    @Column(name = "closed_days", length = 50)
    private String closedDays;

    /**
     * 休息開始時間（午休）
     */
    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    /**
     * 休息結束時間（午休）
     */
    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    // ========================================
    // 通知設定
    // ========================================

    /**
     * 新預約通知
     */
    @Column(name = "notify_new_booking")
    @Builder.Default
    private Boolean notifyNewBooking = true;

    /**
     * 預約提醒通知
     */
    @Column(name = "notify_booking_reminder")
    @Builder.Default
    private Boolean notifyBookingReminder = true;

    /**
     * 取消預約通知
     */
    @Column(name = "notify_booking_cancel")
    @Builder.Default
    private Boolean notifyBookingCancel = false;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查租戶是否可用
     *
     * @return true 表示可用
     */
    public boolean isAvailable() {
        return TenantStatus.ACTIVE.equals(this.status) && !this.isDeleted();
    }

    /**
     * 啟用租戶
     */
    public void activate() {
        this.status = TenantStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    /**
     * 停用租戶
     */
    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    /**
     * 凍結租戶
     */
    public void freeze() {
        this.status = TenantStatus.FROZEN;
    }

    /**
     * 檢查推送額度是否足夠
     *
     * @param count 需要的數量
     * @return true 表示額度足夠
     */
    public boolean hasPushQuota(int count) {
        return (this.monthlyPushQuota - this.monthlyPushUsed) >= count;
    }

    /**
     * 使用推送額度
     *
     * @param count 使用數量
     */
    public void usePushQuota(int count) {
        this.monthlyPushUsed += count;
    }

    /**
     * 重置月度推送計數
     */
    public void resetMonthlyPushUsed() {
        this.monthlyPushUsed = 0;
    }

    /**
     * 增加點數
     *
     * @param amount 增加金額
     */
    public void addPoints(BigDecimal amount) {
        this.pointBalance = this.pointBalance.add(amount);
    }

    /**
     * 扣除點數
     *
     * @param amount 扣除金額
     * @return true 表示扣除成功
     */
    public boolean deductPoints(BigDecimal amount) {
        if (this.pointBalance.compareTo(amount) < 0) {
            return false;
        }
        this.pointBalance = this.pointBalance.subtract(amount);
        return true;
    }
}
