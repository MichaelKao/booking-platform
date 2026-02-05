package com.booking.platform.service;

import com.booking.platform.dto.response.AdminDashboardResponse;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.dto.response.TenantListItemResponse;
import com.booking.platform.entity.system.PointTopUp;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.enums.TopUpStatus;
import com.booking.platform.mapper.PointTopUpMapper;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.PointTopUpRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 超級管理儀表板服務
 *
 * <p>提供平台整體統計數據
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminDashboardService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;
    private final PointTopUpRepository pointTopUpRepository;
    private final BookingRepository bookingRepository;
    private final PointTopUpMapper pointTopUpMapper;

    // ========================================
    // 儀表板資料
    // ========================================

    /**
     * 取得儀表板資料
     *
     * @return 儀表板回應
     */
    public AdminDashboardResponse getDashboard() {
        log.debug("取得超級管理儀表板資料");

        // ========================================
        // 1. 租戶統計
        // ========================================

        long totalTenants = tenantRepository.countByStatus(null);
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long pendingTenants = tenantRepository.countByStatus(TenantStatus.PENDING);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);

        // ========================================
        // 2. 儲值統計
        // ========================================

        long pendingTopUps = pointTopUpRepository.countByStatusAndDeletedAtIsNull(TopUpStatus.PENDING);

        // 計算待審核金額
        BigDecimal pendingTopUpAmount = pointTopUpRepository.sumPendingAmount();
        if (pendingTopUpAmount == null) {
            pendingTopUpAmount = BigDecimal.ZERO;
        }

        // 計算本月儲值
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        Integer monthlyApprovedPoints = pointTopUpRepository.sumApprovedPointsBetween(monthStart, monthEnd);
        if (monthlyApprovedPoints == null) {
            monthlyApprovedPoints = 0;
        }

        // 計算本月審核通過金額
        BigDecimal monthlyApprovedAmount = pointTopUpRepository.sumApprovedAmountBetween(monthStart, monthEnd);
        if (monthlyApprovedAmount == null) {
            monthlyApprovedAmount = BigDecimal.ZERO;
        }

        // ========================================
        // 3. 預約統計
        // ========================================

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        long todayBookings = bookingRepository.countByBookingDateBetween(
                todayStart.toLocalDate(), todayEnd.toLocalDate()
        );
        long monthlyBookings = bookingRepository.countByBookingDateBetween(
                monthStart.toLocalDate(), monthEnd.toLocalDate()
        );

        // ========================================
        // 4. 最近資料
        // ========================================

        // 最近註冊的租戶
        Pageable top5 = PageRequest.of(0, 5);
        List<TenantListItemResponse> recentTenants = tenantRepository
                .findListItems(null, null, top5)
                .getContent();

        // 待審核的儲值申請
        List<PointTopUp> pendingTopUpList = pointTopUpRepository
                .findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(TopUpStatus.PENDING);
        List<PointTopUpResponse> pendingTopUpResponses = pendingTopUpList.stream()
                .limit(10)
                .map(pointTopUpMapper::toResponse)
                .collect(Collectors.toList());

        // ========================================
        // 5. 組合並返回
        // ========================================

        return AdminDashboardResponse.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .pendingTenants(pendingTenants)
                .suspendedTenants(suspendedTenants)
                .pendingTopUps(pendingTopUps)
                .pendingTopUpAmount(pendingTopUpAmount)
                .monthlyApprovedAmount(monthlyApprovedAmount)
                .monthlyApprovedPoints(monthlyApprovedPoints)
                .todayBookings(todayBookings)
                .monthlyBookings(monthlyBookings)
                .recentTenants(recentTenants)
                .pendingTopUpList(pendingTopUpResponses)
                .build();
    }
}
