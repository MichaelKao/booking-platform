package com.booking.platform.service;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.DailyReportResponse;
import com.booking.platform.dto.response.ReportSummaryResponse;
import com.booking.platform.dto.response.TopItemResponse;
import com.booking.platform.enums.BookingStatus;
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

    // ========================================
    // 報表摘要
    // ========================================

    /**
     * 取得報表摘要
     */
    public ReportSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

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

        // 票券統計
        long issuedCoupons = couponInstanceRepository.countIssuedByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );
        long usedCoupons = couponInstanceRepository.countUsedByTenantIdAndDateRange(
                tenantId, startDateTime, endDateTime
        );

        return ReportSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .noShowBookings(noShowBookings)
                .completionRate(completionRate)
                .newCustomers(newCustomers)
                .returningCustomers(0L) // TODO: 需要更複雜的查詢
                .totalServedCustomers(completedBookings) // 簡化：以完成預約數代替
                .totalRevenue(BigDecimal.ZERO) // TODO: 需要訂單系統
                .serviceRevenue(BigDecimal.ZERO)
                .productRevenue(BigDecimal.ZERO)
                .averageOrderValue(BigDecimal.ZERO)
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

            result.add(DailyReportResponse.builder()
                    .date(current)
                    .bookingCount(bookingCount)
                    .completedCount(completedCount)
                    .newCustomerCount(newCustomerCount)
                    .revenue(BigDecimal.ZERO) // TODO: 需要訂單系統
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

            result.add(TopItemResponse.builder()
                    .id(serviceId)
                    .name(serviceName)
                    .count(count)
                    .amount(BigDecimal.ZERO) // TODO: 需要價格資訊
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
     * 取得今日統計
     */
    public ReportSummaryResponse getTodaySummary() {
        LocalDate today = LocalDate.now();
        return getSummary(today, today);
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
}
