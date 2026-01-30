package com.booking.platform.controller.admin;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.request.EnableFeatureRequest;
import com.booking.platform.dto.response.FeatureResponse;
import com.booking.platform.dto.response.TenantFeatureResponse;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.service.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超級管理後台 - 功能管理控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminFeatureController {

    private final FeatureService featureService;

    // TODO: 從認證中取得操作者 ID
    private static final String OPERATOR_ID = "admin";

    // ========================================
    // 功能定義查詢
    // ========================================

    /**
     * 取得所有功能定義
     */
    @GetMapping("/features")
    public ApiResponse<List<FeatureResponse>> getAllFeatures() {
        List<FeatureResponse> result = featureService.getAllFeatures();
        return ApiResponse.ok(result);
    }

    /**
     * 取得免費功能
     */
    @GetMapping("/features/free")
    public ApiResponse<List<FeatureResponse>> getFreeFeatures() {
        List<FeatureResponse> result = featureService.getFreeFeatures();
        return ApiResponse.ok(result);
    }

    /**
     * 取得付費功能
     */
    @GetMapping("/features/paid")
    public ApiResponse<List<FeatureResponse>> getPaidFeatures() {
        List<FeatureResponse> result = featureService.getPaidFeatures();
        return ApiResponse.ok(result);
    }

    /**
     * 初始化功能定義
     */
    @PostMapping("/features/initialize")
    public ApiResponse<Void> initializeFeatures() {
        featureService.initializeFeatures();
        return ApiResponse.ok("功能定義初始化成功", null);
    }

    // ========================================
    // 租戶功能管理
    // ========================================

    /**
     * 取得租戶所有功能狀態
     */
    @GetMapping("/tenants/{tenantId}/features")
    public ApiResponse<List<TenantFeatureResponse>> getTenantFeatures(@PathVariable String tenantId) {
        List<TenantFeatureResponse> result = featureService.getTenantFeatures(tenantId);
        return ApiResponse.ok(result);
    }

    /**
     * 取得租戶指定功能狀態
     */
    @GetMapping("/tenants/{tenantId}/features/{featureCode}")
    public ApiResponse<TenantFeatureResponse> getTenantFeature(
            @PathVariable String tenantId,
            @PathVariable FeatureCode featureCode
    ) {
        TenantFeatureResponse result = featureService.getTenantFeature(tenantId, featureCode);
        return ApiResponse.ok(result);
    }

    /**
     * 啟用租戶功能
     */
    @PostMapping("/tenants/{tenantId}/features/{featureCode}/enable")
    public ApiResponse<TenantFeatureResponse> enableFeature(
            @PathVariable String tenantId,
            @PathVariable FeatureCode featureCode,
            @Valid @RequestBody(required = false) EnableFeatureRequest request
    ) {
        if (request == null) {
            request = new EnableFeatureRequest();
        }
        TenantFeatureResponse result = featureService.enableFeature(
                tenantId, featureCode, request, OPERATOR_ID
        );
        return ApiResponse.ok("功能已啟用", result);
    }

    /**
     * 停用租戶功能
     */
    @PostMapping("/tenants/{tenantId}/features/{featureCode}/disable")
    public ApiResponse<TenantFeatureResponse> disableFeature(
            @PathVariable String tenantId,
            @PathVariable FeatureCode featureCode
    ) {
        TenantFeatureResponse result = featureService.disableFeature(tenantId, featureCode, OPERATOR_ID);
        return ApiResponse.ok("功能已停用", result);
    }

    /**
     * 凍結租戶功能
     */
    @PostMapping("/tenants/{tenantId}/features/{featureCode}/suspend")
    public ApiResponse<TenantFeatureResponse> suspendFeature(
            @PathVariable String tenantId,
            @PathVariable FeatureCode featureCode
    ) {
        TenantFeatureResponse result = featureService.suspendFeature(tenantId, featureCode, OPERATOR_ID);
        return ApiResponse.ok("功能已凍結", result);
    }

    /**
     * 解凍租戶功能
     */
    @PostMapping("/tenants/{tenantId}/features/{featureCode}/unsuspend")
    public ApiResponse<TenantFeatureResponse> unsuspendFeature(
            @PathVariable String tenantId,
            @PathVariable FeatureCode featureCode
    ) {
        TenantFeatureResponse result = featureService.unsuspendFeature(tenantId, featureCode, OPERATOR_ID);
        return ApiResponse.ok("功能已解凍", result);
    }

    // ========================================
    // 批次操作
    // ========================================

    /**
     * 批次啟用功能
     */
    @PostMapping("/tenants/batch/features/{featureCode}/enable")
    public ApiResponse<Void> batchEnableFeature(
            @PathVariable FeatureCode featureCode,
            @RequestBody List<String> tenantIds,
            @Valid @RequestBody(required = false) EnableFeatureRequest request
    ) {
        if (request == null) {
            request = new EnableFeatureRequest();
        }
        featureService.batchEnableFeature(tenantIds, featureCode, request, OPERATOR_ID);
        return ApiResponse.ok("批次啟用成功", null);
    }

    /**
     * 批次停用功能
     */
    @PostMapping("/tenants/batch/features/{featureCode}/disable")
    public ApiResponse<Void> batchDisableFeature(
            @PathVariable FeatureCode featureCode,
            @RequestBody List<String> tenantIds
    ) {
        featureService.batchDisableFeature(tenantIds, featureCode, OPERATOR_ID);
        return ApiResponse.ok("批次停用成功", null);
    }
}
