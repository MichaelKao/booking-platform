package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.AdjustPointsRequest;
import com.booking.platform.dto.request.CreatePointTopUpRequest;
import com.booking.platform.dto.request.ReviewTopUpRequest;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.entity.system.PointTopUp;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TopUpStatus;
import com.booking.platform.mapper.PointTopUpMapper;
import com.booking.platform.repository.PointTopUpRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 點數儲值服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PointTopUpService {

    private final PointTopUpRepository pointTopUpRepository;
    private final TenantRepository tenantRepository;
    private final PointTopUpMapper pointTopUpMapper;

    // ========================================
    // 管理員查詢
    // ========================================

    /**
     * 取得所有儲值申請（分頁）
     */
    public PageResponse<PointTopUpResponse> getAllTopUps(
            TopUpStatus status,
            String tenantId,
            Pageable pageable
    ) {
        Page<PointTopUp> page = pointTopUpRepository.findAllWithFilters(status, tenantId, pageable);

        List<PointTopUpResponse> content = page.getContent().stream()
                .map(topUp -> {
                    String tenantName = tenantRepository.findById(topUp.getTenantId())
                            .map(Tenant::getName)
                            .orElse(null);
                    return pointTopUpMapper.toResponse(topUp, tenantName);
                })
                .collect(Collectors.toList());

        return PageResponse.<PointTopUpResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 取得待審核的儲值申請
     */
    public List<PointTopUpResponse> getPendingTopUps() {
        return pointTopUpRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(TopUpStatus.PENDING)
                .stream()
                .map(topUp -> {
                    String tenantName = tenantRepository.findById(topUp.getTenantId())
                            .map(Tenant::getName)
                            .orElse(null);
                    return pointTopUpMapper.toResponse(topUp, tenantName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 取得待審核數量
     */
    public long getPendingCount() {
        return pointTopUpRepository.countByStatusAndDeletedAtIsNull(TopUpStatus.PENDING);
    }

    /**
     * 取得儲值統計資料
     */
    public java.util.Map<String, Object> getStats() {
        long pendingCount = pointTopUpRepository.countByStatusAndDeletedAtIsNull(TopUpStatus.PENDING);
        long approvedCount = pointTopUpRepository.countByStatusAndDeletedAtIsNull(TopUpStatus.APPROVED);
        long rejectedCount = pointTopUpRepository.countByStatusAndDeletedAtIsNull(TopUpStatus.REJECTED);

        // 計算本月總金額（已通過的，使用 DB 查詢避免載入全部記錄）
        LocalDateTime monthStart = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        BigDecimal totalAmount = pointTopUpRepository.sumApprovedAmountBetween(monthStart, monthEnd);

        return java.util.Map.of(
                "pendingCount", pendingCount,
                "approvedCount", approvedCount,
                "rejectedCount", rejectedCount,
                "totalAmount", totalAmount
        );
    }

    /**
     * 取得儲值申請詳情
     */
    public PointTopUpResponse getTopUpDetail(String id) {
        PointTopUp topUp = pointTopUpRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POINT_TOPUP_NOT_FOUND, "找不到指定的儲值申請"
                ));

        String tenantName = tenantRepository.findById(topUp.getTenantId())
                .map(Tenant::getName)
                .orElse(null);

        return pointTopUpMapper.toResponse(topUp, tenantName);
    }

    // ========================================
    // 店家查詢
    // ========================================

    /**
     * 取得店家的儲值申請（分頁）
     */
    public PageResponse<PointTopUpResponse> getTenantTopUps(TopUpStatus status, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<PointTopUp> page = pointTopUpRepository.findByTenantId(tenantId, status, pageable);

        List<PointTopUpResponse> content = page.getContent().stream()
                .map(pointTopUpMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PointTopUpResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ========================================
    // 店家操作
    // ========================================

    /**
     * 建立儲值申請（店家）
     */
    @Transactional
    public PointTopUpResponse createTopUp(CreatePointTopUpRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立儲值申請，租戶：{}，點數：{}", tenantId, request.getPoints());

        PointTopUp topUp = PointTopUp.builder()
                .points(request.getPoints())
                .amount(request.getAmount())
                .status(TopUpStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentProofUrl(request.getPaymentProofUrl())
                .requestNote(request.getRequestNote())
                .build();
        topUp.setTenantId(tenantId);

        topUp = pointTopUpRepository.save(topUp);

        log.info("儲值申請建立成功，ID：{}", topUp.getId());

        return pointTopUpMapper.toResponse(topUp);
    }

    /**
     * 取消儲值申請（店家）
     */
    @Transactional
    public void cancelTopUp(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("取消儲值申請，ID：{}", id);

        PointTopUp topUp = pointTopUpRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POINT_TOPUP_NOT_FOUND, "找不到指定的儲值申請"
                ));

        if (topUp.getStatus() != TopUpStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.POINT_TOPUP_ALREADY_PROCESSED,
                    "此儲值申請已處理，無法取消"
            );
        }

        topUp.cancel();
        pointTopUpRepository.save(topUp);

        log.info("儲值申請取消成功，ID：{}", id);
    }

    // ========================================
    // 管理員審核操作
    // ========================================

    /**
     * 審核通過
     */
    @Transactional
    public PointTopUpResponse approveTopUp(String id, String reviewerId, ReviewTopUpRequest request) {
        log.info("審核通過儲值申請，ID：{}，審核者：{}", id, reviewerId);

        PointTopUp topUp = pointTopUpRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POINT_TOPUP_NOT_FOUND, "找不到指定的儲值申請"
                ));

        if (!topUp.canReview()) {
            throw new BusinessException(
                    ErrorCode.POINT_TOPUP_ALREADY_PROCESSED,
                    "此儲值申請已處理"
            );
        }

        // 取得租戶並更新點數（排除已刪除的租戶）
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(topUp.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        BigDecimal balanceBefore = tenant.getPointBalance() != null ? tenant.getPointBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(BigDecimal.valueOf(topUp.getPoints()));

        // 更新租戶點數
        tenant.setPointBalance(balanceAfter);
        tenantRepository.save(tenant);

        // 更新儲值申請狀態
        topUp.approve(reviewerId, balanceBefore.intValue(), balanceAfter.intValue(), request.getNote());
        topUp = pointTopUpRepository.save(topUp);

        log.info("儲值申請審核通過，租戶：{}，新點數餘額：{}", topUp.getTenantId(), balanceAfter);

        String tenantName = tenant.getName();
        return pointTopUpMapper.toResponse(topUp, tenantName);
    }

    /**
     * 審核駁回
     */
    @Transactional
    public PointTopUpResponse rejectTopUp(String id, String reviewerId, ReviewTopUpRequest request) {
        log.info("審核駁回儲值申請，ID：{}，審核者：{}", id, reviewerId);

        PointTopUp topUp = pointTopUpRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POINT_TOPUP_NOT_FOUND, "找不到指定的儲值申請"
                ));

        if (!topUp.canReview()) {
            throw new BusinessException(
                    ErrorCode.POINT_TOPUP_ALREADY_PROCESSED,
                    "此儲值申請已處理"
            );
        }

        topUp.reject(reviewerId, request.getNote());
        topUp = pointTopUpRepository.save(topUp);

        log.info("儲值申請審核駁回，ID：{}", id);

        String tenantName = tenantRepository.findById(topUp.getTenantId())
                .map(Tenant::getName)
                .orElse(null);

        return pointTopUpMapper.toResponse(topUp, tenantName);
    }

    /**
     * 手動調整租戶點數
     */
    @Transactional
    public void adjustTenantPoints(String tenantId, AdjustPointsRequest request, String operatorId) {
        log.info("手動調整租戶點數，租戶：{}，點數：{}，原因：{}，操作者：{}",
                tenantId, request.getPoints(), request.getReason(), operatorId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        BigDecimal currentBalance = tenant.getPointBalance() != null ? tenant.getPointBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(BigDecimal.valueOf(request.getPoints()));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    ErrorCode.POINT_INSUFFICIENT,
                    "扣除後點數餘額不能為負數"
            );
        }

        tenant.setPointBalance(newBalance);
        tenantRepository.save(tenant);

        log.info("租戶點數調整成功，租戶：{}，原餘額：{}，新餘額：{}", tenantId, currentBalance, newBalance);
    }
}
