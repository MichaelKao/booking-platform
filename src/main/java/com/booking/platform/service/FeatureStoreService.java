package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.ApplyFeatureRequest;
import com.booking.platform.dto.response.FeatureStoreItemResponse;
import com.booking.platform.entity.system.Feature;
import com.booking.platform.entity.system.TenantFeature;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.enums.FeatureStatus;
import com.booking.platform.repository.FeatureRepository;
import com.booking.platform.repository.TenantFeatureRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 功能商店服務
 *
 * <p>管理店家的功能訂閱
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeatureStoreService {

    // ========================================
    // 依賴注入
    // ========================================

    private final FeatureRepository featureRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final TenantRepository tenantRepository;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得功能商店列表
     *
     * @return 功能列表
     */
    public List<FeatureStoreItemResponse> getFeatureList() {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.debug("取得功能商店列表，租戶 ID：{}", tenantId);

        // ========================================
        // 2. 查詢所有可用功能
        // ========================================

        List<Feature> features = featureRepository.findByIsActiveTrueOrderBySortOrderAsc();

        // ========================================
        // 3. 查詢租戶已訂閱的功能
        // ========================================

        List<TenantFeature> tenantFeatures = tenantFeatureRepository
                .findByTenantIdAndDeletedAtIsNullOrderByFeatureCodeAsc(tenantId);
        Map<FeatureCode, TenantFeature> featureMap = tenantFeatures.stream()
                .collect(Collectors.toMap(TenantFeature::getFeatureCode, tf -> tf));

        // ========================================
        // 4. 組合並返回結果
        // ========================================

        return features.stream()
                .map(feature -> {
                    TenantFeature tf = featureMap.get(feature.getCode());
                    boolean isEnabled = tf != null && tf.isEffective();

                    return FeatureStoreItemResponse.builder()
                            .code(feature.getCode().name())
                            .name(feature.getName())
                            .description(feature.getDescription())
                            .category(feature.getCategory())
                            .monthlyPrice(BigDecimal.valueOf(feature.getMonthlyPoints() != null ? feature.getMonthlyPoints() : 0))
                            .isFree(Boolean.TRUE.equals(feature.getIsFree()))
                            .isEnabled(isEnabled)
                            .subscriptionExpiry(tf != null ? tf.getExpiresAt() : null)
                            .iconUrl(feature.getIcon())
                            .sortOrder(feature.getSortOrder())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 取得單一功能詳情
     *
     * @param code 功能代碼
     * @return 功能詳情
     */
    public FeatureStoreItemResponse getFeatureDetail(String code) {
        String tenantId = TenantContext.getTenantId();

        // 解析功能代碼
        FeatureCode featureCode;
        try {
            featureCode = FeatureCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能");
        }

        Feature feature = featureRepository.findByCode(featureCode)
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能"
                ));

        TenantFeature tf = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElse(null);

        boolean isEnabled = tf != null && tf.isEffective();

        return FeatureStoreItemResponse.builder()
                .code(feature.getCode().name())
                .name(feature.getName())
                .description(feature.getDescription())
                .category(feature.getCategory())
                .monthlyPrice(BigDecimal.valueOf(feature.getMonthlyPoints() != null ? feature.getMonthlyPoints() : 0))
                .isFree(Boolean.TRUE.equals(feature.getIsFree()))
                .isEnabled(isEnabled)
                .subscriptionExpiry(tf != null ? tf.getExpiresAt() : null)
                .iconUrl(feature.getIcon())
                .sortOrder(feature.getSortOrder())
                .build();
    }

    // ========================================
    // 申請功能
    // ========================================

    /**
     * 申請訂閱功能
     *
     * @param code 功能代碼
     * @param request 申請請求
     * @return 功能詳情
     */
    @Transactional
    public FeatureStoreItemResponse applyFeature(String code, ApplyFeatureRequest request) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        int months = request.getMonths() != null ? request.getMonths() : 1;
        log.info("申請訂閱功能，租戶 ID：{}，功能：{}，月數：{}", tenantId, code, months);

        // ========================================
        // 2. 解析並查詢功能
        // ========================================

        FeatureCode featureCode;
        try {
            featureCode = FeatureCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能");
        }

        Feature feature = featureRepository.findByCode(featureCode)
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能"
                ));

        // ========================================
        // 3. 查詢租戶
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶資料"
                ));

        // ========================================
        // 4. 計算費用並扣除點數
        // ========================================

        int monthlyPoints = feature.getMonthlyPoints() != null ? feature.getMonthlyPoints() : 0;
        int totalCost = monthlyPoints * months;
        boolean isFree = Boolean.TRUE.equals(feature.getIsFree()) || monthlyPoints == 0;

        if (!isFree) {
            // 檢查點數餘額
            BigDecimal costDecimal = BigDecimal.valueOf(totalCost);
            if (tenant.getPointBalance().compareTo(costDecimal) < 0) {
                throw new BusinessException(ErrorCode.POINT_INSUFFICIENT,
                        "點數不足，需要 " + totalCost + " 點，目前餘額 " + tenant.getPointBalance() + " 點");
            }

            // 扣除點數
            tenant.deductPoints(costDecimal);
            tenantRepository.save(tenant);
        }

        // ========================================
        // 5. 建立或更新訂閱
        // ========================================

        // 查詢功能訂閱記錄（包含已刪除的，用於重新訂閱）
        TenantFeature tf = tenantFeatureRepository
                .findByTenantIdAndFeatureCode(tenantId, featureCode)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry;

        if (tf == null) {
            // 全新訂閱
            expiry = isFree ? null : now.plusMonths(months);
            tf = TenantFeature.builder()
                    .featureCode(featureCode)
                    .status(FeatureStatus.ENABLED)
                    .enabledAt(now)
                    .expiresAt(expiry)
                    .build();
            tf.setTenantId(tenantId);
        } else if (tf.getDeletedAt() != null) {
            // 重新訂閱（之前取消過）
            expiry = isFree ? null : now.plusMonths(months);
            tf.setDeletedAt(null);  // 清除刪除標記
            tf.setStatus(FeatureStatus.ENABLED);
            tf.setEnabledAt(now);
            tf.setExpiresAt(expiry);
        } else {
            // 續訂（目前訂閱中）
            LocalDateTime baseDate = tf.getExpiresAt() != null && tf.getExpiresAt().isAfter(now)
                    ? tf.getExpiresAt() : now;
            expiry = isFree ? null : baseDate.plusMonths(months);

            tf.setStatus(FeatureStatus.ENABLED);
            tf.setEnabledAt(now);
            if (!isFree) {
                tf.setExpiresAt(expiry);
            }
        }

        tenantFeatureRepository.save(tf);

        log.info("功能訂閱成功，租戶 ID：{}，功能：{}，到期時間：{}",
                tenantId, code, tf.getExpiresAt());

        // ========================================
        // 6. 返回結果
        // ========================================

        return FeatureStoreItemResponse.builder()
                .code(feature.getCode().name())
                .name(feature.getName())
                .description(feature.getDescription())
                .category(feature.getCategory())
                .monthlyPrice(BigDecimal.valueOf(monthlyPoints))
                .isFree(isFree)
                .isEnabled(true)
                .subscriptionExpiry(tf.getExpiresAt())
                .iconUrl(feature.getIcon())
                .sortOrder(feature.getSortOrder())
                .build();
    }

    // ========================================
    // 取消功能訂閱
    // ========================================

    /**
     * 取消訂閱功能
     *
     * @param code 功能代碼
     * @return 功能詳情
     */
    @Transactional
    public FeatureStoreItemResponse cancelFeature(String code) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================

        String tenantId = TenantContext.getTenantId();
        log.info("取消訂閱功能，租戶 ID：{}，功能：{}", tenantId, code);

        // ========================================
        // 2. 解析並查詢功能
        // ========================================

        FeatureCode featureCode;
        try {
            featureCode = FeatureCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能");
        }

        Feature feature = featureRepository.findByCode(featureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能"
                ));

        // ========================================
        // 3. 查詢並更新訂閱狀態
        // ========================================

        TenantFeature tf = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FEATURE_NOT_SUBSCRIBED, "尚未訂閱此功能"
                ));

        // 設為已過期（取消訂閱）
        tf.setStatus(FeatureStatus.EXPIRED);
        tf.setDeletedAt(LocalDateTime.now());
        tenantFeatureRepository.save(tf);

        log.info("功能取消訂閱成功，租戶 ID：{}，功能：{}", tenantId, code);

        // ========================================
        // 4. 返回結果
        // ========================================

        return FeatureStoreItemResponse.builder()
                .code(feature.getCode().name())
                .name(feature.getName())
                .description(feature.getDescription())
                .category(feature.getCategory())
                .monthlyPrice(BigDecimal.valueOf(feature.getMonthlyPoints() != null ? feature.getMonthlyPoints() : 0))
                .isFree(Boolean.TRUE.equals(feature.getIsFree()))
                .isEnabled(false)
                .subscriptionExpiry(null)
                .iconUrl(feature.getIcon())
                .sortOrder(feature.getSortOrder())
                .build();
    }
}
