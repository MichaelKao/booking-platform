package com.booking.platform.dto.response;

import com.booking.platform.enums.ReferralStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 推薦記錄回應 DTO
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralResponse {

    /**
     * 推薦記錄 ID
     */
    private String id;

    /**
     * 被推薦店家名稱
     */
    private String refereeTenantName;

    /**
     * 被推薦店家代碼
     */
    private String refereeTenantCode;

    /**
     * 推薦狀態
     */
    private ReferralStatus status;

    /**
     * 推薦人獲得的獎勵點數
     */
    private Integer referrerBonusPoints;

    /**
     * 被推薦人獲得的獎勵點數
     */
    private Integer refereeBonusPoints;

    /**
     * 獎勵發放時間
     */
    private LocalDateTime rewardedAt;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
}
