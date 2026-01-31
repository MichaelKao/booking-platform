package com.booking.platform.enums.line;

/**
 * LINE 事件類型
 *
 * <p>對應 LINE Messaging API 的 Webhook 事件類型
 *
 * @author Developer
 * @since 1.0.0
 * @see <a href="https://developers.line.biz/en/reference/messaging-api/#webhook-event-objects">LINE Webhook Events</a>
 */
public enum LineEventType {

    /**
     * 訊息事件
     * <p>用戶發送訊息時觸發
     */
    MESSAGE,

    /**
     * 追蹤事件
     * <p>用戶加入好友或解除封鎖時觸發
     */
    FOLLOW,

    /**
     * 取消追蹤事件
     * <p>用戶封鎖帳號時觸發
     */
    UNFOLLOW,

    /**
     * Postback 事件
     * <p>用戶點擊按鈕或選單項目時觸發
     */
    POSTBACK,

    /**
     * 加入群組/聊天室事件
     */
    JOIN,

    /**
     * 離開群組/聊天室事件
     */
    LEAVE,

    /**
     * 成員加入事件
     */
    MEMBER_JOINED,

    /**
     * 成員離開事件
     */
    MEMBER_LEFT,

    /**
     * 未知事件類型
     */
    UNKNOWN;

    /**
     * 從字串解析事件類型
     *
     * @param value 事件類型字串
     * @return 對應的列舉值，若無法識別則返回 UNKNOWN
     */
    public static LineEventType fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
