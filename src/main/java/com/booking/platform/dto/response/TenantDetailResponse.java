package com.booking.platform.dto.response;

import com.booking.platform.enums.TenantStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租戶詳情回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailResponse {

    // ========================================
    // 基本資料
    // ========================================

    private String id;
    private String code;
    private String name;
    private String description;
    private String logoUrl;

    // ========================================
    // 聯絡資訊
    // ========================================

    private String phone;
    private String email;
    private String address;

    // ========================================
    // 狀態
    // ========================================

    private TenantStatus status;
    private Boolean isTestAccount;

    // ========================================
    // LINE 設定（僅顯示是否已設定）
    // ========================================

    private Boolean lineConfigured;

    // ========================================
    // 點數與配額
    // ========================================

    private BigDecimal pointBalance;
    private Integer maxStaffCount;
    private Integer monthlyPushQuota;
    private Integer monthlyPushUsed;
    private Integer monthlyPushRemaining;

    // ========================================
    // 統計資料
    // ========================================

    private Long staffCount;
    private Long customerCount;

    // ========================================
    // 時間欄位
    // ========================================

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
