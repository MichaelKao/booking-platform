package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.response.InventoryLogResponse;
import com.booking.platform.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 庫存異動 Controller
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 取得所有庫存異動記錄（分頁）
     */
    @GetMapping("/logs")
    public ApiResponse<PageResponse<InventoryLogResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(inventoryService.getList(pageable));
    }

    /**
     * 取得指定商品的庫存異動記錄
     */
    @GetMapping("/products/{productId}/logs")
    public ApiResponse<List<InventoryLogResponse>> getProductLogs(@PathVariable String productId) {
        return ApiResponse.ok(inventoryService.getByProduct(productId));
    }

    /**
     * 取得指定商品的庫存異動記錄（分頁）
     */
    @GetMapping("/products/{productId}/logs/paged")
    public ApiResponse<PageResponse<InventoryLogResponse>> getProductLogsPaged(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(inventoryService.getByProductPaged(productId, pageable));
    }
}
