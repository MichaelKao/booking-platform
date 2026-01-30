package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.request.CreateMembershipLevelRequest;
import com.booking.platform.dto.response.MembershipLevelResponse;
import com.booking.platform.service.MembershipLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 會員等級管理控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/membership-levels")
@RequiredArgsConstructor
public class MembershipLevelController {

    private final MembershipLevelService membershipLevelService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得所有會員等級
     */
    @GetMapping
    public ApiResponse<List<MembershipLevelResponse>> getList() {
        List<MembershipLevelResponse> result = membershipLevelService.getList();
        return ApiResponse.ok(result);
    }

    /**
     * 取得會員等級詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<MembershipLevelResponse> getDetail(@PathVariable String id) {
        MembershipLevelResponse result = membershipLevelService.getDetail(id);
        return ApiResponse.ok(result);
    }

    /**
     * 取得預設會員等級
     */
    @GetMapping("/default")
    public ApiResponse<MembershipLevelResponse> getDefaultLevel() {
        MembershipLevelResponse result = membershipLevelService.getDefaultLevel();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立會員等級
     */
    @PostMapping
    public ApiResponse<MembershipLevelResponse> create(@Valid @RequestBody CreateMembershipLevelRequest request) {
        MembershipLevelResponse result = membershipLevelService.create(request);
        return ApiResponse.ok("會員等級建立成功", result);
    }

    /**
     * 更新會員等級
     */
    @PutMapping("/{id}")
    public ApiResponse<MembershipLevelResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateMembershipLevelRequest request
    ) {
        MembershipLevelResponse result = membershipLevelService.update(id, request);
        return ApiResponse.ok("會員等級更新成功", result);
    }

    /**
     * 刪除會員等級
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        membershipLevelService.delete(id);
        return ApiResponse.ok("會員等級刪除成功", null);
    }

    // ========================================
    // 狀態操作 API
    // ========================================

    /**
     * 切換會員等級啟用狀態
     */
    @PostMapping("/{id}/toggle-active")
    public ApiResponse<MembershipLevelResponse> toggleActive(@PathVariable String id) {
        MembershipLevelResponse result = membershipLevelService.toggleActive(id);
        return ApiResponse.ok("狀態已更新", result);
    }

    /**
     * 更新會員等級排序
     */
    @PostMapping("/sort-order")
    public ApiResponse<Void> updateSortOrder(@RequestBody List<String> ids) {
        membershipLevelService.updateSortOrder(ids);
        return ApiResponse.ok("排序已更新", null);
    }
}
