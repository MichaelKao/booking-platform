package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateCampaignRequest;
import com.booking.platform.dto.response.CampaignResponse;
import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
import com.booking.platform.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行銷活動控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得活動列表（分頁）
     */
    @GetMapping
    public ApiResponse<PageResponse<CampaignResponse>> getList(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) CampaignType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CampaignResponse> result = campaignService.getList(status, type, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得活動詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getDetail(@PathVariable String id) {
        CampaignResponse result = campaignService.getDetail(id);
        return ApiResponse.ok(result);
    }

    /**
     * 取得進行中的活動
     */
    @GetMapping("/active")
    public ApiResponse<List<CampaignResponse>> getActiveCampaigns() {
        List<CampaignResponse> result = campaignService.getActiveCampaigns();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立活動
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse result = campaignService.create(request);
        return ApiResponse.ok("活動建立成功", result);
    }

    /**
     * 更新活動
     */
    @PutMapping("/{id}")
    public ApiResponse<CampaignResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        CampaignResponse result = campaignService.update(id, request);
        return ApiResponse.ok("活動更新成功", result);
    }

    /**
     * 刪除活動
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        campaignService.delete(id);
        return ApiResponse.ok("活動刪除成功", null);
    }

    // ========================================
    // 狀態操作 API
    // ========================================

    /**
     * 發布活動
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<CampaignResponse> publish(@PathVariable String id) {
        CampaignResponse result = campaignService.publish(id);
        return ApiResponse.ok("活動已發布", result);
    }

    /**
     * 暫停活動
     */
    @PostMapping("/{id}/pause")
    public ApiResponse<CampaignResponse> pause(@PathVariable String id) {
        CampaignResponse result = campaignService.pause(id);
        return ApiResponse.ok("活動已暫停", result);
    }

    /**
     * 恢復活動
     */
    @PostMapping("/{id}/resume")
    public ApiResponse<CampaignResponse> resume(@PathVariable String id) {
        CampaignResponse result = campaignService.resume(id);
        return ApiResponse.ok("活動已恢復", result);
    }

    /**
     * 結束活動
     */
    @PostMapping("/{id}/end")
    public ApiResponse<CampaignResponse> end(@PathVariable String id) {
        CampaignResponse result = campaignService.end(id);
        return ApiResponse.ok("活動已結束", result);
    }
}
