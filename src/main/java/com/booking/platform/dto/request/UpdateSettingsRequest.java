package com.booking.platform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 營業時間（JSON 格式）
     */
    @Size(max = 1000, message = "營業時間設定長度不能超過 1000 字")
    private String businessHours;

    /**
     * 預約間隔（分鐘）
     */
    private Integer bookingInterval;

    /**
     * 預約提前天數上限
     */
    private Integer maxAdvanceBookingDays;

    /**
     * 預約取消時限（小時）
     */
    private Integer cancelBeforeHours;
}
