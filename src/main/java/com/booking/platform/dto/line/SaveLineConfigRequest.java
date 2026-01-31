package com.booking.platform.dto.line;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 儲存 LINE 設定請求 DTO
 *
 * <p>用於新增或更新 LINE 設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveLineConfigRequest {

    // ========================================
    // LINE Channel 設定
    // ========================================

    /**
     * LINE Channel ID
     */
    @Size(max = 50, message = "Channel ID 長度不能超過 50 字元")
    private String channelId;

    /**
     * LINE Channel Secret
     * <p>只有在需要更新時才傳入，null 表示不更新
     */
    @Size(max = 100, message = "Channel Secret 長度不能超過 100 字元")
    private String channelSecret;

    /**
     * LINE Channel Access Token
     * <p>只有在需要更新時才傳入，null 表示不更新
     */
    @Size(max = 500, message = "Access Token 長度不能超過 500 字元")
    private String channelAccessToken;

    // ========================================
    // 訊息設定
    // ========================================

    /**
     * 歡迎訊息
     */
    @Size(max = 1000, message = "歡迎訊息長度不能超過 1000 字元")
    private String welcomeMessage;

    /**
     * 預設回覆訊息
     */
    @Size(max = 1000, message = "預設回覆訊息長度不能超過 1000 字元")
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
    // 輔助方法
    // ========================================

    /**
     * 檢查是否有更新 Channel Secret
     *
     * @return true 表示有更新
     */
    public boolean hasChannelSecretUpdate() {
        return this.channelSecret != null && !this.channelSecret.isEmpty();
    }

    /**
     * 檢查是否有更新 Access Token
     *
     * @return true 表示有更新
     */
    public boolean hasAccessTokenUpdate() {
        return this.channelAccessToken != null && !this.channelAccessToken.isEmpty();
    }

    /**
     * 檢查是否為完整的 LINE 設定
     *
     * @return true 表示完整
     */
    public boolean isComplete() {
        return this.channelId != null && !this.channelId.isEmpty()
                && hasChannelSecretUpdate()
                && hasAccessTokenUpdate();
    }
}
