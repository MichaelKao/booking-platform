package com.booking.platform.mapper;

import com.booking.platform.dto.response.StaffResponse;
import com.booking.platform.entity.staff.Staff;
import org.springframework.stereotype.Component;

/**
 * 員工物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class StaffMapper {

    public StaffResponse toResponse(Staff entity) {
        if (entity == null) {
            return null;
        }

        return StaffResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .avatarUrl(entity.getAvatarUrl())
                .bio(entity.getBio())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .isBookable(entity.getIsBookable())
                .isVisible(entity.getIsVisible())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
