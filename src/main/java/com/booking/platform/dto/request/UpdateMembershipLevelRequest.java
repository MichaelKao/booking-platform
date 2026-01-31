package com.booking.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新會員等級請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMembershipLevelRequest {

    /**
     * 等級名稱
     */
    @Size(max = 50, message = "等級名稱長度不能超過 50 字")
    private String name;

    /**
     * 等級描述
     */
    @Size(max = 500, message = "等級描述長度不能超過 500 字")
    private String description;

    /**
     * 升級所需消費金額
     */
    @DecimalMin(value = "0", message = "升級門檻不能為負數")
    private BigDecimal upgradeThreshold;

    /**
     * 折扣百分比（0-100）
     */
    @Min(value = 0, message = "折扣百分比不能為負數")
    @Max(value = 100, message = "折扣百分比不能超過 100")
    private Integer discountPercent;

    /**
     * 點數倍率
     */
    @DecimalMin(value = "1.0", message = "點數倍率至少為 1.0")
    private BigDecimal pointMultiplier;

    /**
     * 排序順序
     */
    @Min(value = 0, message = "排序順序不能為負數")
    @Max(value = 9999, message = "排序順序不能超過 9999")
    private Integer sortOrder;

    /**
     * 是否為預設等級
     */
    private Boolean isDefault;

    /**
     * 顏色代碼
     */
    @Size(max = 20, message = "顏色代碼長度不能超過 20 字")
    private String colorCode;

    /**
     * 圖示 URL
     */
    @Size(max = 500, message = "圖示 URL 長度不能超過 500 字")
    private String iconUrl;
}
