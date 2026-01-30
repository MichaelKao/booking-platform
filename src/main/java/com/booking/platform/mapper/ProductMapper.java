package com.booking.platform.mapper;

import com.booking.platform.dto.response.ProductResponse;
import com.booking.platform.entity.product.Product;
import org.springframework.stereotype.Component;

/**
 * 商品 Mapper
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class ProductMapper {

    /**
     * Entity 轉 Response
     */
    public ProductResponse toResponse(Product entity) {
        if (entity == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .sku(entity.getSku())
                .barcode(entity.getBarcode())
                .price(entity.getPrice())
                .cost(entity.getCost())
                .stockQuantity(entity.getStockQuantity())
                .safetyStock(entity.getSafetyStock())
                .imageUrl(entity.getImageUrl())
                .brand(entity.getBrand())
                .specification(entity.getSpecification())
                .unit(entity.getUnit())
                .trackInventory(entity.getTrackInventory())
                .isSellable(entity.getIsSellable())
                .sortOrder(entity.getSortOrder())
                .soldQuantity(entity.getSoldQuantity())
                .note(entity.getNote())
                .isLowStock(entity.isLowStock())
                .canSell(entity.canSell())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
