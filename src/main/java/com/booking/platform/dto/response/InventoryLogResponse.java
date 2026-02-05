package com.booking.platform.dto.response;

import com.booking.platform.enums.InventoryActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 庫存異動記錄回應 DTO
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogResponse {

    private String id;

    private String productId;

    private String productName;

    private InventoryActionType actionType;

    private String actionTypeDescription;

    private Integer quantity;

    private Integer quantityBefore;

    private Integer quantityAfter;

    private String reason;

    private String orderId;

    private String operatorId;

    private String operatorName;

    private LocalDateTime createdAt;
}
