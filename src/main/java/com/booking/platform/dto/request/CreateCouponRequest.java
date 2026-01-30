package com.booking.platform.dto.request;

import com.booking.platform.enums.CouponType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建立票券請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    @NotBlank(message = "票券名稱不能為空")
    @Size(max = 100, message = "票券名稱長度不能超過 100 字")
    private String name;

    @Size(max = 500, message = "票券描述長度不能超過 500 字")
    private String description;

    @NotNull(message = "票券類型不能為空")
    private CouponType type;

    /**
     * 折扣金額（DISCOUNT_AMOUNT 用）
     */
    @DecimalMin(value = "0", message = "折扣金額不能為負數")
    private BigDecimal discountAmount;

    /**
     * 折扣比例（DISCOUNT_PERCENT 用，0.1 = 9折）
     */
    @DecimalMin(value = "0", message = "折扣比例不能為負數")
    @DecimalMax(value = "1", message = "折扣比例不能超過 1")
    private BigDecimal discountPercent;

    /**
     * 最低消費金額
     */
    @DecimalMin(value = "0", message = "最低消費金額不能為負數")
    private BigDecimal minOrderAmount;

    /**
     * 最高折抵金額
     */
    @DecimalMin(value = "0", message = "最高折抵金額不能為負數")
    private BigDecimal maxDiscountAmount;

    /**
     * 兌換項目（GIFT 用）
     */
    @Size(max = 200, message = "兌換項目長度不能超過 200 字")
    private String giftItem;

    /**
     * 發行數量（null 表示不限）
     */
    @Min(value = 1, message = "發行數量至少為 1")
    private Integer totalQuantity;

    /**
     * 每人限領數量
     */
    @Min(value = 1, message = "每人限領數量至少為 1")
    private Integer limitPerCustomer;

    /**
     * 有效期天數
     */
    @Min(value = 1, message = "有效期天數至少為 1")
    private Integer validDays;

    /**
     * 固定有效起始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStartAt;

    /**
     * 固定有效結束日
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEndAt;

    /**
     * 適用服務 ID（逗號分隔）
     */
    @Size(max = 1000, message = "適用服務長度不能超過 1000 字")
    private String applicableServices;

    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    @Size(max = 1000, message = "使用說明長度不能超過 1000 字")
    private String terms;

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;
}
