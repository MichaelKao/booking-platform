package com.booking.platform.dto.response;

import com.booking.platform.enums.ProductOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品訂單回應 DTO
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderResponse {

    private String id;

    private String orderNo;

    private String customerId;

    private String customerName;

    private String lineUserId;

    private String productId;

    private String productName;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    private ProductOrderStatus status;

    private String statusDescription;

    private String note;

    private LocalDateTime pickupAt;

    private LocalDateTime cancelledAt;

    private String cancelReason;

    private LocalDateTime createdAt;
}
