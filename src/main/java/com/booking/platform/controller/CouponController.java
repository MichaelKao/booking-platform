package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateCouponRequest;
import com.booking.platform.dto.request.IssueCouponRequest;
import com.booking.platform.dto.response.CouponInstanceResponse;
import com.booking.platform.dto.response.CouponResponse;
import com.booking.platform.enums.CouponInstanceStatus;
import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.CouponType;
import com.booking.platform.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 票券控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ========================================
    // 票券定義查詢 API
    // ========================================

    /**
     * 取得票券列表（分頁）
     */
    @GetMapping
    public ApiResponse<PageResponse<CouponResponse>> getList(
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) CouponType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CouponResponse> result = couponService.getList(status, type, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得票券詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<CouponResponse> getDetail(@PathVariable String id) {
        CouponResponse result = couponService.getDetail(id);
        return ApiResponse.ok(result);
    }

    /**
     * 取得可發放的票券
     */
    @GetMapping("/available")
    public ApiResponse<List<CouponResponse>> getAvailableCoupons() {
        List<CouponResponse> result = couponService.getAvailableCoupons();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 票券定義寫入 API
    // ========================================

    /**
     * 建立票券
     */
    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        CouponResponse result = couponService.create(request);
        return ApiResponse.ok("票券建立成功", result);
    }

    /**
     * 更新票券
     */
    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateCouponRequest request
    ) {
        CouponResponse result = couponService.update(id, request);
        return ApiResponse.ok("票券更新成功", result);
    }

    /**
     * 刪除票券
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        couponService.delete(id);
        return ApiResponse.ok("票券刪除成功", null);
    }

    // ========================================
    // 票券狀態操作 API
    // ========================================

    /**
     * 發布票券
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<CouponResponse> publish(@PathVariable String id) {
        CouponResponse result = couponService.publish(id);
        return ApiResponse.ok("票券已發布", result);
    }

    /**
     * 暫停票券
     */
    @PostMapping("/{id}/pause")
    public ApiResponse<CouponResponse> pause(@PathVariable String id) {
        CouponResponse result = couponService.pause(id);
        return ApiResponse.ok("票券已暫停", result);
    }

    /**
     * 恢復票券
     */
    @PostMapping("/{id}/resume")
    public ApiResponse<CouponResponse> resume(@PathVariable String id) {
        CouponResponse result = couponService.resume(id);
        return ApiResponse.ok("票券已恢復", result);
    }

    // ========================================
    // 票券發放 API
    // ========================================

    /**
     * 發放票券給顧客
     */
    @PostMapping("/{id}/issue")
    public ApiResponse<CouponInstanceResponse> issue(
            @PathVariable String id,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        CouponInstanceResponse result = couponService.issueCoupon(id, request);
        return ApiResponse.ok("票券發放成功", result);
    }

    // ========================================
    // 票券核銷 API
    // ========================================

    /**
     * 核銷票券（依實例 ID）
     */
    @PostMapping("/instances/{instanceId}/redeem")
    public ApiResponse<CouponInstanceResponse> redeem(
            @PathVariable String instanceId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) java.math.BigDecimal orderAmount
    ) {
        CouponInstanceResponse result = couponService.redeemCoupon(instanceId, orderId, orderAmount);
        return ApiResponse.ok("票券核銷成功", result);
    }

    /**
     * 核銷票券（依代碼）
     */
    @PostMapping("/redeem-by-code")
    public ApiResponse<CouponInstanceResponse> redeemByCode(
            @RequestParam String code,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) java.math.BigDecimal orderAmount
    ) {
        CouponInstanceResponse result = couponService.redeemByCode(code, orderId, orderAmount);
        return ApiResponse.ok("票券核銷成功", result);
    }

    // ========================================
    // 顧客票券查詢 API
    // ========================================

    /**
     * 取得顧客的票券（分頁）
     */
    @GetMapping("/customers/{customerId}")
    public ApiResponse<PageResponse<CouponInstanceResponse>> getCustomerCoupons(
            @PathVariable String customerId,
            @RequestParam(required = false) CouponInstanceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CouponInstanceResponse> result = couponService.getCustomerCoupons(customerId, status, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得顧客可用的票券
     */
    @GetMapping("/customers/{customerId}/usable")
    public ApiResponse<List<CouponInstanceResponse>> getCustomerUsableCoupons(@PathVariable String customerId) {
        List<CouponInstanceResponse> result = couponService.getCustomerUsableCoupons(customerId);
        return ApiResponse.ok(result);
    }
}
