package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.dto.request.EnableFeatureRequest;
import com.booking.platform.dto.request.UpdateFeatureRequest;
import com.booking.platform.dto.response.FeatureResponse;
import com.booking.platform.dto.response.TenantFeatureResponse;
import com.booking.platform.entity.system.Feature;
import com.booking.platform.entity.system.TenantFeature;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.enums.FeatureStatus;
import com.booking.platform.mapper.FeatureMapper;
import com.booking.platform.repository.FeatureRepository;
import com.booking.platform.repository.TenantFeatureRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 功能管理服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeatureService {

    private final FeatureRepository featureRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final TenantRepository tenantRepository;
    private final FeatureMapper featureMapper;

    // ========================================
    // 功能定義查詢
    // ========================================

    /**
     * 取得所有功能定義（包含停用的），供管理員使用
     */
    public List<FeatureResponse> getAllFeatures() {
        return featureRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(featureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得啟用中的功能定義
     */
    public List<FeatureResponse> getActiveFeatures() {
        return featureRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(featureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 依分類取得功能
     */
    public List<FeatureResponse> getFeaturesByCategory(String category) {
        return featureRepository.findByCategory(category)
                .stream()
                .map(featureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得免費功能
     */
    public List<FeatureResponse> getFreeFeatures() {
        return featureRepository.findByIsFree(true)
                .stream()
                .map(featureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得付費功能
     */
    public List<FeatureResponse> getPaidFeatures() {
        return featureRepository.findByIsFree(false)
                .stream()
                .map(featureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 依功能代碼取得功能
     */
    public FeatureResponse getFeatureByCode(FeatureCode code) {
        Feature feature = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能：" + code
                ));
        return featureMapper.toResponse(feature);
    }

    /**
     * 更新功能定義
     */
    @Transactional
    public FeatureResponse updateFeature(FeatureCode code, UpdateFeatureRequest request) {
        log.info("更新功能定義，功能代碼：{}", code);

        Feature feature = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能：" + code
                ));

        // ========================================
        // 更新功能屬性
        // ========================================

        if (request.getName() != null) {
            feature.setName(request.getName());
        }

        if (request.getDescription() != null) {
            feature.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            feature.setIsActive(request.getIsActive());
        }

        if (request.getIsFree() != null) {
            feature.setIsFree(request.getIsFree());
            // 如果設為免費，清除月費
            if (request.getIsFree()) {
                feature.setMonthlyPoints(0);
            }
        }

        if (request.getMonthlyPoints() != null) {
            feature.setMonthlyPoints(request.getMonthlyPoints());
        }

        if (request.getIcon() != null) {
            feature.setIcon(request.getIcon());
        }

        if (request.getCategory() != null) {
            feature.setCategory(request.getCategory());
        }

        if (request.getSortOrder() != null) {
            feature.setSortOrder(request.getSortOrder());
        }

        feature = featureRepository.save(feature);

        log.info("功能定義更新成功，功能代碼：{}", code);

        return featureMapper.toResponse(feature);
    }

    // ========================================
    // 租戶功能查詢
    // ========================================

    /**
     * 取得租戶所有功能狀態
     */
    public List<TenantFeatureResponse> getTenantFeatures(String tenantId) {
        // 驗證租戶存在
        validateTenantExists(tenantId);

        return tenantFeatureRepository.findByTenantIdAndDeletedAtIsNullOrderByFeatureCodeAsc(tenantId)
                .stream()
                .map(tf -> {
                    Feature feature = featureRepository.findByCode(tf.getFeatureCode()).orElse(null);
                    return featureMapper.toTenantFeatureResponse(tf, feature);
                })
                .collect(Collectors.toList());
    }

    /**
     * 取得租戶指定功能狀態
     */
    public TenantFeatureResponse getTenantFeature(String tenantId, FeatureCode featureCode) {
        validateTenantExists(tenantId);

        TenantFeature tenantFeature = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElse(null);

        if (tenantFeature == null) {
            // 如果沒有記錄，返回預設狀態
            Feature feature = featureRepository.findByCode(featureCode).orElse(null);
            return TenantFeatureResponse.builder()
                    .tenantId(tenantId)
                    .featureCode(featureCode)
                    .featureName(featureCode.getName())
                    .featureDescription(featureCode.getDescription())
                    .status(FeatureStatus.AVAILABLE)
                    .isFree(featureCode.isFree())
                    .monthlyPoints(featureCode.getMonthlyPoints())
                    .isEffective(false)
                    .build();
        }

        Feature feature = featureRepository.findByCode(featureCode).orElse(null);
        return featureMapper.toTenantFeatureResponse(tenantFeature, feature);
    }

    /**
     * 檢查租戶是否啟用指定功能
     */
    public boolean isFeatureEnabled(String tenantId, FeatureCode featureCode) {
        // 免費功能預設啟用
        if (featureCode.isFree()) {
            return true;
        }

        return tenantFeatureRepository.isFeatureEnabled(
                tenantId, featureCode, LocalDateTime.now()
        );
    }

    /**
     * 檢查功能是否啟用（如未啟用則拋出例外）
     */
    public void checkFeatureEnabled(String tenantId, FeatureCode featureCode) {
        if (!isFeatureEnabled(tenantId, featureCode)) {
            throw new BusinessException(
                    ErrorCode.TENANT_FEATURE_NOT_ENABLED,
                    "功能「" + featureCode.getName() + "」未開通"
            );
        }
    }

    // ========================================
    // 功能啟用/停用（管理員操作）
    // ========================================

    /**
     * 啟用租戶功能
     */
    @Transactional
    public TenantFeatureResponse enableFeature(
            String tenantId,
            FeatureCode featureCode,
            EnableFeatureRequest request,
            String operatorId
    ) {
        log.info("啟用租戶功能，租戶：{}，功能：{}，操作者：{}", tenantId, featureCode, operatorId);

        validateTenantExists(tenantId);

        // 取得或建立租戶功能記錄（包含已刪除的，用於重新啟用）
        TenantFeature tenantFeature = tenantFeatureRepository
                .findByTenantIdAndFeatureCode(tenantId, featureCode)
                .orElse(null);

        if (tenantFeature == null) {
            // 全新記錄
            tenantFeature = TenantFeature.builder()
                    .featureCode(featureCode)
                    .status(FeatureStatus.AVAILABLE)
                    .build();
            tenantFeature.setTenantId(tenantId);
        } else if (tenantFeature.getDeletedAt() != null) {
            // 重新啟用已刪除的記錄
            tenantFeature.setDeletedAt(null);
        }

        // 啟用功能
        tenantFeature.enable(operatorId, request.getExpiresAt());

        // 設定自訂價格
        if (request.getCustomMonthlyPoints() != null) {
            tenantFeature.setCustomMonthlyPoints(request.getCustomMonthlyPoints());
        }

        // 設定備註
        if (request.getNote() != null) {
            tenantFeature.setNote(request.getNote());
        }

        tenantFeature = tenantFeatureRepository.save(tenantFeature);

        log.info("租戶功能啟用成功，租戶：{}，功能：{}", tenantId, featureCode);

        Feature feature = featureRepository.findByCode(featureCode).orElse(null);
        return featureMapper.toTenantFeatureResponse(tenantFeature, feature);
    }

    /**
     * 停用租戶功能
     */
    @Transactional
    public TenantFeatureResponse disableFeature(String tenantId, FeatureCode featureCode, String operatorId) {
        log.info("停用租戶功能，租戶：{}，功能：{}，操作者：{}", tenantId, featureCode, operatorId);

        validateTenantExists(tenantId);

        TenantFeature tenantFeature = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能記錄"
                ));

        tenantFeature.disable();
        tenantFeature = tenantFeatureRepository.save(tenantFeature);

        log.info("租戶功能停用成功，租戶：{}，功能：{}", tenantId, featureCode);

        Feature feature = featureRepository.findByCode(featureCode).orElse(null);
        return featureMapper.toTenantFeatureResponse(tenantFeature, feature);
    }

    /**
     * 凍結租戶功能
     */
    @Transactional
    public TenantFeatureResponse suspendFeature(String tenantId, FeatureCode featureCode, String operatorId) {
        log.info("凍結租戶功能，租戶：{}，功能：{}，操作者：{}", tenantId, featureCode, operatorId);

        validateTenantExists(tenantId);

        TenantFeature tenantFeature = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能記錄"
                ));

        tenantFeature.suspend();
        tenantFeature = tenantFeatureRepository.save(tenantFeature);

        log.info("租戶功能凍結成功，租戶：{}，功能：{}", tenantId, featureCode);

        Feature feature = featureRepository.findByCode(featureCode).orElse(null);
        return featureMapper.toTenantFeatureResponse(tenantFeature, feature);
    }

    /**
     * 解凍租戶功能
     */
    @Transactional
    public TenantFeatureResponse unsuspendFeature(String tenantId, FeatureCode featureCode, String operatorId) {
        log.info("解凍租戶功能，租戶：{}，功能：{}，操作者：{}", tenantId, featureCode, operatorId);

        validateTenantExists(tenantId);

        TenantFeature tenantFeature = tenantFeatureRepository
                .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, featureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.FEATURE_NOT_FOUND, "找不到指定的功能記錄"
                ));

        tenantFeature.unsuspend();
        tenantFeature = tenantFeatureRepository.save(tenantFeature);

        log.info("租戶功能解凍成功，租戶：{}，功能：{}", tenantId, featureCode);

        Feature feature = featureRepository.findByCode(featureCode).orElse(null);
        return featureMapper.toTenantFeatureResponse(tenantFeature, feature);
    }

    // ========================================
    // 批次操作
    // ========================================

    /**
     * 批次啟用多個租戶的功能
     */
    @Transactional
    public void batchEnableFeature(
            List<String> tenantIds,
            FeatureCode featureCode,
            EnableFeatureRequest request,
            String operatorId
    ) {
        log.info("批次啟用功能，租戶數：{}，功能：{}", tenantIds.size(), featureCode);

        for (String tenantId : tenantIds) {
            try {
                enableFeature(tenantId, featureCode, request, operatorId);
            } catch (Exception e) {
                log.error("批次啟用功能失敗，租戶：{}，功能：{}，錯誤：{}", tenantId, featureCode, e.getMessage());
            }
        }
    }

    /**
     * 批次停用多個租戶的功能
     */
    @Transactional
    public void batchDisableFeature(List<String> tenantIds, FeatureCode featureCode, String operatorId) {
        log.info("批次停用功能，租戶數：{}，功能：{}", tenantIds.size(), featureCode);

        for (String tenantId : tenantIds) {
            try {
                disableFeature(tenantId, featureCode, operatorId);
            } catch (Exception e) {
                log.error("批次停用功能失敗，租戶：{}，功能：{}，錯誤：{}", tenantId, featureCode, e.getMessage());
            }
        }
    }

    // ========================================
    // 初始化功能定義
    // ========================================

    /**
     * 初始化所有功能定義到資料庫
     */
    @Transactional
    public void initializeFeatures() {
        log.info("開始初始化功能定義...");

        int sortOrder = 1;
        for (FeatureCode code : FeatureCode.values()) {
            if (!featureRepository.existsByCode(code)) {
                Feature feature = Feature.builder()
                        .code(code)
                        .name(code.getName())
                        .description(code.getDescription())
                        .isFree(code.isFree())
                        .monthlyPoints(code.getMonthlyPoints())
                        .sortOrder(sortOrder++)
                        .isActive(true)
                        .build();
                featureRepository.save(feature);
                log.info("建立功能定義：{}", code);
            }
        }

        log.info("功能定義初始化完成");
    }

    // ========================================
    // 初始化租戶免費功能
    // ========================================

    /**
     * 為新租戶啟用所有免費功能
     */
    @Transactional
    public void initializeTenantFreeFeatures(String tenantId) {
        log.info("初始化租戶免費功能，租戶：{}", tenantId);

        Arrays.stream(FeatureCode.values())
                .filter(FeatureCode::isFree)
                .forEach(code -> {
                    if (!tenantFeatureRepository.existsByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, code)) {
                        TenantFeature tenantFeature = TenantFeature.builder()
                                .featureCode(code)
                                .status(FeatureStatus.ENABLED)
                                .enabledAt(LocalDateTime.now())
                                .build();
                        tenantFeature.setTenantId(tenantId);
                        tenantFeatureRepository.save(tenantFeature);
                    }
                });

        log.info("租戶免費功能初始化完成，租戶：{}", tenantId);
    }

    // ========================================
    // 私有方法
    // ========================================

    private void validateTenantExists(String tenantId) {
        if (!tenantRepository.existsByIdAndDeletedAtIsNull(tenantId)) {
            throw new ResourceNotFoundException(
                    ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
            );
        }
    }
}
