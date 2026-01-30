package com.booking.platform.dto.response;

import com.booking.platform.enums.TopUpStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 點數儲值申請回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTopUpResponse {

    private String id;
    private String tenantId;
    private String tenantName;
    private Integer points;
    private BigDecimal amount;
    private TopUpStatus status;
    private String paymentMethod;
    private String paymentProofUrl;
    private String requestNote;
    private String reviewNote;
    private String reviewedBy;
    private Integer balanceBefore;
    private Integer balanceAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
