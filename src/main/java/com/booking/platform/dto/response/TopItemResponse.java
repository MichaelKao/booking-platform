package com.booking.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 熱門項目回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopItemResponse {

    /**
     * 項目 ID
     */
    private String id;

    /**
     * 項目名稱
     */
    private String name;

    /**
     * 數量/次數
     */
    private Long count;

    /**
     * 金額
     */
    private BigDecimal amount;

    /**
     * 佔比
     */
    private BigDecimal percentage;
}
