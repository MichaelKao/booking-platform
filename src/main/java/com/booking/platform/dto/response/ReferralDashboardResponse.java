package com.booking.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推薦儀表板回應 DTO
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralDashboardResponse {

    /**
     * 推薦碼
     */
    private String referralCode;

    /**
     * 推薦連結
     */
    private String referralLink;

    /**
     * 總推薦數
     */
    private long totalReferrals;

    /**
     * 已完成推薦數
     */
    private long completedReferrals;

    /**
     * 待完成推薦數
     */
    private long pendingReferrals;

    /**
     * 累計獲得點數
     */
    private int totalBonusPoints;

    /**
     * 推薦歷史記錄
     */
    private List<ReferralResponse> referralHistory;
}
