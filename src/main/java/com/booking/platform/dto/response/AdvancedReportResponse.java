package com.booking.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 進階報表回應
 *
 * <p>需要訂閱 ADVANCED_REPORT 功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedReportResponse {

    /**
     * 是否有權限存取
     */
    private Boolean hasAccess;

    /**
     * 無權限時的訊息
     */
    private String message;

    /**
     * 報表開始日期
     */
    private LocalDate startDate;

    /**
     * 報表結束日期
     */
    private LocalDate endDate;

    // ========================================
    // 顧客分析
    // ========================================

    /**
     * 總顧客數
     */
    private Long totalCustomers;

    /**
     * 新增顧客數
     */
    private Long newCustomers;

    /**
     * 活躍顧客數（期間內有預約的顧客）
     */
    private Long activeCustomers;

    /**
     * 顧客保留率（%）
     */
    private BigDecimal retentionRate;

    /**
     * 平均來客週期（天）
     */
    private BigDecimal avgVisitInterval;

    /**
     * 平均顧客價值
     */
    private BigDecimal avgCustomerValue;

    // ========================================
    // 服務趨勢
    // ========================================

    /**
     * 服務趨勢列表
     */
    private List<ServiceTrend> serviceTrends;

    // ========================================
    // 尖峰時段
    // ========================================

    /**
     * 尖峰時段分析
     */
    private List<PeakHour> peakHours;

    // ========================================
    // 內嵌類別
    // ========================================

    /**
     * 服務趨勢
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceTrend {
        /**
         * 服務 ID
         */
        private String serviceId;

        /**
         * 服務名稱
         */
        private String serviceName;

        /**
         * 當期預約數
         */
        private Long currentPeriodCount;

        /**
         * 前期預約數
         */
        private Long previousPeriodCount;

        /**
         * 成長率（%）
         */
        private BigDecimal growthRate;
    }

    /**
     * 尖峰時段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHour {
        /**
         * 小時（24 小時制）
         */
        private Integer hour;

        /**
         * 時段標籤
         */
        private String hourLabel;

        /**
         * 預約數
         */
        private Long bookingCount;

        /**
         * 是否為尖峰時段
         */
        private Boolean isPeak;
    }
}
