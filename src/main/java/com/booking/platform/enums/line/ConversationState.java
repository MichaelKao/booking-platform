package com.booking.platform.enums.line;

/**
 * LINE 對話狀態
 *
 * <p>定義預約流程中的對話狀態機
 *
 * <p>狀態流程：
 * <pre>
 * IDLE（閒置）
 *   ↓ 用戶說「預約」或點選「開始預約」
 * SELECTING_SERVICE（選擇服務）
 *   ↓ 選擇服務
 * SELECTING_STAFF（選擇員工）
 *   ↓ 選擇員工（或不指定）
 * SELECTING_DATE（選擇日期）
 *   ↓ 選擇日期
 * SELECTING_TIME（選擇時段）
 *   ↓ 選擇時段
 * CONFIRMING_BOOKING（確認預約）
 *   ↓ 確認
 * IDLE（完成，回到閒置）
 * </pre>
 *
 * @author Developer
 * @since 1.0.0
 */
public enum ConversationState {

    /**
     * 閒置狀態（初始狀態）
     */
    IDLE,

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
     * 確認預約中
     */
    CONFIRMING_BOOKING,

    /**
     * 查看預約中
     */
    VIEWING_BOOKINGS,

    /**
     * 查看個人資料中
     */
    VIEWING_PROFILE
}
