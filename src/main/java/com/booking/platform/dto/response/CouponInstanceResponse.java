package com.booking.platform.dto.response;

import com.booking.platform.enums.CouponInstanceStatus;
import com.booking.platform.enums.CouponType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票券實例回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponInstanceResponse {

    private String id;
    private String couponId;
    private String couponName;
    private CouponType couponType;
    private String customerId;
    private String customerName;
    private String code;
    private CouponInstanceStatus status;
    private String source;
    private String sourceDescription;

    /**
     * 折扣金額或比例（方便前端顯示）
     */
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private String giftItem;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;

    private String usedOrderId;
    private String voidReason;
    private Boolean canUse;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
