package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.response.AdvancedReportResponse;
import com.booking.platform.dto.response.DailyReportResponse;
import com.booking.platform.dto.response.ReportSummaryResponse;
import com.booking.platform.dto.response.TopItemResponse;
import com.booking.platform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 報表控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ========================================
    // 摘要報表
    // ========================================

    /**
     * 取得報表摘要
     */
    @GetMapping("/summary")
    public ApiResponse<ReportSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range
    ) {
        // 如果沒有提供日期，根據 range 計算
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1); // month
            }
        }
        ReportSummaryResponse result = reportService.getSummary(startDate, endDate);
        return ApiResponse.ok(result);
    }

    /**
     * 取得儀表板統計（Dashboard 用）
     */
    @GetMapping("/dashboard")
    public ApiResponse<ReportSummaryResponse> getDashboard() {
        ReportSummaryResponse result = reportService.getTodaySummary();
        return ApiResponse.ok(result);
    }

    /**
     * 取得今日統計
     */
    @GetMapping("/today")
    public ApiResponse<ReportSummaryResponse> getTodaySummary() {
        ReportSummaryResponse result = reportService.getTodaySummary();
        return ApiResponse.ok(result);
    }

    /**
     * 取得本週統計
     */
    @GetMapping("/weekly")
    public ApiResponse<ReportSummaryResponse> getWeeklySummary() {
        ReportSummaryResponse result = reportService.getWeeklySummary();
        return ApiResponse.ok(result);
    }

    /**
     * 取得本月統計
     */
    @GetMapping("/monthly")
    public ApiResponse<ReportSummaryResponse> getMonthlySummary() {
        ReportSummaryResponse result = reportService.getMonthlySummary();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 趨勢報表
    // ========================================

    /**
     * 取得每日報表
     */
    @GetMapping("/daily")
    public ApiResponse<List<DailyReportResponse>> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range
    ) {
        // 如果沒有提供日期，根據 range 計算
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1); // month
            }
        }
        List<DailyReportResponse> result = reportService.getDailyReport(startDate, endDate);
        return ApiResponse.ok(result);
    }

    // ========================================
    // 排名報表
    // ========================================

    /**
     * 取得熱門服務
     */
    @GetMapping("/top-services")
    public ApiResponse<List<TopItemResponse>> getTopServices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range,
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 如果沒有提供日期，根據 range 計算
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1); // month
            }
        }
        List<TopItemResponse> result = reportService.getTopServices(startDate, endDate, limit);
        return ApiResponse.ok(result);
    }

    /**
     * 取得熱門員工
     */
    @GetMapping("/top-staff")
    public ApiResponse<List<TopItemResponse>> getTopStaff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range,
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 如果沒有提供日期，根據 range 計算
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1); // month
            }
        }
        List<TopItemResponse> result = reportService.getTopStaff(startDate, endDate, limit);
        return ApiResponse.ok(result);
    }

    // ========================================
    // 時段分布（基本報表）
    // ========================================

    /**
     * 取得預約時段分布
     */
    @GetMapping("/hourly")
    public ApiResponse<List<AdvancedReportResponse.PeakHour>> getHourlyDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range
    ) {
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1);
            }
        }
        List<AdvancedReportResponse.PeakHour> result = reportService.getHourlyDistribution(startDate, endDate);
        return ApiResponse.ok(result);
    }

    // ========================================
    // 進階報表（需訂閱 ADVANCED_REPORT）
    // ========================================

    /**
     * 取得進階報表
     *
     * <p>需要訂閱 ADVANCED_REPORT 功能才能存取完整資料
     */
    @GetMapping("/advanced")
    public ApiResponse<AdvancedReportResponse> getAdvancedReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String range
    ) {
        // 如果沒有提供日期，根據 range 計算
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            endDate = today;
            switch (range) {
                case "week" -> startDate = today.minusDays(7);
                case "quarter" -> startDate = today.minusMonths(3);
                default -> startDate = today.minusMonths(1); // month
            }
        }
        AdvancedReportResponse result = reportService.getAdvancedReport(startDate, endDate);
        return ApiResponse.ok(result);
    }
}
