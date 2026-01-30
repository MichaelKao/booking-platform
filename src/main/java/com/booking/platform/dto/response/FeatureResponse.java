package com.booking.platform.dto.response;

import com.booking.platform.enums.FeatureCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 功能定義回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureResponse {

    private String id;
    private FeatureCode code;
    private String name;
    private String description;
    private Boolean isFree;
    private Integer monthlyPoints;
    private String icon;
    private String category;
    private Integer sortOrder;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
