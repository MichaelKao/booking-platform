package com.booking.platform.mapper;

import com.booking.platform.dto.response.CampaignResponse;
import com.booking.platform.entity.marketing.Campaign;
import org.springframework.stereotype.Component;

/**
 * 行銷活動物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class CampaignMapper {

    public CampaignResponse toResponse(Campaign entity) {
        return toResponse(entity, null);
    }

    public CampaignResponse toResponse(Campaign entity, String couponName) {
        if (entity == null) {
            return null;
        }

        return CampaignResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .status(entity.getStatus())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .imageUrl(entity.getImageUrl())
                .thresholdAmount(entity.getThresholdAmount())
                .recallDays(entity.getRecallDays())
                .couponId(entity.getCouponId())
                .couponName(couponName)
                .bonusPoints(entity.getBonusPoints())
                .pushMessage(entity.getPushMessage())
                .isAutoTrigger(entity.getIsAutoTrigger())
                .participantCount(entity.getParticipantCount())
                .note(entity.getNote())
                .isEffective(entity.isEffective())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
