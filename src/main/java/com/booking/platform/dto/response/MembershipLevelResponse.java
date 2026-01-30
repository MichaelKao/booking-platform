package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 會員等級回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipLevelResponse {

    private String id;
    private String name;
    private String description;
    private String badgeColor;
    private BigDecimal upgradeThreshold;
    private BigDecimal discountRate;
    private BigDecimal pointRate;
    private Boolean isDefault;
    private Boolean isActive;
    private Integer sortOrder;
    private Long memberCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
