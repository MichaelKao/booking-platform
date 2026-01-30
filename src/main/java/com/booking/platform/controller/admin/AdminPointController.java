package com.booking.platform.controller.admin;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.AdjustPointsRequest;
import com.booking.platform.dto.request.ReviewTopUpRequest;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.enums.TopUpStatus;
import com.booking.platform.service.PointTopUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超級管理後台 - 點數管理控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPointController {

    private final PointTopUpService pointTopUpService;

    // TODO: 從認證中取得操作者 ID
    private static final String OPERATOR_ID = "admin";

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得所有儲值申請（分頁）
     */
    @GetMapping("/point-topups")
    public ApiResponse<PageResponse<PointTopUpResponse>> getAllTopUps(
            @RequestParam(required = false) TopUpStatus status,
            @RequestParam(required = false) String tenantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PointTopUpResponse> result = pointTopUpService.getAllTopUps(status, tenantId, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得待審核的儲值申請
     */
    @GetMapping("/point-topups/pending")
    public ApiResponse<List<PointTopUpResponse>> getPendingTopUps() {
        List<PointTopUpResponse> result = pointTopUpService.getPendingTopUps();
        return ApiResponse.ok(result);
    }

    /**
     * 取得待審核數量
     */
    @GetMapping("/point-topups/pending/count")
    public ApiResponse<Long> getPendingCount() {
        long count = pointTopUpService.getPendingCount();
        return ApiResponse.ok(count);
    }

    /**
     * 取得儲值申請詳情
     */
    @GetMapping("/point-topups/{id}")
    public ApiResponse<PointTopUpResponse> getTopUpDetail(@PathVariable String id) {
        PointTopUpResponse result = pointTopUpService.getTopUpDetail(id);
        return ApiResponse.ok(result);
    }

    // ========================================
    // 審核操作
    // ========================================

    /**
     * 審核通過
     */
    @PostMapping("/point-topups/{id}/approve")
    public ApiResponse<PointTopUpResponse> approveTopUp(
            @PathVariable String id,
            @Valid @RequestBody(required = false) ReviewTopUpRequest request
    ) {
        if (request == null) {
            request = new ReviewTopUpRequest();
        }
        PointTopUpResponse result = pointTopUpService.approveTopUp(id, OPERATOR_ID, request);
        return ApiResponse.ok("審核通過", result);
    }

    /**
     * 審核駁回
     */
    @PostMapping("/point-topups/{id}/reject")
    public ApiResponse<PointTopUpResponse> rejectTopUp(
            @PathVariable String id,
            @Valid @RequestBody ReviewTopUpRequest request
    ) {
        PointTopUpResponse result = pointTopUpService.rejectTopUp(id, OPERATOR_ID, request);
        return ApiResponse.ok("審核駁回", result);
    }

    // ========================================
    // 點數調整
    // ========================================

    /**
     * 手動調整租戶點數
     */
    @PostMapping("/tenants/{tenantId}/points/adjust")
    public ApiResponse<Void> adjustTenantPoints(
            @PathVariable String tenantId,
            @Valid @RequestBody AdjustPointsRequest request
    ) {
        pointTopUpService.adjustTenantPoints(tenantId, request, OPERATOR_ID);
        return ApiResponse.ok("點數調整成功", null);
    }
}
