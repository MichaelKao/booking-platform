package com.booking.platform.service;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.DailyReportResponse;
import com.booking.platform.dto.response.ReportSummaryResponse;
import com.booking.platform.dto.response.TopItemResponse;
import com.booking.platform.dto.response.AdvancedReportResponse;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 報表服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final CouponInstanceRepository couponInstanceRepository;
    private final ProductRepository productRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final TenantFeatureRepository tenantFeatureRepository;

    // ========================================
    // 報表摘要
    // ========================================

    /**
     * 取得報表摘要
     */
    public ReportSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        // 如果沒有租戶上下文，返回空報表
        if (tenantId == null) {
            log.warn("產生報表摘要時沒有租戶上下文");
            return ReportSummaryResponse.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalBookings(0L)
                    .completedBookings(0L)
                    .cancelledBookings(0L)
                    .noShowBookings(0L)
                    .completionRate(java.math.BigDecimal.ZERO)
                    .newCustomers(0L)
                    .returningCustomers(0L)
                    .totalServedCustomers(0L)
                    .totalRevenue(java.math.BigDecimal.ZERO)
                    .serviceRevenue(java.math.BigDecimal.ZERO)
                    .productRevenue(java.math.BigDecimal.ZERO)
                    .averageOrderValue(java.math.BigDecimal.ZERO)
                    .issuedCoupons(0L)
                    .usedCoupons(0L)
                    .couponDiscountAmount(java.math.BigDecimal.ZERO)
                    .build();
        }

        log.info("產生報表摘要，租戶：{}，期間：{} ~ {}", tenantId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 預約統計
        long totalBookings = bookingRepository.countByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );
        long completedBookings = bookingRepository.countByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.COMPLETED, startDateTime, endDateTime
        );
        long cancelledBookings = bookingRepository.countByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.CANCELLED, startDateTime, endDateTime
        );
        long noShowBookings = bookingRepository.countByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.NO_SHOW, startDateTime, endDateTime
        );

        BigDecimal completionRate = BigDecimal.ZERO;
        if (totalBookings > 0) {
            completionRate = BigDecimal.valueOf(completedBookings)
                    .divide(BigDecimal.valueOf(totalBookings), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 顧客統計
        long newCustomers = customerRepository.countNewByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );

        // 回客統計（在指定時間範圍內有預約，且在此之前也有過預約的顧客）
        long returningCustomers = bookingRepository.countReturningCustomersByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );

        // 票券統計
        long issuedCoupons = couponInstanceRepository.countIssuedByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );
        long usedCoupons = couponInstanceRepository.countUsedByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );

        // 營收統計（只計算已完成的預約）
        BigDecimal serviceRevenue = bookingRepository.sumRevenueByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.COMPLETED, startDateTime, endDateTime
        );
        if (serviceRevenue == null) {
            serviceRevenue = BigDecimal.ZERO;
        }

        // 計算平均客單價
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (completedBookings > 0) {
            averageOrderValue = serviceRevenue.divide(
                    BigDecimal.valueOf(completedBookings), 2, RoundingMode.HALF_UP
            );
        }

        return ReportSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .noShowBookings(noShowBookings)
                .completionRate(completionRate)
                .newCustomers(newCustomers)
                .returningCustomers(returningCustomers)
                .totalServedCustomers(completedBookings) // 簡化：以完成預約數代替
                .totalRevenue(serviceRevenue) // 服務營收即為總營收
                .serviceRevenue(serviceRevenue)
                .productRevenue(BigDecimal.ZERO) // 商品營收另計
                .averageOrderValue(averageOrderValue)
                .issuedCoupons(issuedCoupons)
                .usedCoupons(usedCoupons)
                .couponDiscountAmount(BigDecimal.ZERO)
                .build();
    }

    // ========================================
    // 每日報表
    // ========================================

    /**
     * 取得每日報表
     */
    public List<DailyReportResponse> getDailyReport(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        log.info("產生每日報表，租戶：{}，期間：{} ~ {}", tenantId, startDate, endDate);

        List<DailyReportResponse> result = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = current.atTime(LocalTime.MAX);

            long bookingCount = bookingRepository.countByTenantIdAndDateRange(
                    tenantId, dayStart, dayEnd
            );
            long completedCount = bookingRepository.countByTenantIdAndStatusAndDateRange(
                    tenantId, BookingStatus.COMPLETED, dayStart, dayEnd
            );
            long newCustomerCount = customerRepository.countNewByTenantIdAndDateRange(
                    tenantId, dayStart, dayEnd
            );

            // 計算當日營收（只計算已完成的預約）
            BigDecimal dailyRevenue = bookingRepository.sumRevenueByTenantIdAndDate(tenantId, current);
            if (dailyRevenue == null) {
                dailyRevenue = BigDecimal.ZERO;
            }

            result.add(DailyReportResponse.builder()
                    .date(current)
                    .bookingCount(bookingCount)
                    .completedCount(completedCount)
                    .newCustomerCount(newCustomerCount)
                    .revenue(dailyRevenue)
                    .build());

            current = current.plusDays(1);
        }

        return result;
    }

    // ========================================
    // 熱門項目報表
    // ========================================

    /**
     * 取得熱門服務
     */
    public List<TopItemResponse> getTopServices(LocalDate startDate, LocalDate endDate, int limit) {
        String tenantId = TenantContext.getTenantId();

        log.info("查詢熱門服務，租戶：{}，期間：{} ~ {}，數量：{}", tenantId, startDate, endDate, limit);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> topServices = bookingRepository.findTopServicesByTenantId(
                tenantId, startDateTime, endDateTime, limit
        );

        // 計算總數用於百分比
        long total = topServices.stream()
                .mapToLong(row -> (Long) row[2])
                .sum();

        List<TopItemResponse> result = new ArrayList<>();
        for (Object[] row : topServices) {
            String serviceId = (String) row[0];
            String serviceName = (String) row[1];
            Long count = (Long) row[2];

            BigDecimal percentage = BigDecimal.ZERO;
            if (total > 0) {
                percentage = BigDecimal.valueOf(count)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // 計算該服務的營收
            BigDecimal amount = bookingRepository.sumRevenueByTenantIdAndServiceIdAndDateRange(
                    tenantId, serviceId, startDateTime, endDateTime
            );
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }

            result.add(TopItemResponse.builder()
                    .id(serviceId)
                    .name(serviceName)
                    .count(count)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    /**
     * 取得熱門員工
     */
    public List<TopItemResponse> getTopStaff(LocalDate startDate, LocalDate endDate, int limit) {
        String tenantId = TenantContext.getTenantId();

        log.info("查詢熱門員工，租戶：{}，期間：{} ~ {}，數量：{}", tenantId, startDate, endDate, limit);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> topStaff = bookingRepository.findTopStaffByTenantId(
                tenantId, startDateTime, endDateTime, limit
        );

        // 計算總數用於百分比
        long total = topStaff.stream()
                .mapToLong(row -> (Long) row[2])
                .sum();

        List<TopItemResponse> result = new ArrayList<>();
        for (Object[] row : topStaff) {
            String staffId = (String) row[0];
            String staffName = (String) row[1];
            Long count = (Long) row[2];

            BigDecimal percentage = BigDecimal.ZERO;
            if (total > 0) {
                percentage = BigDecimal.valueOf(count)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            result.add(TopItemResponse.builder()
                    .id(staffId)
                    .name(staffName)
                    .count(count)
                    .amount(BigDecimal.ZERO)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    // ========================================
    // 快速統計（Dashboard 用）
    // ========================================

    /**
     * 取得今日統計（含 Dashboard 所需欄位）
     */
    public ReportSummaryResponse getTodaySummary() {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();

        // 如果沒有租戶上下文，返回空報表
        if (tenantId == null) {
            log.warn("取得今日統計時沒有租戶上下文");
            return ReportSummaryResponse.builder()
                    .startDate(today)
                    .endDate(today)
                    .totalBookings(0L)
                    .todayBookings(0L)
                    .pendingBookings(0L)
                    .completedBookings(0L)
                    .totalCustomers(0L)
                    .monthlyRevenue(BigDecimal.ZERO)
                    .build();
        }

        // 取得基本報表
        ReportSummaryResponse summary = getSummary(today, today);

        // 補充 Dashboard 所需欄位
        // 今日預約數
        summary.setTodayBookings(summary.getTotalBookings());

        // 待確認預約數
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        long pendingCount = bookingRepository.countByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.PENDING, startOfDay, endOfDay
        );
        summary.setPendingBookings(pendingCount);

        // 總顧客數
        long totalCustomers = customerRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        summary.setTotalCustomers(totalCustomers);

        // 本月營收（只計算已完成的預約）
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = startOfMonth.atStartOfDay();
        LocalDateTime monthEnd = today.atTime(LocalTime.MAX);
        BigDecimal monthlyRevenue = bookingRepository.sumRevenueByTenantIdAndStatusAndDateRange(
                tenantId, BookingStatus.COMPLETED, monthStart, monthEnd
        );
        summary.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);

        return summary;
    }

    /**
     * 取得本週統計
     */
    public ReportSummaryResponse getWeeklySummary() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return getSummary(startOfWeek, today);
    }

    /**
     * 取得本月統計
     */
    public ReportSummaryResponse getMonthlySummary() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return getSummary(startOfMonth, today);
    }

    // ========================================
    // 時段分布（基本報表）
    // ========================================

    /**
     * 取得預約時段分布
     *
     * <p>不需要訂閱進階報表功能，屬於基本報表
     */
    public List<AdvancedReportResponse.PeakHour> getHourlyDistribution(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        log.info("查詢時段分布，租戶：{}，期間：{} ~ {}", tenantId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return analyzePeakHours(tenantId, startDateTime, endDateTime);
    }

    // ========================================
    // 進階報表（ADVANCED_REPORT 功能）
    // ========================================

    /**
     * 取得進階報表分析
     *
     * <p>需要訂閱 ADVANCED_REPORT 功能
     */
    public AdvancedReportResponse getAdvancedReport(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        log.info("產生進階報表，租戶：{}，期間：{} ~ {}", tenantId, startDate, endDate);

        // 檢查是否有進階報表功能
        boolean hasAdvancedReport = tenantFeatureRepository
                .isFeatureEnabled(tenantId, FeatureCode.ADVANCED_REPORT, LocalDateTime.now());

        if (!hasAdvancedReport) {
            log.warn("租戶 {} 未訂閱 ADVANCED_REPORT 功能", tenantId);
            return AdvancedReportResponse.builder()
                    .hasAccess(false)
                    .message("需要訂閱進階報表功能才能查看此報表")
                    .build();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 顧客分析
        long totalCustomers = customerRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long newCustomers = customerRepository.countNewByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );
        long activeCustomers = bookingRepository.countDistinctCustomersByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );

        // 計算顧客保留率
        BigDecimal retentionRate = BigDecimal.ZERO;
        if (totalCustomers > 0) {
            retentionRate = BigDecimal.valueOf(activeCustomers)
                    .divide(BigDecimal.valueOf(totalCustomers), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 計算平均來客週期（天）
        BigDecimal avgVisitInterval = BigDecimal.valueOf(30); // 預設 30 天

        // 服務趨勢分析 - 計算各服務的預約數變化
        List<AdvancedReportResponse.ServiceTrend> serviceTrends = new ArrayList<>();
        List<Object[]> topServices = bookingRepository.findTopServicesByTenantId(
                tenantId, startDateTime, endDateTime, 5
        );
        for (Object[] row : topServices) {
            serviceTrends.add(AdvancedReportResponse.ServiceTrend.builder()
                    .serviceId((String) row[0])
                    .serviceName((String) row[1])
                    .currentPeriodCount((Long) row[2])
                    .growthRate(BigDecimal.ZERO) // 需要比較前一期
                    .build());
        }

        // 尖峰時段分析
        List<AdvancedReportResponse.PeakHour> peakHours = analyzePeakHours(tenantId, startDateTime, endDateTime);

        // 顧客價值分析
        BigDecimal avgCustomerValue = BigDecimal.ZERO;
        if (activeCustomers > 0) {
            // 簡化計算：總完成預約 / 活躍顧客數
            long completedBookings = bookingRepository.countByTenantIdAndStatusAndDateRange(
                    tenantId, BookingStatus.COMPLETED, startDateTime, endDateTime
            );
            avgCustomerValue = BigDecimal.valueOf(completedBookings)
                    .divide(BigDecimal.valueOf(activeCustomers), 2, RoundingMode.HALF_UP);
        }

        return AdvancedReportResponse.builder()
                .hasAccess(true)
                .startDate(startDate)
                .endDate(endDate)
                .totalCustomers(totalCustomers)
                .newCustomers(newCustomers)
                .activeCustomers(activeCustomers)
                .retentionRate(retentionRate)
                .avgVisitInterval(avgVisitInterval)
                .serviceTrends(serviceTrends)
                .peakHours(peakHours)
                .avgCustomerValue(avgCustomerValue)
                .build();
    }

    /**
     * 分析尖峰時段
     */
    private List<AdvancedReportResponse.PeakHour> analyzePeakHours(
            String tenantId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<AdvancedReportResponse.PeakHour> peakHours = new ArrayList<>();

        // 統計各時段的預約數
        for (int hour = 9; hour < 21; hour++) {
            long count = bookingRepository.countByTenantIdAndHour(tenantId, hour, startDateTime, endDateTime);
            peakHours.add(AdvancedReportResponse.PeakHour.builder()
                    .hour(hour)
                    .hourLabel(String.format("%02d:00 - %02d:00", hour, hour + 1))
                    .bookingCount(count)
                    .build());
        }

        // 標記最高時段
        if (!peakHours.isEmpty()) {
            long maxCount = peakHours.stream()
                    .mapToLong(AdvancedReportResponse.PeakHour::getBookingCount)
                    .max().orElse(0);
            for (AdvancedReportResponse.PeakHour ph : peakHours) {
                ph.setIsPeak(ph.getBookingCount() == maxCount && maxCount > 0);
            }
        }

        return peakHours;
    }
}
