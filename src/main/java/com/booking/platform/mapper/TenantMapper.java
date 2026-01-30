package com.booking.platform.mapper;

import com.booking.platform.dto.response.TenantDetailResponse;
import com.booking.platform.dto.response.TenantResponse;
import com.booking.platform.entity.tenant.Tenant;
import org.springframework.stereotype.Component;

/**
 * 租戶物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class TenantMapper {

    /**
     * Entity 轉 Response
     */
    public TenantResponse toResponse(Tenant entity) {
        if (entity == null) {
            return null;
        }

        return TenantResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .logoUrl(entity.getLogoUrl())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .isTestAccount(entity.getIsTestAccount())
                .pointBalance(entity.getPointBalance())
                .maxStaffCount(entity.getMaxStaffCount())
                .monthlyPushQuota(entity.getMonthlyPushQuota())
                .monthlyPushUsed(entity.getMonthlyPushUsed())
                .activatedAt(entity.getActivatedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Entity 轉 DetailResponse
     */
    public TenantDetailResponse toDetailResponse(Tenant entity) {
        if (entity == null) {
            return null;
        }

        return TenantDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .logoUrl(entity.getLogoUrl())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .isTestAccount(entity.getIsTestAccount())
                .lineConfigured(entity.getLineChannelToken() != null && !entity.getLineChannelToken().isEmpty())
                .pointBalance(entity.getPointBalance())
                .maxStaffCount(entity.getMaxStaffCount())
                .monthlyPushQuota(entity.getMonthlyPushQuota())
                .monthlyPushUsed(entity.getMonthlyPushUsed())
                .monthlyPushRemaining(entity.getMonthlyPushQuota() - entity.getMonthlyPushUsed())
                .activatedAt(entity.getActivatedAt())
                .expiredAt(entity.getExpiredAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
