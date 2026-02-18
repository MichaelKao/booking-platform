package com.booking.platform.entity.tenant;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Version
    private Long version;

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
    // 推薦機制
    // ========================================

    /**
     * 推薦碼（唯一，8 字元）
     */
    @Column(name = "referral_code", length = 20, unique = true)
    private String referralCode;

    /**
     * 由哪個推薦碼推薦來的
     */
    @Column(name = "referred_by_code", length = 20)
    private String referredByCode;

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
    // 店家點數餘額（用於訂閱功能）
    // ========================================

    /**
     * 點數餘額
     */
    @Column(name = "point_balance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pointBalance = BigDecimal.ZERO;

    // ========================================
    // 顧客點數累積設定
    // ========================================

    /**
     * 是否啟用顧客點數累積
     */
    @Column(name = "point_earn_enabled")
    @Builder.Default
    private Boolean pointEarnEnabled = true;

    /**
     * 點數累積比例（每消費多少元得 1 點）
     * 例如：10 表示每消費 NT$10 獲得 1 點
     */
    @Column(name = "point_earn_rate")
    @Builder.Default
    private Integer pointEarnRate = 10;

    /**
     * 點數取整方式
     * FLOOR: 無條件捨去（預設）
     * ROUND: 四捨五入
     * CEIL: 無條件進位
     */
    @Column(name = "point_round_mode", length = 10)
    @Builder.Default
    private String pointRoundMode = "FLOOR";

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
    private LocalTime businessEndTime = LocalTime.of(18, 0);

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
    @Builder.Default
    private String closedDays = "[0,6]";

    /**
     * 休息開始時間（午休）
     */
    @Column(name = "break_start_time")
    @Builder.Default
    private LocalTime breakStartTime = LocalTime.of(12, 0);

    /**
     * 休息結束時間（午休）
     */
    @Column(name = "break_end_time")
    @Builder.Default
    private LocalTime breakEndTime = LocalTime.of(13, 0);

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
    // 預約提醒設定
    // ========================================

    /**
     * 是否啟用預約提醒
     */
    @Column(name = "enable_booking_reminder")
    @Builder.Default
    private Boolean enableBookingReminder = true;

    /**
     * 預約提醒提前小時數
     */
    @Column(name = "reminder_hours_before")
    @Builder.Default
    private Integer reminderHoursBefore = 24;

    // ========================================
    // SMS 設定
    // ========================================

    /**
     * 是否啟用 SMS 通知
     */
    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;

    /**
     * 每月 SMS 額度
     */
    @Column(name = "monthly_sms_quota")
    @Builder.Default
    private Integer monthlySmsQuota = 0;

    /**
     * 本月已使用 SMS 數量
     */
    @Column(name = "monthly_sms_used")
    @Builder.Default
    private Integer monthlySmsUsed = 0;

    // ========================================
    // 預約緩衝設定
    // ========================================

    /**
     * 預約緩衝時間（分鐘）
     */
    @Column(name = "booking_buffer_minutes")
    @Builder.Default
    private Integer bookingBufferMinutes = 0;

    // ========================================
    // 生日祝福設定
    // ========================================

    /**
     * 是否啟用生日祝福
     */
    @Column(name = "enable_birthday_greeting")
    @Builder.Default
    private Boolean enableBirthdayGreeting = false;

    /**
     * 生日祝福訊息
     */
    @Column(name = "birthday_greeting_message", length = 500)
    @Builder.Default
    private String birthdayGreetingMessage = "親愛的顧客，祝您生日快樂！🎂 感謝您一直以來的支持，期待再次為您服務！";

    // ========================================
    // 顧客喚回設定
    // ========================================

    /**
     * 是否啟用顧客喚回通知
     */
    @Column(name = "enable_customer_recall")
    @Builder.Default
    private Boolean enableCustomerRecall = false;

    /**
     * 顧客喚回天數（超過幾天未到訪就發送喚回通知）
     */
    @Column(name = "customer_recall_days")
    @Builder.Default
    private Integer customerRecallDays = 30;

    /**
     * 顧客喚回訊息
     */
    @Column(name = "customer_recall_message", length = 500)
    @Builder.Default
    private String customerRecallMessage = "好久不見！我們想念您了 💕 期待您再次光臨，為您提供最優質的服務！";

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
        int quota = this.monthlyPushQuota != null ? this.monthlyPushQuota : 100;
        int used = this.monthlyPushUsed != null ? this.monthlyPushUsed : 0;
        return (quota - used) >= count;
    }

    /**
     * 使用推送額度
     *
     * @param count 使用數量
     */
    public void usePushQuota(int count) {
        this.monthlyPushUsed = (this.monthlyPushUsed != null ? this.monthlyPushUsed : 0) + count;
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
        if (this.pointBalance == null) {
            this.pointBalance = BigDecimal.ZERO;
        }
        this.pointBalance = this.pointBalance.add(amount);
    }

    /**
     * 扣除點數
     *
     * @param amount 扣除金額
     * @return true 表示扣除成功
     */
    public boolean deductPoints(BigDecimal amount) {
        if (this.pointBalance == null) {
            this.pointBalance = BigDecimal.ZERO;
        }
        if (this.pointBalance.compareTo(amount) < 0) {
            return false;
        }
        this.pointBalance = this.pointBalance.subtract(amount);
        return true;
    }

    /**
     * 檢查 SMS 額度是否足夠
     *
     * @param count 需要的數量
     * @return true 表示額度足夠
     */
    public boolean hasSmsQuota(int count) {
        int quota = this.monthlySmsQuota != null ? this.monthlySmsQuota : 0;
        int used = this.monthlySmsUsed != null ? this.monthlySmsUsed : 0;
        return (quota - used) >= count;
    }

    /**
     * 使用 SMS 額度
     *
     * @param count 使用數量
     */
    public void useSmsQuota(int count) {
        this.monthlySmsUsed = (this.monthlySmsUsed != null ? this.monthlySmsUsed : 0) + count;
    }

    /**
     * 重置月度 SMS 計數
     */
    public void resetMonthlySmsUsed() {
        this.monthlySmsUsed = 0;
    }
}
