package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreatePointTopUpRequest;
import com.booking.platform.dto.response.PointBalanceResponse;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.dto.response.PointTransactionResponse;
import com.booking.platform.entity.system.PointTopUp;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TopUpStatus;
import com.booking.platform.mapper.PointTopUpMapper;
import com.booking.platform.repository.PointTopUpRepository;
import com.booking.platform.repository.PointTransactionRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 點數服務
 *
 * <p>管理店家的點數餘額、儲值、交易記錄
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PointService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;
    private final PointTopUpRepository pointTopUpRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointTopUpMapper pointTopUpMapper;

    // ========================================
    // 餘額查詢
    // ========================================

    /**
     * 取得點數餘額
     *
     * @return 餘額資料
     */
    public PointBalanceResponse getBalance() {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("取得點數餘額，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢租戶資料
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        // ========================================
        // 3. 查詢待審核儲值金額
        // ========================================

        BigDecimal pendingTopUp = pointTopUpRepository.sumPendingAmountByTenantId(tenantId);
        if (pendingTopUp == null) {
            pendingTopUp = BigDecimal.ZERO;
        }

        // ========================================
        // 4. 返回結果
        // ========================================

        return PointBalanceResponse.builder()
                .tenantId(tenantId)
                .tenantName(tenant.getName())
                .balance(tenant.getPointBalance())
                .pendingTopUp(pendingTopUp)
                .monthlyUsed(BigDecimal.ZERO)
                .pushQuotaRemaining(tenant.getMonthlyPushQuota() - tenant.getMonthlyPushUsed())
                .build();
    }

    // ========================================
    // 儲值申請
    // ========================================

    /**
     * 申請儲值
     *
     * @param request 儲值請求
     * @return 儲值申請資料
     */
    @Transactional
    public PointTopUpResponse createTopUp(CreatePointTopUpRequest request) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.info("申請儲值，租戶 ID：{}，點數：{}，金額：{}",
                tenantId, request.getPoints(), request.getAmount());

        // ========================================
        // 2. 驗證租戶存在
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        // ========================================
        // 3. 驗證參數
        // ========================================

        if (request.getPoints() == null || request.getPoints() < 100) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "儲值點數最少 100 點");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "儲值金額必須大於 0");
        }

        // ========================================
        // 4. 建立儲值申請
        // ========================================

        PointTopUp topUp = PointTopUp.builder()
                .points(request.getPoints())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentProofUrl(request.getPaymentProofUrl())
                .requestNote(request.getRequestNote())
                .status(TopUpStatus.PENDING)
                .build();

        // 設定租戶 ID（繼承自 BaseEntity）
        topUp.setTenantId(tenantId);

        topUp = pointTopUpRepository.save(topUp);

        log.info("儲值申請建立成功，ID：{}，點數：{}，金額：{}",
                topUp.getId(), topUp.getPoints(), topUp.getAmount());

        // ========================================
        // 5. 返回結果
        // ========================================

        return pointTopUpMapper.toResponse(topUp);
    }

    // ========================================
    // 儲值記錄查詢
    // ========================================

    /**
     * 查詢儲值記錄
     *
     * @param status 狀態篩選
     * @param pageable 分頁參數
     * @return 分頁結果
     */
    public PageResponse<PointTopUpResponse> getTopUpList(TopUpStatus status, Pageable pageable) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("查詢儲值記錄，租戶 ID：{}，狀態：{}", tenantId, status);

        // ========================================
        // 2. 查詢資料
        // ========================================

        Page<PointTopUp> page;
        if (status != null) {
            page = pointTopUpRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = pointTopUpRepository.findByTenantId(tenantId, pageable);
        }

        // ========================================
        // 3. 轉換並返回
        // ========================================

        Page<PointTopUpResponse> responsePage = page.map(pointTopUpMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    // ========================================
    // 交易記錄查詢
    // ========================================

    /**
     * 查詢點數交易記錄
     *
     * @param pageable 分頁參數
     * @return 分頁結果
     */
    public PageResponse<PointTransactionResponse> getTransactionList(Pageable pageable) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("查詢點數交易記錄，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢資料
        // ========================================

        Page<PointTransactionResponse> page = pointTransactionRepository
                .findTransactionsByTenantId(tenantId, pageable);

        // ========================================
        // 3. 返回結果
        // ========================================

        return PageResponse.from(page);
    }
}
