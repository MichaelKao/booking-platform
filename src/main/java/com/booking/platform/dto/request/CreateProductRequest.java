package com.booking.platform.dto.request;

import com.booking.platform.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 建立商品請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "商品名稱不能為空")
    @Size(max = 100, message = "商品名稱長度不能超過 100 字")
    private String name;

    @Size(max = 1000, message = "商品描述長度不能超過 1000 字")
    private String description;

    @NotNull(message = "商品分類不能為空")
    private ProductCategory category;

    @Size(max = 50, message = "SKU 長度不能超過 50 字")
    private String sku;

    @Size(max = 50, message = "條碼長度不能超過 50 字")
    private String barcode;

    @NotNull(message = "售價不能為空")
    @DecimalMin(value = "0", message = "售價不能為負數")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "成本價不能為負數")
    private BigDecimal cost;

    @Min(value = 0, message = "庫存數量不能為負數")
    private Integer stockQuantity;

    @Min(value = 0, message = "安全庫存量不能為負數")
    private Integer safetyStock;

    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    @Size(max = 100, message = "品牌長度不能超過 100 字")
    private String brand;

    @Size(max = 200, message = "規格長度不能超過 200 字")
    private String specification;

    @Size(max = 20, message = "單位長度不能超過 20 字")
    private String unit;

    private Boolean trackInventory;

    private Boolean isSellable;

    private Integer sortOrder;

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;
}
