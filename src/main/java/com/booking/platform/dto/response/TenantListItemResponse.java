package com.booking.platform.dto.response;

import com.booking.platform.enums.TenantStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租戶列表項目回應（用於投影查詢）
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantListItemResponse {

    private String id;
    private String code;
    private String name;
    private TenantStatus status;
    private String phone;
    private String email;
    private BigDecimal pointBalance;
    private Boolean isTestAccount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
