package com.booking.platform.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 更新店家設定請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {

    // ========================================
    // 基本資訊
    // ========================================

    /**
     * 店家名稱
     */
    @Size(max = 100, message = "店家名稱長度不能超過 100 字")
    private String name;

    /**
     * 店家描述
     */
    @Size(max = 500, message = "店家描述長度不能超過 500 字")
    private String description;

    /**
     * Logo URL
     */
    @Size(max = 500, message = "Logo URL 長度不能超過 500 字")
    private String logoUrl;

    // ========================================
    // 聯絡資訊
    // ========================================

    /**
     * 聯絡電話
     */
    @Size(max = 20, message = "聯絡電話長度不能超過 20 字")
    private String phone;

    /**
     * 電子郵件
     */
    @Size(max = 100, message = "電子郵件長度不能超過 100 字")
    private String email;

    /**
     * 店家地址
     */
    @Size(max = 200, message = "店家地址長度不能超過 200 字")
    private String address;

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
     * 預約間隔（分鐘）
     */
    @Min(value = 5, message = "預約間隔最少 5 分鐘")
    @Max(value = 120, message = "預約間隔最多 120 分鐘")
    private Integer bookingInterval;

    /**
     * 預約提前天數上限
     */
    @Min(value = 1, message = "預約提前天數最少 1 天")
    @Max(value = 365, message = "預約提前天數最多 365 天")
    private Integer maxAdvanceBookingDays;

    /**
     * 公休日（JSON 格式，例如：[0,6] 表示週日和週六）
     */
    @Size(max = 50, message = "公休日設定長度不能超過 50 字")
    private String closedDays;

    /**
     * 休息開始時間（午休）
     */
    private LocalTime breakStartTime;

    /**
     * 休息結束時間（午休）
     */
    private LocalTime breakEndTime;
}
