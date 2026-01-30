package com.booking.platform.mapper;

import com.booking.platform.dto.response.MembershipLevelResponse;
import com.booking.platform.entity.customer.MembershipLevel;
import org.springframework.stereotype.Component;

/**
 * 會員等級物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class MembershipLevelMapper {

    public MembershipLevelResponse toResponse(MembershipLevel entity) {
        return toResponse(entity, null);
    }

    public MembershipLevelResponse toResponse(MembershipLevel entity, Long memberCount) {
        if (entity == null) {
            return null;
        }

        return MembershipLevelResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .badgeColor(entity.getBadgeColor())
                .upgradeThreshold(entity.getUpgradeThreshold())
                .discountRate(entity.getDiscountRate())
                .pointRate(entity.getPointRate())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .memberCount(memberCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
