package com.booking.platform.enums.line;

/**
 * LINE 對話狀態
 *
 * <p>定義預約流程中的對話狀態機
 *
 * <p>預約流程：
 * <pre>
 * IDLE（閒置）
 *   ↓ 用戶說「預約」或點選「開始預約」
 * SELECTING_CATEGORY（選擇分類，有多個分類時）
 *   ↓ 選擇分類（或跳過，若分類 &lt; 2）
 * SELECTING_SERVICE（選擇服務）
 *   ↓ 選擇服務
 * SELECTING_STAFF（選擇員工）
 *   ↓ 選擇員工（或不指定）
 * SELECTING_DATE（選擇日期）
 *   ↓ 選擇日期
 * SELECTING_TIME（選擇時段）
 *   ↓ 選擇時段
 * INPUTTING_NOTE（輸入備註，可跳過）
 *   ↓ 輸入備註或跳過
 * CONFIRMING_BOOKING（確認預約）
 *   ↓ 確認
 * IDLE（完成，回到閒置）
 * </pre>
 *
 * <p>商品購買流程：
 * <pre>
 * IDLE → BROWSING_PRODUCTS → VIEWING_PRODUCT_DETAIL → CONFIRMING_PURCHASE → IDLE
 * </pre>
 *
 * <p>票券領取流程：
 * <pre>
 * IDLE → BROWSING_COUPONS → IDLE
 * </pre>
 *
 * @author Developer
 * @since 1.0.0
 */
public enum ConversationState {

    // ========================================
    // 基本狀態
    // ========================================

    /**
     * 閒置狀態（初始狀態）
     */
    IDLE,

    // ========================================
    // 預約流程
    // ========================================

    /**
     * 選擇服務分類中（有多個分類時）
     */
    SELECTING_CATEGORY,

    /**
     * 選擇服務中
     */
    SELECTING_SERVICE,

    /**
     * 選擇員工中
     */
    SELECTING_STAFF,

    /**
     * 選擇日期中
     */
    SELECTING_DATE,

    /**
     * 選擇時段中
     */
    SELECTING_TIME,

    /**
     * 輸入備註中
     */
    INPUTTING_NOTE,

    /**
     * 確認預約中
     */
    CONFIRMING_BOOKING,

    /**
     * 查看預約中
     */
    VIEWING_BOOKINGS,

    /**
     * 取消預約確認中
     */
    CONFIRMING_CANCEL_BOOKING,

    // ========================================
    // 商品購買流程
    // ========================================

    /**
     * 瀏覽商品中
     */
    BROWSING_PRODUCTS,

    /**
     * 查看商品詳情中
     */
    VIEWING_PRODUCT_DETAIL,

    /**
     * 選擇商品數量中
     */
    SELECTING_QUANTITY,

    /**
     * 確認購買中
     */
    CONFIRMING_PURCHASE,

    // ========================================
    // 票券流程
    // ========================================

    /**
     * 瀏覽可領取票券中
     */
    BROWSING_COUPONS,

    /**
     * 查看已領取票券中
     */
    VIEWING_MY_COUPONS,

    // ========================================
    // 會員資訊
    // ========================================

    /**
     * 查看個人資料中
     */
    VIEWING_PROFILE,

    /**
     * 查看會員資訊中（點數、等級等）
     */
    VIEWING_MEMBER_INFO
}
