package com.booking.platform.dto.request;

import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新商品請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    /**
     * 商品名稱
     */
    @Size(max = 100, message = "商品名稱長度不能超過 100 字")
    private String name;

    /**
     * 商品編號（SKU）
     */
    @Size(max = 50, message = "商品編號長度不能超過 50 字")
    private String sku;

    /**
     * 商品描述
     */
    @Size(max = 500, message = "商品描述長度不能超過 500 字")
    private String description;

    /**
     * 商品分類
     */
    private ProductCategory category;

    /**
     * 商品價格
     */
    @DecimalMin(value = "0", message = "價格不能為負數")
    private BigDecimal price;

    /**
     * 成本價格
     */
    @DecimalMin(value = "0", message = "成本價格不能為負數")
    private BigDecimal costPrice;

    /**
     * 庫存數量
     */
    @Min(value = 0, message = "庫存數量不能為負數")
    private Integer stockQuantity;

    /**
     * 安全庫存量
     */
    @Min(value = 0, message = "安全庫存量不能為負數")
    private Integer safetyStock;

    /**
     * 商品狀態
     */
    private ProductStatus status;

    /**
     * 排序順序
     */
    @Min(value = 0, message = "排序順序不能為負數")
    @Max(value = 9999, message = "排序順序不能超過 9999")
    private Integer sortOrder;

    /**
     * 圖片 URL
     */
    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;
}
