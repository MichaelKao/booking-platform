package com.booking.platform.service;

import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.UpdateSettingsRequest;
import com.booking.platform.dto.response.SettingsResponse;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 店家設定服務
 *
 * <p>管理店家的基本設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettingsService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得當前店家設定
     *
     * @return 設定資料
     */
    public SettingsResponse getSettings() {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("取得店家設定，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢租戶資料
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        // ========================================
        // 3. 轉換並返回
        // ========================================

        return toSettingsResponse(tenant);
    }

    /**
     * 依租戶 ID 取得設定（供內部使用）
     *
     * @param tenantId 租戶 ID
     * @return 設定資料
     */
    public SettingsResponse getSettingsByTenantId(String tenantId) {
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        return toSettingsResponse(tenant);
    }

    // ========================================
    // 更新方法
    // ========================================

    /**
     * 更新店家設定
     *
     * @param request 更新請求
     * @return 更新後的設定
     */
    @Transactional
    public SettingsResponse updateSettings(UpdateSettingsRequest request) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.info("更新店家設定，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢租戶資料
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        // ========================================
        // 3. 更新欄位
        // ========================================

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tenant.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            tenant.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPhone() != null) {
            tenant.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            tenant.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            tenant.setAddress(request.getAddress());
        }

        // ========================================
        // 3.1 更新營業設定
        // ========================================

        if (request.getBusinessStartTime() != null) {
            tenant.setBusinessStartTime(request.getBusinessStartTime());
        }
        if (request.getBusinessEndTime() != null) {
            tenant.setBusinessEndTime(request.getBusinessEndTime());
        }
        if (request.getBookingInterval() != null) {
            tenant.setBookingInterval(request.getBookingInterval());
        }
        if (request.getMaxAdvanceBookingDays() != null) {
            tenant.setMaxAdvanceBookingDays(request.getMaxAdvanceBookingDays());
        }
        if (request.getClosedDays() != null) {
            tenant.setClosedDays(request.getClosedDays());
        }
        if (request.getBreakStartTime() != null) {
            tenant.setBreakStartTime(request.getBreakStartTime());
        }
        if (request.getBreakEndTime() != null) {
            tenant.setBreakEndTime(request.getBreakEndTime());
        }

        // ========================================
        // 4. 儲存更新
        // ========================================

        tenant = tenantRepository.save(tenant);
        log.info("店家設定更新成功，租戶 ID：{}", tenantId);

        // ========================================
        // 5. 返回結果
        // ========================================

        return toSettingsResponse(tenant);
    }

    // ========================================
    // 工具方法
    // ========================================

    /**
     * 轉換為設定回應
     *
     * @param tenant 租戶實體
     * @return 設定回應
     */
    private SettingsResponse toSettingsResponse(Tenant tenant) {
        return SettingsResponse.builder()
                .tenantId(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .description(tenant.getDescription())
                .logoUrl(tenant.getLogoUrl())
                .phone(tenant.getPhone())
                .email(tenant.getEmail())
                .address(tenant.getAddress())
                .status(tenant.getStatus().name())
                .isTestAccount(tenant.getIsTestAccount())
                .maxStaffCount(tenant.getMaxStaffCount())
                .monthlyPushQuota(tenant.getMonthlyPushQuota())
                .monthlyPushUsed(tenant.getMonthlyPushUsed())
                .monthlyPushRemaining(tenant.getMonthlyPushQuota() - tenant.getMonthlyPushUsed())
                .pointBalance(tenant.getPointBalance())
                .activatedAt(tenant.getActivatedAt())
                .expiredAt(tenant.getExpiredAt())
                .createdAt(tenant.getCreatedAt())
                // 營業設定
                .businessStartTime(tenant.getBusinessStartTime())
                .businessEndTime(tenant.getBusinessEndTime())
                .bookingInterval(tenant.getBookingInterval())
                .maxAdvanceBookingDays(tenant.getMaxAdvanceBookingDays())
                .closedDays(tenant.getClosedDays())
                .breakStartTime(tenant.getBreakStartTime())
                .breakEndTime(tenant.getBreakEndTime())
                .build();
    }
}
