package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
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
        // 3. 驗證時間邏輯
        // ========================================

        // 營業時間驗證：開始時間不能晚於結束時間
        if (request.getBusinessStartTime() != null && request.getBusinessEndTime() != null
                && !request.getBusinessStartTime().isBefore(request.getBusinessEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "營業開始時間必須早於結束時間");
        }
        // 單邊更新時，與現有值比較
        if (request.getBusinessStartTime() != null && request.getBusinessEndTime() == null
                && tenant.getBusinessEndTime() != null
                && !request.getBusinessStartTime().isBefore(tenant.getBusinessEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "營業開始時間必須早於結束時間");
        }
        if (request.getBusinessEndTime() != null && request.getBusinessStartTime() == null
                && tenant.getBusinessStartTime() != null
                && !tenant.getBusinessStartTime().isBefore(request.getBusinessEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "營業開始時間必須早於結束時間");
        }

        // 休息時間驗證：開始時間不能晚於結束時間
        if (request.getBreakStartTime() != null && request.getBreakEndTime() != null
                && !request.getBreakStartTime().isBefore(request.getBreakEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "休息開始時間必須早於結束時間");
        }
        if (request.getBreakStartTime() != null && request.getBreakEndTime() == null
                && tenant.getBreakEndTime() != null
                && !request.getBreakStartTime().isBefore(tenant.getBreakEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "休息開始時間必須早於結束時間");
        }
        if (request.getBreakEndTime() != null && request.getBreakStartTime() == null
                && tenant.getBreakStartTime() != null
                && !tenant.getBreakStartTime().isBefore(request.getBreakEndTime())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                    "休息開始時間必須早於結束時間");
        }

        // ========================================
        // 4. 更新欄位
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
        // 4.1 更新營業設定
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
        // 4.2 更新通知設定
        // ========================================

        if (request.getNotifyNewBooking() != null) {
            tenant.setNotifyNewBooking(request.getNotifyNewBooking());
        }
        if (request.getNotifyBookingReminder() != null) {
            tenant.setNotifyBookingReminder(request.getNotifyBookingReminder());
        }
        if (request.getNotifyBookingCancel() != null) {
            tenant.setNotifyBookingCancel(request.getNotifyBookingCancel());
        }

        // ========================================
        // 4.3 更新顧客點數累積設定
        // ========================================

        if (request.getPointEarnEnabled() != null) {
            tenant.setPointEarnEnabled(request.getPointEarnEnabled());
        }
        if (request.getPointEarnRate() != null) {
            tenant.setPointEarnRate(request.getPointEarnRate());
        }
        if (request.getPointRoundMode() != null) {
            // 驗證取整方式
            String mode = request.getPointRoundMode().toUpperCase();
            if ("FLOOR".equals(mode) || "ROUND".equals(mode) || "CEIL".equals(mode)) {
                tenant.setPointRoundMode(mode);
            }
        }

        // ========================================
        // 5. 儲存更新
        // ========================================

        tenant = tenantRepository.save(tenant);
        log.info("店家設定更新成功，租戶 ID：{}", tenantId);

        // ========================================
        // 6. 返回結果
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
                .monthlyPushRemaining((tenant.getMonthlyPushQuota() != null ? tenant.getMonthlyPushQuota() : 100) - (tenant.getMonthlyPushUsed() != null ? tenant.getMonthlyPushUsed() : 0))
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
                // 通知設定
                .notifyNewBooking(tenant.getNotifyNewBooking())
                .notifyBookingReminder(tenant.getNotifyBookingReminder())
                .notifyBookingCancel(tenant.getNotifyBookingCancel())
                // 顧客點數累積設定
                .pointEarnEnabled(tenant.getPointEarnEnabled())
                .pointEarnRate(tenant.getPointEarnRate())
                .pointRoundMode(tenant.getPointRoundMode())
                .build();
    }
}
