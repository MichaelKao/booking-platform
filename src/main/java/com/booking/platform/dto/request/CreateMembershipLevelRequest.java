package com.booking.platform.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 建立會員等級請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMembershipLevelRequest {

    @NotBlank(message = "等級名稱不能為空")
    @Size(max = 50, message = "等級名稱長度不能超過 50 字")
    private String name;

    @Size(max = 200, message = "等級描述長度不能超過 200 字")
    private String description;

    @Size(max = 20, message = "徽章顏色長度不能超過 20 字")
    private String badgeColor;

    @DecimalMin(value = "0", message = "升級門檻不能小於 0")
    private BigDecimal upgradeThreshold;

    @DecimalMin(value = "0", message = "折扣比例不能小於 0")
    @DecimalMax(value = "1", message = "折扣比例不能大於 1")
    private BigDecimal discountRate;

    @DecimalMin(value = "0", message = "點數回饋比例不能小於 0")
    @DecimalMax(value = "1", message = "點數回饋比例不能大於 1")
    private BigDecimal pointRate;

    private Boolean isDefault;

    private Boolean isActive;

    private Integer sortOrder;
}
