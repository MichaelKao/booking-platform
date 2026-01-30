package com.booking.platform.dto.response;

import com.booking.platform.enums.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服務項目回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemResponse {

    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer duration;
    private Integer bufferTime;
    private ServiceStatus status;
    private Boolean isVisible;
    private Boolean requiresStaff;
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
