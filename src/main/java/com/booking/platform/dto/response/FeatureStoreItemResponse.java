package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 功能商店項目回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureStoreItemResponse {

    /**
     * 功能代碼
     */
    private String code;

    /**
     * 功能名稱
     */
    private String name;

    /**
     * 功能描述
     */
    private String description;

    /**
     * 功能分類
     */
    private String category;

    /**
     * 每月價格（點數）
     */
    private BigDecimal monthlyPrice;

    /**
     * 是否免費
     */
    private Boolean isFree;

    /**
     * 是否已啟用
     */
    private Boolean isEnabled;

    /**
     * 訂閱到期時間
     */
    private LocalDateTime subscriptionExpiry;

    /**
     * 功能圖示
     */
    private String iconUrl;

    /**
     * 排序順序
     */
    private Integer sortOrder;
}
