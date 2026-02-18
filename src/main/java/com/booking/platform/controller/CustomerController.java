package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateCustomerRequest;
import com.booking.platform.dto.response.CustomerResponse;
import com.booking.platform.enums.CustomerStatus;
import com.booking.platform.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 顧客管理控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得顧客列表（分頁）
     */
    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> getList(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CustomerResponse> result = customerService.getList(status, keyword, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得顧客詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getDetail(@PathVariable String id) {
        CustomerResponse result = customerService.getDetail(id);
        return ApiResponse.ok(result);
    }

    /**
     * 依 LINE User ID 取得顧客
     */
    @GetMapping("/by-line-user/{lineUserId}")
    public ApiResponse<CustomerResponse> getByLineUserId(@PathVariable String lineUserId) {
        CustomerResponse result = customerService.getByLineUserId(lineUserId);
        return ApiResponse.ok(result);
    }

    /**
     * 取得今日壽星
     */
    @GetMapping("/birthdays/today")
    public ApiResponse<List<CustomerResponse>> getBirthdayCustomers() {
        List<CustomerResponse> result = customerService.getBirthdayCustomers();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立顧客
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse result = customerService.create(request);
        return ApiResponse.ok("顧客建立成功", result);
    }

    /**
     * 更新顧客
     */
    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponse result = customerService.update(id, request);
        return ApiResponse.ok("顧客更新成功", result);
    }

    /**
     * 刪除顧客
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        customerService.delete(id);
        return ApiResponse.ok("顧客刪除成功", null);
    }

    // ========================================
    // 點數操作 API
    // ========================================

    /**
     * 調整顧客點數（統一端點）
     */
    @PostMapping("/points/adjust")
    public ApiResponse<CustomerResponse> adjustPoints(@RequestBody java.util.Map<String, Object> request) {
        String customerId = (String) request.get("customerId");
        Integer points = request.get("points") instanceof Number ? ((Number) request.get("points")).intValue() : 0;
        String description = (String) request.get("description");

        CustomerResponse result;
        if (points > 0) {
            result = customerService.addPoints(customerId, points, description);
        } else if (points < 0) {
            result = customerService.deductPoints(customerId, -points, description);
        } else {
            return ApiResponse.ok("點數無變動", customerService.getDetail(customerId));
        }
        return ApiResponse.ok("點數調整成功", result);
    }

    /**
     * 增加顧客點數
     */
    @PostMapping("/{id}/points/add")
    public ApiResponse<CustomerResponse> addPoints(
            @PathVariable String id,
            @RequestParam int points,
            @RequestParam(required = false) String description
    ) {
        CustomerResponse result = customerService.addPoints(id, points, description);
        return ApiResponse.ok("點數增加成功", result);
    }

    /**
     * 扣除顧客點數
     */
    @PostMapping("/{id}/points/deduct")
    public ApiResponse<CustomerResponse> deductPoints(
            @PathVariable String id,
            @RequestParam int points,
            @RequestParam(required = false) String description
    ) {
        CustomerResponse result = customerService.deductPoints(id, points, description);
        return ApiResponse.ok("點數扣除成功", result);
    }

    // ========================================
    // 狀態操作 API
    // ========================================

    /**
     * 封鎖顧客
     */
    @PostMapping("/{id}/block")
    public ApiResponse<CustomerResponse> blockCustomer(@PathVariable String id) {
        CustomerResponse result = customerService.blockCustomer(id);
        return ApiResponse.ok("顧客已封鎖", result);
    }

    /**
     * 解除封鎖顧客
     */
    @PostMapping("/{id}/unblock")
    public ApiResponse<CustomerResponse> unblockCustomer(@PathVariable String id) {
        CustomerResponse result = customerService.unblockCustomer(id);
        return ApiResponse.ok("顧客已解除封鎖", result);
    }

    // ========================================
    // 標籤操作 API（需訂閱 ADVANCED_CUSTOMER）
    // ========================================

    /**
     * 更新顧客標籤
     */
    @PostMapping("/{id}/tags")
    public ApiResponse<CustomerResponse> updateTags(
            @PathVariable String id,
            @RequestBody java.util.Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) request.get("tags");
        CustomerResponse result = customerService.updateTags(id, tags);
        return ApiResponse.ok("標籤更新成功", result);
    }

    /**
     * 新增顧客標籤
     */
    @PostMapping("/{id}/tags/add")
    public ApiResponse<CustomerResponse> addTag(
            @PathVariable String id,
            @RequestParam String tag
    ) {
        CustomerResponse result = customerService.addTag(id, tag);
        return ApiResponse.ok("標籤新增成功", result);
    }

    /**
     * 移除顧客標籤
     */
    @DeleteMapping("/{id}/tags/{tag}")
    public ApiResponse<CustomerResponse> removeTag(
            @PathVariable String id,
            @PathVariable String tag
    ) {
        CustomerResponse result = customerService.removeTag(id, tag);
        return ApiResponse.ok("標籤移除成功", result);
    }

    /**
     * 依標籤搜尋顧客
     */
    @GetMapping("/by-tag/{tag}")
    public ApiResponse<List<CustomerResponse>> getCustomersByTag(@PathVariable String tag) {
        List<CustomerResponse> result = customerService.getCustomersByTag(tag);
        return ApiResponse.ok(result);
    }

    /**
     * 取得所有使用中的標籤
     */
    @GetMapping("/tags")
    public ApiResponse<List<String>> getAllTags() {
        List<String> result = customerService.getAllTags();
        return ApiResponse.ok(result);
    }

    // ========================================
    // 點數交易記錄 API
    // ========================================

    /**
     * 取得顧客的點數交易記錄
     */
    @GetMapping("/{id}/points/transactions")
    public ApiResponse<PageResponse<com.booking.platform.dto.response.PointTransactionResponse>> getPointTransactions(
            @PathVariable String id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<com.booking.platform.dto.response.PointTransactionResponse> result =
                customerService.getPointTransactions(id, pageable);
        return ApiResponse.ok(result);
    }
}
