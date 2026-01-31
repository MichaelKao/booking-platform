package com.booking.platform.controller.admin;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateTenantRequest;
import com.booking.platform.dto.request.UpdateTenantRequest;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.dto.response.TenantDetailResponse;
import com.booking.platform.dto.response.TenantListItemResponse;
import com.booking.platform.dto.response.TenantResponse;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.service.PointTopUpService;
import com.booking.platform.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 超級管理後台 - 租戶管理 Controller
 *
 * <p>提供超級管理員管理所有店家的 API
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminTenantController {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantService tenantService;
    private final PointTopUpService pointTopUpService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 查詢租戶列表
     *
     * @param page 頁碼
     * @param size 每頁筆數
     * @param status 狀態篩選
     * @param keyword 關鍵字
     * @return 分頁結果
     */
    @GetMapping
    public ApiResponse<PageResponse<TenantListItemResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) String keyword
    ) {
        // 限制最大分頁大小
        size = Math.min(size, 100);

        // 建立分頁參數
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 查詢並返回
        return ApiResponse.ok(tenantService.getList(status, keyword, pageable));
    }

    /**
     * 查詢租戶詳情
     *
     * @param id 租戶 ID
     * @return 詳情資料
     */
    @GetMapping("/{id}")
    public ApiResponse<TenantDetailResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(tenantService.getDetail(id));
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立租戶
     *
     * @param request 建立請求
     * @return 建立結果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        log.info("收到建立租戶請求：{}", request);
        return ApiResponse.ok(tenantService.create(request));
    }

    /**
     * 更新租戶
     *
     * @param id 租戶 ID
     * @param request 更新請求
     * @return 更新結果
     */
    @PutMapping("/{id}")
    public ApiResponse<TenantResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        log.info("收到更新租戶請求，ID：{}", id);
        return ApiResponse.ok(tenantService.update(id, request));
    }

    /**
     * 刪除租戶
     *
     * @param id 租戶 ID
     * @return 刪除結果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("收到刪除租戶請求，ID：{}", id);
        tenantService.delete(id);
        return ApiResponse.ok();
    }

    // ========================================
    // 狀態管理 API
    // ========================================

    /**
     * 更新租戶狀態（統一端點）
     *
     * @param id 租戶 ID
     * @param request 狀態請求
     * @return 更新結果
     */
    @PutMapping("/{id}/status")
    public ApiResponse<TenantResponse> updateStatus(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> request
    ) {
        String status = request.get("status");
        log.info("收到更新租戶狀態請求，ID：{}，狀態：{}", id, status);

        TenantResponse result;
        switch (status) {
            case "ACTIVE":
                result = tenantService.activate(id);
                break;
            case "SUSPENDED":
                result = tenantService.suspend(id);
                break;
            case "FROZEN":
                result = tenantService.freeze(id);
                break;
            default:
                throw new com.booking.platform.common.exception.BusinessException(
                        com.booking.platform.common.exception.ErrorCode.SYS_PARAM_ERROR,
                        "無效的狀態值：" + status
                );
        }
        return ApiResponse.ok("狀態更新成功", result);
    }

    /**
     * 啟用租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @PostMapping("/{id}/activate")
    public ApiResponse<TenantResponse> activate(@PathVariable String id) {
        log.info("收到啟用租戶請求，ID：{}", id);
        return ApiResponse.ok(tenantService.activate(id));
    }

    /**
     * 停用租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @PostMapping("/{id}/suspend")
    public ApiResponse<TenantResponse> suspend(@PathVariable String id) {
        log.info("收到停用租戶請求，ID：{}", id);
        return ApiResponse.ok(tenantService.suspend(id));
    }

    /**
     * 凍結租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @PostMapping("/{id}/freeze")
    public ApiResponse<TenantResponse> freeze(@PathVariable String id) {
        log.info("收到凍結租戶請求，ID：{}", id);
        return ApiResponse.ok(tenantService.freeze(id));
    }

    // ========================================
    // 點數管理 API
    // ========================================

    /**
     * 增加租戶點數
     *
     * @param id 租戶 ID
     * @param amount 增加金額
     * @return 更新結果
     */
    @PostMapping("/{id}/points/add")
    public ApiResponse<TenantResponse> addPoints(
            @PathVariable String id,
            @RequestParam BigDecimal amount
    ) {
        log.info("收到增加租戶點數請求，ID：{}，金額：{}", id, amount);
        return ApiResponse.ok(tenantService.addPoints(id, amount));
    }

    /**
     * 取得租戶儲值記錄
     *
     * @param id 租戶 ID
     * @param page 頁碼
     * @param size 每頁筆數
     * @return 儲值記錄
     */
    @GetMapping("/{id}/topups")
    public ApiResponse<PageResponse<PointTopUpResponse>> getTenantTopups(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.ok(pointTopUpService.getAllTopUps(null, id, pageable));
    }
}
