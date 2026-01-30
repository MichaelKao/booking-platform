package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ReportSummaryResponse result = reportService.getSummary(startDate, endDate);
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TopItemResponse> result = reportService.getTopServices(startDate, endDate, limit);
        return ApiResponse.ok(result);
    }

    /**
     * 取得熱門員工
     */
    @GetMapping("/top-staff")
    public ApiResponse<List<TopItemResponse>> getTopStaff(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TopItemResponse> result = reportService.getTopStaff(startDate, endDate, limit);
        return ApiResponse.ok(result);
    }
}
