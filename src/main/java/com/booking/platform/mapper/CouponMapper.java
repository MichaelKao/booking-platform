package com.booking.platform.mapper;

import com.booking.platform.dto.response.CouponInstanceResponse;
import com.booking.platform.dto.response.CouponResponse;
import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.entity.marketing.CouponInstance;
import org.springframework.stereotype.Component;

/**
 * 票券物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon entity) {
        if (entity == null) {
            return null;
        }

        return CouponResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .status(entity.getStatus())
                .discountAmount(entity.getDiscountAmount())
                .discountPercent(entity.getDiscountPercent())
                .minOrderAmount(entity.getMinOrderAmount())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .giftItem(entity.getGiftItem())
                .totalQuantity(entity.getTotalQuantity())
                .issuedQuantity(entity.getIssuedQuantity())
                .usedQuantity(entity.getUsedQuantity())
                .remainingQuantity(entity.getRemainingQuantity())
                .limitPerCustomer(entity.getLimitPerCustomer())
                .validDays(entity.getValidDays())
                .validStartAt(entity.getValidStartAt())
                .validEndAt(entity.getValidEndAt())
                .applicableServices(entity.getApplicableServices())
                .imageUrl(entity.getImageUrl())
                .terms(entity.getTerms())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CouponInstanceResponse toInstanceResponse(CouponInstance entity, Coupon coupon, String customerName) {
        if (entity == null) {
            return null;
        }

        CouponInstanceResponse.CouponInstanceResponseBuilder builder = CouponInstanceResponse.builder()
                .id(entity.getId())
                .couponId(entity.getCouponId())
                .customerId(entity.getCustomerId())
                .customerName(customerName)
                .code(entity.getCode())
                .status(entity.getStatus())
                .source(entity.getSource())
                .sourceDescription(entity.getSourceDescription())
                .validFrom(entity.getValidFrom())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .usedOrderId(entity.getUsedOrderId())
                .actualDiscountAmount(entity.getActualDiscountAmount())
                .voidReason(entity.getVoidReason())
                .canUse(entity.canUse())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (coupon != null) {
            builder.couponName(coupon.getName())
                    .couponType(coupon.getType())
                    .discountAmount(coupon.getDiscountAmount())
                    .discountPercent(coupon.getDiscountPercent())
                    .giftItem(coupon.getGiftItem());
        }

        return builder.build();
    }
}
