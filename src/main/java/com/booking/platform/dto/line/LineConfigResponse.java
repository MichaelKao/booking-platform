package com.booking.platform.dto.line;

import com.booking.platform.enums.line.LineConfigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LINE 設定回應 DTO
 *
 * <p>返回給前端的 LINE 設定資訊
 *
 * <p>注意：不包含敏感資訊（Channel Secret 和 Access Token）
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineConfigResponse {

    // ========================================
    // 基本資訊
    // ========================================

    /**
     * 租戶 ID
     */
    private String tenantId;

    /**
     * LINE Channel ID
     */
    private String channelId;

    /**
     * 是否已設定 Channel Secret
     */
    private Boolean hasChannelSecret;

    /**
     * 是否已設定 Access Token
     */
    private Boolean hasAccessToken;

    // ========================================
    // Webhook 資訊
    // ========================================

    /**
     * Webhook URL
     */
    private String webhookUrl;

    /**
     * Webhook 是否已驗證
     */
    private Boolean webhookVerified;

    /**
     * 最後驗證時間
     */
    private LocalDateTime lastVerifiedAt;

    // ========================================
    // 狀態
    // ========================================

    /**
     * 設定狀態
     */
    private LineConfigStatus status;

    /**
     * 狀態說明
     */
    private String statusDescription;

    // ========================================
    // 訊息設定
    // ========================================

    /**
     * 歡迎訊息
     */
    private String welcomeMessage;

    /**
     * 預設回覆訊息
     */
    private String defaultReply;

    /**
     * 是否啟用自動回覆
     */
    private Boolean autoReplyEnabled;

    /**
     * 是否啟用預約功能
     */
    private Boolean bookingEnabled;

    // ========================================
    // 額度資訊
    // ========================================

    /**
     * 本月推送訊息數量
     */
    private Integer monthlyPushCount;

    /**
     * 每月推送額度
     */
    private Integer monthlyPushQuota;

    /**
     * 剩餘推送額度
     */
    private Integer remainingPushQuota;

    // ========================================
    // 時間資訊
    // ========================================

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 取得狀態說明
     *
     * @param status 狀態
     * @return 狀態說明
     */
    public static String getStatusDescription(LineConfigStatus status) {
        if (status == null) {
            return "未知狀態";
        }
        return switch (status) {
            case PENDING -> "待設定，請填寫 LINE Channel 資訊";
            case ACTIVE -> "運作中";
            case INACTIVE -> "已停用";
            case INVALID -> "設定無效，請檢查 Channel Secret 和 Access Token";
            case VERIFYING -> "驗證中";
        };
    }
}
