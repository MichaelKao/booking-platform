package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateProductRequest;
import com.booking.platform.dto.response.ProductResponse;
import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import com.booking.platform.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得商品列表（分頁）
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getList(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductResponse> result = productService.getList(status, category, keyword, pageable);
        return ApiResponse.ok(result);
    }

    /**
     * 取得商品詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getDetail(@PathVariable String id) {
        ProductResponse result = productService.getDetail(id);
        return ApiResponse.ok(result);
    }

    /**
     * 取得上架中的商品
     */
    @GetMapping("/on-sale")
    public ApiResponse<List<ProductResponse>> getOnSaleProducts() {
        List<ProductResponse> result = productService.getOnSaleProducts();
        return ApiResponse.ok(result);
    }

    /**
     * 取得低庫存商品
     */
    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> getLowStockProducts() {
        List<ProductResponse> result = productService.getLowStockProducts();
        return ApiResponse.ok(result);
    }

    /**
     * 依分類取得商品
     */
    @GetMapping("/category/{category}")
    public ApiResponse<List<ProductResponse>> getByCategory(@PathVariable ProductCategory category) {
        List<ProductResponse> result = productService.getByCategory(category);
        return ApiResponse.ok(result);
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立商品
     */
    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse result = productService.create(request);
        return ApiResponse.ok("商品建立成功", result);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse result = productService.update(id, request);
        return ApiResponse.ok("商品更新成功", result);
    }

    /**
     * 刪除商品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ApiResponse.ok("商品刪除成功", null);
    }

    // ========================================
    // 狀態操作 API
    // ========================================

    /**
     * 上架商品
     */
    @PostMapping("/{id}/on-sale")
    public ApiResponse<ProductResponse> putOnSale(@PathVariable String id) {
        ProductResponse result = productService.putOnSale(id);
        return ApiResponse.ok("商品已上架", result);
    }

    /**
     * 下架商品
     */
    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ProductResponse> takeOffShelf(@PathVariable String id) {
        ProductResponse result = productService.takeOffShelf(id);
        return ApiResponse.ok("商品已下架", result);
    }

    // ========================================
    // 庫存操作 API
    // ========================================

    /**
     * 調整庫存
     */
    @PostMapping("/{id}/adjust-stock")
    public ApiResponse<ProductResponse> adjustStock(
            @PathVariable String id,
            @RequestParam int adjustment,
            @RequestParam(required = false) String reason
    ) {
        ProductResponse result = productService.adjustStock(id, adjustment, reason);
        return ApiResponse.ok("庫存調整成功", result);
    }
}
