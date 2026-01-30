package com.booking.platform.dto.response;

import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.CouponType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票券定義回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private String id;
    private String name;
    private String description;
    private CouponType type;
    private CouponStatus status;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private String giftItem;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer usedQuantity;
    private Integer remainingQuantity;
    private Integer limitPerCustomer;
    private Integer validDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStartAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEndAt;

    private String applicableServices;
    private String imageUrl;
    private String terms;
    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
