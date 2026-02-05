package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.response.ProductOrderResponse;
import com.booking.platform.enums.ProductOrderStatus;
import com.booking.platform.service.ProductOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品訂單 Controller
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/product-orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductOrderController {

    private final ProductOrderService orderService;

    /**
     * 取得訂單列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductOrderResponse>> getList(
            @RequestParam(required = false) ProductOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(orderService.getList(status, pageable));
    }

    /**
     * 取得訂單詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductOrderResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    /**
     * 取得待處理訂單數量
     */
    @GetMapping("/pending/count")
    public ApiResponse<Long> getPendingCount() {
        return ApiResponse.ok(orderService.getPendingCount());
    }

    /**
     * 取得今日統計
     */
    @GetMapping("/today/stats")
    public ApiResponse<ProductOrderService.TodayStats> getTodayStats() {
        return ApiResponse.ok(orderService.getTodayStats());
    }

    /**
     * 確認訂單
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<ProductOrderResponse> confirm(@PathVariable String id) {
        return ApiResponse.ok("訂單已確認", orderService.confirm(id));
    }

    /**
     * 完成訂單（取貨）
     */
    @PostMapping("/{id}/complete")
    public ApiResponse<ProductOrderResponse> complete(@PathVariable String id) {
        return ApiResponse.ok("訂單已完成", orderService.complete(id));
    }

    /**
     * 取消訂單
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ProductOrderResponse> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.ok("訂單已取消", orderService.cancel(id, reason));
    }
}
