package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 報表摘要回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    // ========================================
    // 日期區間
    // ========================================

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // ========================================
    // 預約統計
    // ========================================

    /**
     * 預約總數
     */
    private Long totalBookings;

    /**
     * 今日預約數（Dashboard 用）
     */
    private Long todayBookings;

    /**
     * 待確認預約數（Dashboard 用）
     */
    private Long pendingBookings;

    /**
     * 已完成預約數
     */
    private Long completedBookings;

    /**
     * 今日完成數
     */
    private Long todayCompleted;

    /**
     * 本週預約數
     */
    private Long weeklyBookings;

    /**
     * 本月預約數
     */
    private Long monthlyBookings;

    /**
     * 已取消預約數
     */
    private Long cancelledBookings;

    /**
     * 未到店數
     */
    private Long noShowBookings;

    /**
     * 預約完成率
     */
    private BigDecimal completionRate;

    // ========================================
    // 顧客統計
    // ========================================

    /**
     * 新顧客數
     */
    private Long newCustomers;

    /**
     * 今日新顧客
     */
    private Long todayNewCustomers;

    /**
     * 本月新顧客
     */
    private Long monthlyNewCustomers;

    /**
     * 回訪顧客數
     */
    private Long returningCustomers;

    /**
     * 總服務顧客數
     */
    private Long totalServedCustomers;

    /**
     * 總顧客數（Dashboard 用）
     */
    private Long totalCustomers;

    // ========================================
    // 營收統計
    // ========================================

    /**
     * 總營收
     */
    private BigDecimal totalRevenue;

    /**
     * 今日營收
     */
    private BigDecimal todayRevenue;

    /**
     * 本週營收
     */
    private BigDecimal weeklyRevenue;

    /**
     * 本月營收（Dashboard 用）
     */
    private BigDecimal monthlyRevenue;

    /**
     * 服務營收
     */
    private BigDecimal serviceRevenue;

    /**
     * 商品營收
     */
    private BigDecimal productRevenue;

    /**
     * 平均客單價
     */
    private BigDecimal averageOrderValue;

    // ========================================
    // 票券統計
    // ========================================

    /**
     * 發放票券數
     */
    private Long issuedCoupons;

    /**
     * 已使用票券數
     */
    private Long usedCoupons;

    /**
     * 票券折抵金額
     */
    private BigDecimal couponDiscountAmount;
}
