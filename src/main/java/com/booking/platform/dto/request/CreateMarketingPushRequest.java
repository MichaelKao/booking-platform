package com.booking.platform.dto.request;

import com.booking.platform.enums.MarketingPushTargetType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 建立行銷推播請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMarketingPushRequest {

    @NotBlank(message = "推播標題不能為空")
    @Size(max = 100, message = "推播標題長度不能超過 100 字")
    private String title;

    @Size(max = 2000, message = "推播內容長度不能超過 2000 字")
    private String content;

    @Size(max = 500, message = "圖片 URL 長度不能超過 500 字")
    private String imageUrl;

    @NotNull(message = "目標類型不能為空")
    private MarketingPushTargetType targetType;

    /**
     * 目標值（會員等級 ID 或標籤名稱）
     */
    private String targetValue;

    /**
     * 自訂名單（LINE User ID 列表）
     */
    private List<String> customTargets;

    /**
     * 排程發送時間（null 表示儲存為草稿）
     * 支援 ISO 格式 (yyyy-MM-dd'T'HH:mm:ss) 和標準格式 (yyyy-MM-dd HH:mm:ss)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;
}
