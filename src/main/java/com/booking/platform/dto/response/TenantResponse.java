package com.booking.platform.dto.response;

import com.booking.platform.enums.TenantStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租戶回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private String logoUrl;
    private String phone;
    private String email;
    private String address;
    private TenantStatus status;
    private Boolean isTestAccount;
    private BigDecimal pointBalance;
    private Integer maxStaffCount;
    private Integer monthlyPushQuota;
    private Integer monthlyPushUsed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
