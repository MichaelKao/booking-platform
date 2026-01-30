package com.booking.platform.dto.response;

import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;

    private String name;

    private String description;

    private ProductCategory category;

    private ProductStatus status;

    private String sku;

    private String barcode;

    private BigDecimal price;

    private BigDecimal cost;

    private Integer stockQuantity;

    private Integer safetyStock;

    private String imageUrl;

    private String brand;

    private String specification;

    private String unit;

    private Boolean trackInventory;

    private Boolean isSellable;

    private Integer sortOrder;

    private Integer soldQuantity;

    private String note;

    /**
     * 是否低庫存
     */
    private Boolean isLowStock;

    /**
     * 是否可販售
     */
    private Boolean canSell;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
