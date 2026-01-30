package com.booking.platform.mapper;

import com.booking.platform.dto.response.ServiceItemResponse;
import com.booking.platform.entity.catalog.ServiceItem;
import org.springframework.stereotype.Component;

/**
 * 服務項目物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class ServiceItemMapper {

    public ServiceItemResponse toResponse(ServiceItem entity) {
        return toResponse(entity, null);
    }

    public ServiceItemResponse toResponse(ServiceItem entity, String categoryName) {
        if (entity == null) {
            return null;
        }

        return ServiceItemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .categoryId(entity.getCategoryId())
                .categoryName(categoryName)
                .price(entity.getPrice())
                .duration(entity.getDuration())
                .bufferTime(entity.getBufferTime())
                .status(entity.getStatus())
                .isVisible(entity.getIsVisible())
                .requiresStaff(entity.getRequiresStaff())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
