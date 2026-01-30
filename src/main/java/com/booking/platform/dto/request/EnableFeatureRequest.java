package com.booking.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 啟用功能請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnableFeatureRequest {

    /**
     * 過期時間（可選，null 表示永久）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 自訂月費點數（可選，覆蓋預設值）
     */
    private Integer customMonthlyPoints;

    /**
     * 備註
     */
    private String note;
}
