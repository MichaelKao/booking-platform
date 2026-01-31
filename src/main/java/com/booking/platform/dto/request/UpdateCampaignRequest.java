package com.booking.platform.dto.request;

import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 更新行銷活動請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignRequest {

    /**
     * 活動名稱
     */
    @Size(max = 100, message = "活動名稱長度不能超過 100 字")
    private String name;

    /**
     * 活動描述
     */
    @Size(max = 1000, message = "活動描述長度不能超過 1000 字")
    private String description;

    /**
     * 活動類型
     */
    private CampaignType type;

    /**
     * 活動開始時間
     */
    private LocalDateTime startTime;

    /**
     * 活動結束時間
     */
    private LocalDateTime endTime;

    /**
     * 活動狀態
     */
    private CampaignStatus status;

    /**
     * 活動規則（JSON 格式）
     */
    @Size(max = 2000, message = "活動規則長度不能超過 2000 字")
    private String rules;

    /**
     * 活動圖片 URL
     */
    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    /**
     * 目標會員等級 ID 列表（逗號分隔）
     */
    @Size(max = 500, message = "目標會員等級列表長度不能超過 500 字")
    private String targetMembershipLevels;

    /**
     * 關聯票券 ID
     */
    private String couponId;
}
