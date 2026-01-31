package com.booking.platform.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 錯誤代碼列舉
 *
 * <p>定義系統所有錯誤代碼，方便前端識別和處理
 *
 * <p>代碼規則：
 * <ul>
 *   <li>SYS_xxx - 系統錯誤</li>
 *   <li>AUTH_xxx - 認證授權錯誤</li>
 *   <li>TENANT_xxx - 租戶相關錯誤</li>
 *   <li>BOOKING_xxx - 預約相關錯誤</li>
 *   <li>CUSTOMER_xxx - 顧客相關錯誤</li>
 *   <li>STAFF_xxx - 員工相關錯誤</li>
 *   <li>SERVICE_xxx - 服務相關錯誤</li>
 *   <li>ORDER_xxx - 訂單相關錯誤</li>
 *   <li>POINT_xxx - 點數相關錯誤</li>
 *   <li>LINE_xxx - LINE 相關錯誤</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========================================
    // 系統錯誤（SYS_xxx）
    // ========================================

    SYS_ERROR("SYS_001", "系統錯誤"),
    SYS_PARAM_ERROR("SYS_002", "參數錯誤"),
    SYS_VALIDATION_ERROR("SYS_003", "驗證錯誤"),
    SYS_NOT_FOUND("SYS_004", "資源不存在"),
    SYS_METHOD_NOT_ALLOWED("SYS_005", "方法不允許"),
    SYS_MEDIA_TYPE_NOT_SUPPORTED("SYS_006", "不支援的媒體類型"),
    SYS_TOO_MANY_REQUESTS("SYS_007", "請求過於頻繁"),

    // ========================================
    // 認證授權錯誤（AUTH_xxx）
    // ========================================

    AUTH_UNAUTHORIZED("AUTH_001", "未授權，請先登入"),
    AUTH_FORBIDDEN("AUTH_002", "權限不足"),
    AUTH_TOKEN_EXPIRED("AUTH_003", "Token 已過期"),
    AUTH_TOKEN_INVALID("AUTH_004", "Token 無效"),
    AUTH_LOGIN_FAILED("AUTH_005", "登入失敗，帳號或密碼錯誤"),
    AUTH_ACCOUNT_DISABLED("AUTH_006", "帳號已停用"),
    AUTH_ACCOUNT_LOCKED("AUTH_007", "帳號已鎖定"),

    // ========================================
    // 租戶相關錯誤（TENANT_xxx）
    // ========================================

    TENANT_NOT_FOUND("TENANT_001", "租戶不存在"),
    TENANT_DISABLED("TENANT_002", "租戶已停用"),
    TENANT_CODE_DUPLICATE("TENANT_003", "租戶代碼已存在"),
    TENANT_QUOTA_EXCEEDED("TENANT_004", "已超過配額限制"),
    TENANT_FEATURE_NOT_ENABLED("TENANT_005", "功能未開通"),

    // ========================================
    // 預約相關錯誤（BOOKING_xxx）
    // ========================================

    BOOKING_NOT_FOUND("BOOKING_001", "預約不存在"),
    BOOKING_TIME_CONFLICT("BOOKING_002", "預約時段衝突"),
    BOOKING_TIME_UNAVAILABLE("BOOKING_003", "該時段無法預約"),
    BOOKING_ALREADY_CANCELLED("BOOKING_004", "預約已取消"),
    BOOKING_CANNOT_CANCEL("BOOKING_005", "無法取消此預約"),
    BOOKING_CANNOT_MODIFY("BOOKING_006", "無法修改此預約"),
    BOOKING_STATUS_ERROR("BOOKING_007", "預約狀態錯誤"),

    // ========================================
    // 顧客相關錯誤（CUSTOMER_xxx）
    // ========================================

    CUSTOMER_NOT_FOUND("CUSTOMER_001", "顧客不存在"),
    CUSTOMER_PHONE_DUPLICATE("CUSTOMER_002", "手機號碼已存在"),
    CUSTOMER_LINE_ID_DUPLICATE("CUSTOMER_003", "LINE ID 已綁定"),
    CUSTOMER_BLACKLISTED("CUSTOMER_004", "顧客已被列入黑名單"),

    // ========================================
    // 會員等級相關錯誤（MEMBERSHIP_xxx）
    // ========================================

    MEMBERSHIP_LEVEL_NOT_FOUND("MEMBERSHIP_001", "會員等級不存在"),
    MEMBERSHIP_LEVEL_NAME_DUPLICATE("MEMBERSHIP_002", "會員等級名稱已存在"),
    MEMBERSHIP_LEVEL_DEFAULT_CANNOT_DELETE("MEMBERSHIP_003", "無法刪除預設會員等級"),
    MEMBERSHIP_LEVEL_HAS_MEMBERS("MEMBERSHIP_004", "此會員等級下尚有會員"),

    // ========================================
    // 員工相關錯誤（STAFF_xxx）
    // ========================================

    STAFF_NOT_FOUND("STAFF_001", "員工不存在"),
    STAFF_LIMIT_EXCEEDED("STAFF_002", "員工數量已達上限"),
    STAFF_UNAVAILABLE("STAFF_003", "員工無法服務"),
    STAFF_ON_LEAVE("STAFF_004", "員工休假中"),

    // ========================================
    // 服務相關錯誤（SERVICE_xxx）
    // ========================================

    SERVICE_NOT_FOUND("SERVICE_001", "服務項目不存在"),
    SERVICE_UNAVAILABLE("SERVICE_002", "服務暫停中"),
    SERVICE_NAME_DUPLICATE("SERVICE_003", "服務名稱已存在"),

    // ========================================
    // 訂單相關錯誤（ORDER_xxx）
    // ========================================

    ORDER_NOT_FOUND("ORDER_001", "訂單不存在"),
    ORDER_ALREADY_PAID("ORDER_002", "訂單已付款"),
    ORDER_PAYMENT_FAILED("ORDER_003", "付款失敗"),
    ORDER_CANNOT_CANCEL("ORDER_004", "無法取消此訂單"),

    // ========================================
    // 點數相關錯誤（POINT_xxx）
    // ========================================

    POINT_INSUFFICIENT("POINT_001", "點數不足"),
    POINT_TOPUP_NOT_FOUND("POINT_002", "儲值申請不存在"),
    POINT_TOPUP_ALREADY_PROCESSED("POINT_003", "儲值申請已處理"),

    // ========================================
    // 功能相關錯誤（FEATURE_xxx）
    // ========================================

    FEATURE_NOT_FOUND("FEATURE_001", "功能不存在"),
    FEATURE_ALREADY_ENABLED("FEATURE_002", "功能已啟用"),
    FEATURE_NOT_ENABLED("FEATURE_003", "功能未啟用"),
    FEATURE_NOT_SUBSCRIBED("FEATURE_004", "尚未訂閱此功能"),

    // ========================================
    // 票券相關錯誤（COUPON_xxx）
    // ========================================

    COUPON_NOT_FOUND("COUPON_001", "票券不存在"),
    COUPON_EXPIRED("COUPON_002", "票券已過期"),
    COUPON_USED("COUPON_003", "票券已使用"),
    COUPON_NOT_APPLICABLE("COUPON_004", "票券不適用"),
    COUPON_NAME_DUPLICATE("COUPON_005", "票券名稱已存在"),
    COUPON_CANNOT_ISSUE("COUPON_006", "票券無法發放"),
    COUPON_LIMIT_EXCEEDED("COUPON_007", "已達領取上限"),

    // ========================================
    // 行銷活動相關錯誤（CAMPAIGN_xxx）
    // ========================================

    CAMPAIGN_NOT_FOUND("CAMPAIGN_001", "活動不存在"),
    CAMPAIGN_NAME_DUPLICATE("CAMPAIGN_002", "活動名稱已存在"),
    CAMPAIGN_ALREADY_ENDED("CAMPAIGN_003", "活動已結束"),

    // ========================================
    // 商品相關錯誤（PRODUCT_xxx）
    // ========================================

    PRODUCT_NOT_FOUND("PRODUCT_001", "商品不存在"),
    PRODUCT_NAME_DUPLICATE("PRODUCT_002", "商品名稱已存在"),
    PRODUCT_SKU_DUPLICATE("PRODUCT_003", "商品編號已存在"),
    PRODUCT_STOCK_INSUFFICIENT("PRODUCT_004", "庫存不足"),
    PRODUCT_NOT_ON_SALE("PRODUCT_005", "商品未上架"),

    // ========================================
    // LINE 相關錯誤（LINE_xxx）
    // ========================================

    LINE_SEND_FAILED("LINE_001", "LINE 訊息發送失敗"),
    LINE_WEBHOOK_ERROR("LINE_002", "LINE Webhook 處理失敗"),
    LINE_USER_NOT_FOUND("LINE_003", "找不到 LINE 使用者"),
    LINE_CONFIG_NOT_FOUND("LINE_004", "LINE 設定不存在"),
    LINE_CONFIG_INVALID("LINE_005", "LINE 設定無效"),
    LINE_CONFIG_NOT_ACTIVE("LINE_006", "LINE Bot 未啟用"),
    LINE_SIGNATURE_INVALID("LINE_007", "LINE 簽名驗證失敗"),
    LINE_CHANNEL_ID_DUPLICATE("LINE_008", "Channel ID 已被其他店家使用"),
    LINE_PUSH_QUOTA_EXCEEDED("LINE_009", "LINE 推送額度已用完"),
    LINE_CONVERSATION_EXPIRED("LINE_010", "對話已過期，請重新開始"),
    LINE_BOOKING_DISABLED("LINE_011", "LINE 預約功能已停用"),
    LINE_ENCRYPTION_ERROR("LINE_012", "加密處理失敗");

    // ========================================
    // 欄位
    // ========================================

    /**
     * 錯誤代碼
     */
    private final String code;

    /**
     * 預設錯誤訊息
     */
    private final String defaultMessage;
}
