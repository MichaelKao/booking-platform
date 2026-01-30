package com.booking.platform.mapper;

import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.entity.system.PointTopUp;
import org.springframework.stereotype.Component;

/**
 * 點數儲值物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class PointTopUpMapper {

    public PointTopUpResponse toResponse(PointTopUp entity) {
        return toResponse(entity, null);
    }

    public PointTopUpResponse toResponse(PointTopUp entity, String tenantName) {
        if (entity == null) {
            return null;
        }

        return PointTopUpResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .tenantName(tenantName)
                .points(entity.getPoints())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paymentMethod(entity.getPaymentMethod())
                .paymentProofUrl(entity.getPaymentProofUrl())
                .requestNote(entity.getRequestNote())
                .reviewNote(entity.getReviewNote())
                .reviewedBy(entity.getReviewedBy())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
