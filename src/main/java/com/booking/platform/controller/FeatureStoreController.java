package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.request.ApplyFeatureRequest;
import com.booking.platform.dto.response.FeatureStoreItemResponse;
import com.booking.platform.service.FeatureStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 功能商店 Controller
 *
 * <p>管理店家的功能訂閱
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/feature-store")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FeatureStoreController {

    // ========================================
    // 依賴注入
    // ========================================

    private final FeatureStoreService featureStoreService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得功能商店列表
     *
     * @return 功能列表
     */
    @GetMapping
    public ApiResponse<List<FeatureStoreItemResponse>> getFeatureList() {
        log.info("收到取得功能商店列表請求");
        return ApiResponse.ok(featureStoreService.getFeatureList());
    }

    /**
     * 取得功能詳情
     *
     * @param code 功能代碼
     * @return 功能詳情
     */
    @GetMapping("/{code}")
    public ApiResponse<FeatureStoreItemResponse> getFeatureDetail(@PathVariable String code) {
        log.info("收到取得功能詳情請求，功能：{}", code);
        return ApiResponse.ok(featureStoreService.getFeatureDetail(code));
    }

    // ========================================
    // 申請 API
    // ========================================

    /**
     * 申請訂閱功能
     *
     * @param code 功能代碼
     * @param request 申請請求
     * @return 功能詳情
     */
    @PostMapping("/{code}/apply")
    public ApiResponse<FeatureStoreItemResponse> applyFeature(
            @PathVariable String code,
            @Valid @RequestBody(required = false) ApplyFeatureRequest request
    ) {
        log.info("收到申請訂閱功能請求，功能：{}", code);

        if (request == null) {
            request = ApplyFeatureRequest.builder().months(1).build();
        }

        return ApiResponse.ok("功能訂閱成功", featureStoreService.applyFeature(code, request));
    }

    /**
     * 取消訂閱功能
     *
     * @param code 功能代碼
     * @return 功能詳情
     */
    @PostMapping("/{code}/cancel")
    public ApiResponse<FeatureStoreItemResponse> cancelFeature(@PathVariable String code) {
        log.info("收到取消訂閱功能請求，功能：{}", code);
        return ApiResponse.ok("功能取消訂閱成功", featureStoreService.cancelFeature(code));
    }
}
