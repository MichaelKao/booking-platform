package com.booking.platform.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 功能代碼列舉
 *
 * <p>定義平台所有功能的代碼，用於功能開關控制
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum FeatureCode {

    // ========================================
    // 免費功能
    // ========================================

    BASIC_BOOKING("基本預約", "基本預約功能", true, 0, true),
    BASIC_CUSTOMER("基本顧客管理", "基本顧客管理功能", true, 0, true),
    BASIC_STAFF("基本員工管理", "基本員工管理（限3位）", true, 0, true),
    BASIC_SERVICE("基本服務管理", "基本服務項目管理", true, 0, true),
    BASIC_REPORT("基本報表", "基本營運報表", true, 0, true),

    // ========================================
    // 加值功能 - 員工與權限
    // ========================================

    UNLIMITED_STAFF("無限員工", "不限制員工數量", false, 500, true),
    MULTI_ACCOUNT("多帳號權限", "多個登入帳號與權限管理", false, 300, false),  // 尚未實作

    // ========================================
    // 加值功能 - 報表與分析
    // ========================================

    ADVANCED_REPORT("進階報表", "進階營運分析報表", false, 300, true),

    // ========================================
    // 加值功能 - 行銷系統
    // ========================================

    COUPON_SYSTEM("票券系統", "優惠券與折扣券功能", false, 500, true),
    MEMBERSHIP_SYSTEM("會員等級系統", "會員等級與權益管理", false, 400, true),
    POINT_SYSTEM("集點系統", "顧客集點獎勵功能", false, 300, true),

    // ========================================
    // 加值功能 - 自動化
    // ========================================

    AUTO_BIRTHDAY("自動生日祝福", "自動發送生日祝福與優惠", false, 200, false),  // 尚未實作
    AUTO_REMINDER("自動預約提醒", "自動發送預約提醒通知", false, 200, true),
    AUTO_RECALL("自動喚回通知", "自動發送久未到店顧客喚回通知", false, 300, false),  // 尚未實作

    // ========================================
    // 加值功能 - 進階功能
    // ========================================

    AI_ASSISTANT("AI 智慧客服", "AI 自動回覆顧客問題", false, 1000, false),  // 尚未實作
    ADVANCED_CUSTOMER("進階顧客篩選", "進階顧客標籤與篩選功能", false, 300, false),  // 尚未實作
    EXTRA_PUSH("推送額度加購", "加購 LINE 推送訊息額度", false, 100, true),

    // ========================================
    // 加值功能 - 擴展
    // ========================================

    MULTI_BRANCH("多分店管理", "管理多個分店", false, 1000, false),  // 尚未實作
    INVENTORY("庫存管理", "商品庫存管理功能", false, 300, true),
    PRODUCT_SALES("商品銷售", "商品銷售與結帳功能", false, 400, true);

    /**
     * 功能名稱
     */
    private final String name;

    /**
     * 功能描述
     */
    private final String description;

    /**
     * 是否為免費功能
     */
    private final boolean free;

    /**
     * 每月點數消耗
     */
    private final int monthlyPoints;

    /**
     * 是否已實作（未實作的功能不會顯示在功能商店）
     */
    private final boolean implemented;
}
