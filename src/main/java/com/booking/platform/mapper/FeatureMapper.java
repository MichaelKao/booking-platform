package com.booking.platform.mapper;

import com.booking.platform.dto.response.FeatureResponse;
import com.booking.platform.dto.response.TenantFeatureResponse;
import com.booking.platform.entity.system.Feature;
import com.booking.platform.entity.system.TenantFeature;
import org.springframework.stereotype.Component;

/**
 * 功能物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class FeatureMapper {

    public FeatureResponse toResponse(Feature entity) {
        if (entity == null) {
            return null;
        }

        return FeatureResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .isFree(entity.getIsFree())
                .monthlyPoints(entity.getMonthlyPoints())
                .icon(entity.getIcon())
                .category(entity.getCategory())
                .sortOrder(entity.getSortOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TenantFeatureResponse toTenantFeatureResponse(TenantFeature entity, Feature feature) {
        if (entity == null) {
            return null;
        }

        return TenantFeatureResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .featureCode(entity.getFeatureCode())
                .featureName(feature != null ? feature.getName() : entity.getFeatureCode().getName())
                .featureDescription(feature != null ? feature.getDescription() : entity.getFeatureCode().getDescription())
                .status(entity.getStatus())
                .isFree(feature != null ? feature.getIsFree() : entity.getFeatureCode().isFree())
                .monthlyPoints(feature != null ? feature.getMonthlyPoints() : entity.getFeatureCode().getMonthlyPoints())
                .customMonthlyPoints(entity.getCustomMonthlyPoints())
                .note(entity.getNote())
                .enabledAt(entity.getEnabledAt())
                .expiresAt(entity.getExpiresAt())
                .enabledBy(entity.getEnabledBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isEffective(entity.isEffective())
                .build();
    }
}
