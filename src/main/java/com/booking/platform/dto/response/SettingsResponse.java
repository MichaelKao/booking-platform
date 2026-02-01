package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 店家設定回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingsResponse {

    // ========================================
    // 基本資訊
    // ========================================

    /**
     * 租戶 ID
     */
    private String tenantId;

    /**
     * 租戶代碼
     */
    private String code;

    /**
     * 店家名稱
     */
    private String name;

    /**
     * 店家描述
     */
    private String description;

    /**
     * Logo URL
     */
    private String logoUrl;

    // ========================================
    // 聯絡資訊
    // ========================================

    /**
     * 聯絡電話
     */
    private String phone;

    /**
     * 電子郵件
     */
    private String email;

    /**
     * 店家地址
     */
    private String address;

    // ========================================
    // 狀態資訊
    // ========================================

    /**
     * 租戶狀態
     */
    private String status;

    /**
     * 是否為測試帳號
     */
    private Boolean isTestAccount;

    // ========================================
    // 配額資訊
    // ========================================

    /**
     * 員工數量上限
     */
    private Integer maxStaffCount;

    /**
     * 每月推送訊息額度
     */
    private Integer monthlyPushQuota;

    /**
     * 本月已使用推送數量
     */
    private Integer monthlyPushUsed;

    /**
     * 推送額度剩餘
     */
    private Integer monthlyPushRemaining;

    // ========================================
    // 點數資訊
    // ========================================

    /**
     * 點數餘額
     */
    private BigDecimal pointBalance;

    // ========================================
    // 時間資訊
    // ========================================

    /**
     * 啟用時間
     */
    private LocalDateTime activatedAt;

    /**
     * 到期時間
     */
    private LocalDateTime expiredAt;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    // ========================================
    // 營業設定
    // ========================================

    /**
     * 營業開始時間
     */
    private LocalTime businessStartTime;

    /**
     * 營業結束時間
     */
    private LocalTime businessEndTime;

    /**
     * 預約時段間隔（分鐘）
     */
    private Integer bookingInterval;

    /**
     * 最大預約提前天數
     */
    private Integer maxAdvanceBookingDays;

    /**
     * 公休日（JSON 格式）
     */
    private String closedDays;

    /**
     * 休息開始時間（午休）
     */
    private LocalTime breakStartTime;

    /**
     * 休息結束時間（午休）
     */
    private LocalTime breakEndTime;

    // ========================================
    // 通知設定
    // ========================================

    /**
     * 新預約通知
     */
    private Boolean notifyNewBooking;

    /**
     * 預約提醒通知
     */
    private Boolean notifyBookingReminder;

    /**
     * 取消預約通知
     */
    private Boolean notifyBookingCancel;
}
