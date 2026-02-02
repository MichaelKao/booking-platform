package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateMarketingPushRequest;
import com.booking.platform.dto.response.MarketingPushResponse;
import com.booking.platform.enums.MarketingPushStatus;
import com.booking.platform.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 行銷推播 Controller
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MarketingController {

    private final MarketingService marketingService;

    // ========================================
    // 推播管理
    // ========================================

    /**
     * 建立推播
     */
    @PostMapping("/pushes")
    public ApiResponse<MarketingPushResponse> createPush(
            @Valid @RequestBody CreateMarketingPushRequest request
    ) {
        MarketingPushResponse response = marketingService.createPush(request);
        return ApiResponse.ok("推播建立成功", response);
    }

    /**
     * 取得推播列表
     */
    @GetMapping("/pushes")
    public ApiResponse<PageResponse<MarketingPushResponse>> getPushList(
            @RequestParam(required = false) MarketingPushStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        PageResponse<MarketingPushResponse> response = marketingService.getPushList(status, page, size);
        return ApiResponse.ok(response);
    }

    /**
     * 取得推播詳情
     */
    @GetMapping("/pushes/{id}")
    public ApiResponse<MarketingPushResponse> getPush(@PathVariable String id) {
        MarketingPushResponse response = marketingService.getPush(id);
        return ApiResponse.ok(response);
    }

    /**
     * 立即發送推播
     */
    @PostMapping("/pushes/{id}/send")
    public ApiResponse<MarketingPushResponse> sendPush(@PathVariable String id) {
        MarketingPushResponse response = marketingService.sendPush(id);
        return ApiResponse.ok("推播已開始發送", response);
    }

    /**
     * 取消推播
     */
    @PostMapping("/pushes/{id}/cancel")
    public ApiResponse<MarketingPushResponse> cancelPush(@PathVariable String id) {
        MarketingPushResponse response = marketingService.cancelPush(id);
        return ApiResponse.ok("推播已取消", response);
    }

    /**
     * 刪除推播
     */
    @DeleteMapping("/pushes/{id}")
    public ApiResponse<Void> deletePush(@PathVariable String id) {
        marketingService.deletePush(id);
        return ApiResponse.ok("推播已刪除", null);
    }
}
