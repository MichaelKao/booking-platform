package com.booking.platform.dto.response;

import com.booking.platform.enums.FeatureCode;
import com.booking.platform.enums.FeatureStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租戶功能狀態回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantFeatureResponse {

    private String id;
    private String tenantId;
    private FeatureCode featureCode;
    private String featureName;
    private String featureDescription;
    private FeatureStatus status;
    private Boolean isFree;
    private Integer monthlyPoints;
    private Integer customMonthlyPoints;
    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enabledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private String enabledBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 是否有效（已啟用且未過期）
     */
    private Boolean isEffective;
}
