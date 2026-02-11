package com.booking.platform.service;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.SetupStatusResponse;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.line.LineConfigStatus;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 店家設定完成狀態服務
 *
 * <p>聚合多個 Repository 查詢，計算店家設定的完成度
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SetupStatusService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;
    private final StaffRepository staffRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final TenantLineConfigRepository tenantLineConfigRepository;
    private final BookingRepository bookingRepository;

    // ========================================
    // 常數
    // ========================================

    private static final int TOTAL_STEPS = 5;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得當前店家的設定完成狀態
     *
     * @return 設定完成狀態
     */
    public SetupStatusResponse getSetupStatus() {
        // ========================================
        // 1. 取得租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("查詢設定完成狀態，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢各項設定狀態
        // ========================================

        // 基本資訊：店名、電話、地址至少填 2 項
        boolean hasBasicInfo = checkBasicInfo(tenantId);

        // 員工數量
        long staffCount = staffRepository.countByTenantIdAndDeletedAtIsNull(tenantId);

        // 服務項目數量
        long serviceCount = serviceItemRepository.countByTenantIdAndDeletedAtIsNull(tenantId);

        // 營業時間：檢查是否有修改過預設值（有員工排班即視為已設定）
        boolean hasBusinessHours = checkBusinessHours(tenantId);

        // LINE Bot 設定
        boolean lineConfigured = tenantLineConfigRepository.findByTenantId(tenantId)
                .map(config -> config.getStatus() == LineConfigStatus.ACTIVE)
                .orElse(false);

        // 是否已有預約
        boolean hasBookings = bookingRepository.existsByTenantIdAndDeletedAtIsNull(tenantId);

        // ========================================
        // 3. 計算完成度
        // ========================================

        int completedSteps = 0;
        if (hasBasicInfo) completedSteps++;
        if (staffCount > 0) completedSteps++;
        if (serviceCount > 0) completedSteps++;
        if (hasBusinessHours) completedSteps++;
        if (lineConfigured) completedSteps++;

        int completionPercentage = (int) Math.round((double) completedSteps / TOTAL_STEPS * 100);

        // ========================================
        // 4. 回傳結果
        // ========================================

        return SetupStatusResponse.builder()
                .hasBasicInfo(hasBasicInfo)
                .hasBusinessHours(hasBusinessHours)
                .staffCount(staffCount)
                .serviceCount(serviceCount)
                .lineConfigured(lineConfigured)
                .hasBookings(hasBookings)
                .completionPercentage(completionPercentage)
                .totalSteps(TOTAL_STEPS)
                .completedSteps(completedSteps)
                .build();
    }

    // ========================================
    // 工具方法
    // ========================================

    /**
     * 檢查基本資訊是否完善（電話、地址至少填 1 項）
     */
    private boolean checkBasicInfo(String tenantId) {
        return tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .map(tenant -> {
                    int filledCount = 0;
                    if (hasValue(tenant.getPhone())) filledCount++;
                    if (hasValue(tenant.getAddress())) filledCount++;
                    if (hasValue(tenant.getDescription())) filledCount++;
                    return filledCount >= 1;
                })
                .orElse(false);
    }

    /**
     * 檢查營業時間是否已設定（有修改過預設值即算已設定）
     */
    private boolean checkBusinessHours(String tenantId) {
        return tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .map(tenant -> {
                    // 如果已修改過營業時間或已設定公休日，視為已設定
                    boolean hasCustomHours = tenant.getBusinessStartTime() != null
                            && tenant.getBusinessEndTime() != null;
                    boolean hasClosedDays = hasValue(tenant.getClosedDays());
                    return hasCustomHours || hasClosedDays;
                })
                .orElse(false);
    }

    /**
     * 檢查字串是否有值
     */
    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
