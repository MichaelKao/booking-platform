package com.booking.platform.mapper;

import com.booking.platform.dto.response.CustomerResponse;
import com.booking.platform.entity.customer.Customer;
import org.springframework.stereotype.Component;

/**
 * 顧客物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer entity) {
        return toResponse(entity, null);
    }

    public CustomerResponse toResponse(Customer entity, String membershipLevelName) {
        if (entity == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(entity.getId())
                .lineUserId(entity.getLineUserId())
                .lineDisplayName(entity.getLineDisplayName())
                .linePictureUrl(entity.getLinePictureUrl())
                .name(entity.getName())
                .nickname(entity.getNickname())
                .displayName(entity.getDisplayName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .gender(entity.getGender())
                .birthday(entity.getBirthday())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .isLineBlocked(entity.getIsLineBlocked())
                .membershipLevelId(entity.getMembershipLevelId())
                .membershipLevelName(membershipLevelName)
                .totalSpent(entity.getTotalSpent())
                .visitCount(entity.getVisitCount())
                .pointBalance(entity.getPointBalance())
                .noShowCount(entity.getNoShowCount())
                .lastVisitAt(entity.getLastVisitAt())
                .note(entity.getNote())
                .tags(entity.getTags())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
