package com.booking.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店家設定完成狀態回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupStatusResponse {

    /**
     * 是否已完善基本資訊（店名、電話、地址至少填 2 項）
     */
    private boolean hasBasicInfo;

    /**
     * 是否已設定營業時間
     */
    private boolean hasBusinessHours;

    /**
     * 員工數量
     */
    private long staffCount;

    /**
     * 服務項目數量
     */
    private long serviceCount;

    /**
     * LINE Bot 是否已設定且啟用
     */
    private boolean lineConfigured;

    /**
     * 是否已有預約記錄
     */
    private boolean hasBookings;

    /**
     * 完成百分比 (0-100)
     */
    private int completionPercentage;

    /**
     * 總步驟數
     */
    private int totalSteps;

    /**
     * 已完成步驟數
     */
    private int completedSteps;
}
