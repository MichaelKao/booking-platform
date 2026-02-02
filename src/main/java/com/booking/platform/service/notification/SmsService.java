package com.booking.platform.service.notification;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.system.SmsLog;
import com.booking.platform.enums.SmsType;

/**
 * SMS 服務介面
 *
 * <p>定義 SMS 發送功能的標準介面
 *
 * @author Developer
 * @since 1.0.0
 */
public interface SmsService {

    /**
     * 發送簡訊
     *
     * @param tenantId    租戶 ID
     * @param phoneNumber 手機號碼
     * @param message     簡訊內容
     * @param smsType     簡訊類型
     * @return SMS 發送記錄
     */
    SmsLog sendSms(String tenantId, String phoneNumber, String message, SmsType smsType);

    /**
     * 發送簡訊（帶關聯資訊）
     *
     * @param tenantId    租戶 ID
     * @param phoneNumber 手機號碼
     * @param message     簡訊內容
     * @param smsType     簡訊類型
     * @param bookingId   關聯預約 ID
     * @param customerId  關聯顧客 ID
     * @return SMS 發送記錄
     */
    SmsLog sendSms(String tenantId, String phoneNumber, String message, SmsType smsType,
                   String bookingId, String customerId);

    /**
     * 發送預約確認簡訊
     *
     * @param booking 預約資訊
     * @return SMS 發送記錄
     */
    SmsLog sendBookingConfirmation(Booking booking);

    /**
     * 發送預約提醒簡訊
     *
     * @param booking 預約資訊
     * @return SMS 發送記錄
     */
    SmsLog sendBookingReminder(Booking booking);

    /**
     * 發送預約取消簡訊
     *
     * @param booking 預約資訊
     * @return SMS 發送記錄
     */
    SmsLog sendBookingCancelled(Booking booking);

    /**
     * 檢查租戶 SMS 額度是否足夠
     *
     * @param tenantId 租戶 ID
     * @param count    需要發送的數量
     * @return true 表示額度足夠
     */
    boolean hasQuota(String tenantId, int count);

    /**
     * 取得租戶剩餘 SMS 額度
     *
     * @param tenantId 租戶 ID
     * @return 剩餘額度
     */
    int getRemainingQuota(String tenantId);
}
