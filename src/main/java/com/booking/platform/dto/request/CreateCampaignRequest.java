package com.booking.platform.dto.request;

import com.booking.platform.enums.CampaignType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建立行銷活動請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {

    @NotBlank(message = "活動名稱不能為空")
    @Size(max = 100, message = "活動名稱長度不能超過 100 字")
    private String name;

    @Size(max = 500, message = "活動描述長度不能超過 500 字")
    private String description;

    @NotNull(message = "活動類型不能為空")
    private CampaignType type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endAt;

    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    /**
     * 消費門檻金額（滿額活動用）
     */
    private BigDecimal thresholdAmount;

    /**
     * 久未到店天數（喚回活動用）
     */
    private Integer recallDays;

    /**
     * 關聯票券 ID
     */
    private String couponId;

    /**
     * 贈送點數
     */
    private Integer bonusPoints;

    /**
     * 推播訊息
     */
    @Size(max = 1000, message = "推播訊息長度不能超過 1000 字")
    private String pushMessage;

    /**
     * 是否自動觸發
     */
    private Boolean isAutoTrigger;

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;
}
