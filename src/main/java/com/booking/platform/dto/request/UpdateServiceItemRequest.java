package com.booking.platform.dto.request;

import com.booking.platform.enums.ServiceStatus;
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
 * 更新服務項目請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceItemRequest {

    /**
     * 服務分類 ID
     */
    private String categoryId;

    /**
     * 服務名稱
     */
    @Size(max = 100, message = "服務名稱長度不能超過 100 字")
    private String name;

    /**
     * 服務描述
     */
    @Size(max = 500, message = "服務描述長度不能超過 500 字")
    private String description;

    /**
     * 服務時長（分鐘）
     */
    @Min(value = 5, message = "服務時長最少 5 分鐘")
    @Max(value = 480, message = "服務時長最多 480 分鐘")
    private Integer duration;

    /**
     * 服務價格
     */
    @DecimalMin(value = "0", message = "價格不能為負數")
    private BigDecimal price;

    /**
     * 服務狀態
     */
    private ServiceStatus status;

    /**
     * 排序順序
     */
    @Min(value = 0, message = "排序順序不能為負數")
    @Max(value = 9999, message = "排序順序不能超過 9999")
    private Integer sortOrder;

    /**
     * 圖片 URL
     */
    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    /**
     * 是否需要指定員工
     */
    private Boolean requireStaff;

    /** 每時段最大預約數 */
    @Min(value = 1, message = "最大容量至少 1")
    private Integer maxCapacity;

    /**
     * 是否允許 LINE 預約
     */
    private Boolean allowLineBooking;
}
