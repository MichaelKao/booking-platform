package com.booking.platform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新功能請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureRequest {

    /**
     * 功能名稱
     */
    @Size(max = 100, message = "功能名稱不能超過 100 字")
    private String name;

    /**
     * 功能描述
     */
    @Size(max = 500, message = "功能描述不能超過 500 字")
    private String description;

    /**
     * 是否啟用（全域）
     */
    private Boolean isActive;

    /**
     * 是否免費功能
     */
    private Boolean isFree;

    /**
     * 每月消耗點數
     */
    private Integer monthlyPoints;

    /**
     * 功能圖示
     */
    @Size(max = 100, message = "圖示不能超過 100 字")
    private String icon;

    /**
     * 功能分類
     */
    @Size(max = 50, message = "分類不能超過 50 字")
    private String category;

    /**
     * 排序順序
     */
    private Integer sortOrder;
}
