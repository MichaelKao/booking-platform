package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 超級管理儀表板回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardResponse {

    // ========================================
    // 租戶統計
    // ========================================

    /**
     * 總租戶數
     */
    private Long totalTenants;

    /**
     * 活躍租戶數
     */
    private Long activeTenants;

    /**
     * 待審核租戶數
     */
    private Long pendingTenants;

    /**
     * 停用租戶數
     */
    private Long suspendedTenants;

    // ========================================
    // 儲值統計
    // ========================================

    /**
     * 待審核儲值數
     */
    private Long pendingTopUps;

    /**
     * 待審核儲值總金額
     */
    private BigDecimal pendingTopUpAmount;

    /**
     * 本月已審核儲值總金額
     */
    private BigDecimal monthlyApprovedAmount;

    /**
     * 本月已審核儲值總點數
     */
    private Integer monthlyApprovedPoints;

    // ========================================
    // 預約統計
    // ========================================

    /**
     * 今日預約數
     */
    private Long todayBookings;

    /**
     * 本月預約數
     */
    private Long monthlyBookings;

    // ========================================
    // 最近資料
    // ========================================

    /**
     * 最近註冊的租戶
     */
    private List<TenantListItemResponse> recentTenants;

    /**
     * 待審核的儲值申請
     */
    private List<PointTopUpResponse> pendingTopUpList;
}
