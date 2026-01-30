package com.booking.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 建立服務項目請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceItemRequest {

    @NotBlank(message = "服務名稱不能為空")
    @Size(max = 100, message = "服務名稱長度不能超過 100 字")
    private String name;

    @Size(max = 500, message = "服務描述長度不能超過 500 字")
    private String description;

    private String categoryId;

    @NotNull(message = "服務價格不能為空")
    @DecimalMin(value = "0", message = "服務價格不能小於 0")
    private BigDecimal price;

    @NotNull(message = "服務時長不能為空")
    @Min(value = 1, message = "服務時長至少 1 分鐘")
    private Integer duration;

    @Min(value = 0, message = "緩衝時間不能小於 0")
    private Integer bufferTime;

    private Boolean isVisible;

    private Boolean requiresStaff;

    private Integer sortOrder;
}
