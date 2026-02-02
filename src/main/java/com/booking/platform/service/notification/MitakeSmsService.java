package com.booking.platform.service.notification;

import com.booking.platform.common.config.SmsConfig;
import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.system.SmsLog;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.SmsStatus;
import com.booking.platform.enums.SmsType;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.SmsLogRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

/**
 * 三竹簡訊服務實作
 *
 * <p>使用三竹科技 SMS API 發送簡訊
 *
 * <p>API 文件：https://sms.mitake.com.tw/
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MitakeSmsService implements SmsService {

    private final SmsConfig smsConfig;
    private final SmsLogRepository smsLogRepository;
    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ========================================
    // 發送簡訊
    // ========================================

    @Override
    @Transactional
    public SmsLog sendSms(String tenantId, String phoneNumber, String message, SmsType smsType) {
        return sendSms(tenantId, phoneNumber, message, smsType, null, null);
    }

    @Override
    @Transactional
    public SmsLog sendSms(String tenantId, String phoneNumber, String message, SmsType smsType,
                          String bookingId, String customerId) {
        log.debug("發送 SMS，租戶：{}，手機：{}，類型：{}", tenantId, phoneNumber, smsType);

        // ========================================
        // 1. 檢查 SMS 是否啟用
        // ========================================
        if (!smsConfig.isEnabled()) {
            log.warn("SMS 功能未啟用");
            return createFailedLog(tenantId, phoneNumber, message, smsType, bookingId, customerId,
                    "DISABLED", "SMS 功能未啟用");
        }

        // ========================================
        // 2. 檢查租戶額度
        // ========================================
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "找不到租戶"));

        if (!tenant.getSmsEnabled()) {
            log.warn("租戶 {} 未啟用 SMS 功能", tenantId);
            return createFailedLog(tenantId, phoneNumber, message, smsType, bookingId, customerId,
                    "TENANT_DISABLED", "租戶未啟用 SMS 功能");
        }

        if (!tenant.hasSmsQuota(1)) {
            log.warn("租戶 {} SMS 額度不足", tenantId);
            return createQuotaExceededLog(tenantId, phoneNumber, message, smsType, bookingId, customerId);
        }

        // ========================================
        // 3. 建立發送記錄
        // ========================================
        SmsLog smsLog = SmsLog.builder()
                .tenantId(tenantId)
                .phoneNumber(normalizePhoneNumber(phoneNumber))
                .message(message)
                .smsType(smsType)
                .bookingId(bookingId)
                .customerId(customerId)
                .status(SmsStatus.SENDING)
                .build();
        smsLogRepository.save(smsLog);

        // ========================================
        // 4. 呼叫三竹 API 發送
        // ========================================
        try {
            String result = callMitakeApi(smsLog.getPhoneNumber(), message);

            // 解析回應
            if (result != null && result.contains("statuscode=1")) {
                // 發送成功
                String msgId = extractMsgId(result);
                smsLog.markSuccess(msgId);
                tenant.useSmsQuota(1);
                tenantRepository.save(tenant);
                log.info("SMS 發送成功，租戶：{}，手機：{}", tenantId, phoneNumber);
            } else {
                // 發送失敗
                smsLog.markFailed("API_ERROR", result);
                log.error("SMS 發送失敗，租戶：{}，回應：{}", tenantId, result);
            }
        } catch (Exception e) {
            smsLog.markFailed("EXCEPTION", e.getMessage());
            log.error("SMS 發送異常，租戶：{}", tenantId, e);
        }

        return smsLogRepository.save(smsLog);
    }

    // ========================================
    // 預約相關簡訊
    // ========================================

    @Override
    @Transactional
    public SmsLog sendBookingConfirmation(Booking booking) {
        String message = buildBookingConfirmationMessage(booking);
        String phoneNumber = getCustomerPhone(booking);

        if (phoneNumber == null) {
            log.warn("無法取得顧客手機號碼，預約：{}", booking.getId());
            return null;
        }

        return sendSms(booking.getTenantId(), phoneNumber, message, SmsType.BOOKING_CONFIRMATION,
                booking.getId(), booking.getCustomerId());
    }

    @Override
    @Transactional
    public SmsLog sendBookingReminder(Booking booking) {
        String message = buildBookingReminderMessage(booking);
        String phoneNumber = getCustomerPhone(booking);

        if (phoneNumber == null) {
            log.warn("無法取得顧客手機號碼，預約：{}", booking.getId());
            return null;
        }

        return sendSms(booking.getTenantId(), phoneNumber, message, SmsType.BOOKING_REMINDER,
                booking.getId(), booking.getCustomerId());
    }

    @Override
    @Transactional
    public SmsLog sendBookingCancelled(Booking booking) {
        String message = buildBookingCancelledMessage(booking);
        String phoneNumber = getCustomerPhone(booking);

        if (phoneNumber == null) {
            log.warn("無法取得顧客手機號碼，預約：{}", booking.getId());
            return null;
        }

        return sendSms(booking.getTenantId(), phoneNumber, message, SmsType.BOOKING_CANCELLED,
                booking.getId(), booking.getCustomerId());
    }

    // ========================================
    // 額度查詢
    // ========================================

    @Override
    public boolean hasQuota(String tenantId, int count) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.hasSmsQuota(count))
                .orElse(false);
    }

    @Override
    public int getRemainingQuota(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getMonthlySmsQuota() - tenant.getMonthlySmsUsed())
                .orElse(0);
    }

    // ========================================
    // 內部方法
    // ========================================

    /**
     * 呼叫三竹 API
     */
    private String callMitakeApi(String phoneNumber, String message) {
        SmsConfig.MitakeConfig mitake = smsConfig.getMitake();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", mitake.getUsername());
        params.add("password", mitake.getPassword());
        params.add("dstaddr", phoneNumber);
        params.add("smbody", message);
        params.add("encoding", "UTF8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                mitake.getApiUrl(),
                request,
                String.class
        );

        return response.getBody();
    }

    /**
     * 從回應中提取訊息 ID
     */
    private String extractMsgId(String response) {
        // 三竹回應格式：[1]\nstatuscode=1\nmsgid=xxxxxxxx
        if (response == null) {
            return null;
        }

        for (String line : response.split("\n")) {
            if (line.startsWith("msgid=")) {
                return line.substring(6).trim();
            }
        }
        return null;
    }

    /**
     * 標準化手機號碼（台灣格式）
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        // 移除所有非數字字元
        String cleaned = phone.replaceAll("[^0-9]", "");

        // 如果是 +886 格式，轉換為 09 開頭
        if (cleaned.startsWith("886")) {
            cleaned = "0" + cleaned.substring(3);
        }

        return cleaned;
    }

    /**
     * 取得顧客手機號碼
     */
    private String getCustomerPhone(Booking booking) {
        if (booking.getCustomerPhone() != null) {
            return booking.getCustomerPhone();
        }

        return customerRepository.findById(booking.getCustomerId())
                .map(customer -> customer.getPhone())
                .orElse(null);
    }

    /**
     * 建立預約確認簡訊內容
     */
    private String buildBookingConfirmationMessage(Booking booking) {
        return String.format(
                "【預約確認】您已成功預約 %s，日期：%s，時間：%s。如需取消請提前聯繫。",
                booking.getServiceName(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getStartTime().format(TIME_FORMATTER)
        );
    }

    /**
     * 建立預約提醒簡訊內容
     */
    private String buildBookingReminderMessage(Booking booking) {
        return String.format(
                "【預約提醒】您預約的 %s 將於 %s %s 開始，請準時到達。",
                booking.getServiceName(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getStartTime().format(TIME_FORMATTER)
        );
    }

    /**
     * 建立預約取消簡訊內容
     */
    private String buildBookingCancelledMessage(Booking booking) {
        return String.format(
                "【預約取消】您於 %s %s 的 %s 預約已取消。如有問題請聯繫我們。",
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getStartTime().format(TIME_FORMATTER),
                booking.getServiceName()
        );
    }

    /**
     * 建立失敗記錄
     */
    private SmsLog createFailedLog(String tenantId, String phoneNumber, String message, SmsType smsType,
                                   String bookingId, String customerId, String errorCode, String errorMessage) {
        SmsLog smsLog = SmsLog.builder()
                .tenantId(tenantId)
                .phoneNumber(normalizePhoneNumber(phoneNumber))
                .message(message)
                .smsType(smsType)
                .bookingId(bookingId)
                .customerId(customerId)
                .status(SmsStatus.FAILED)
                .providerResponseCode(errorCode)
                .providerResponseMessage(errorMessage)
                .build();
        return smsLogRepository.save(smsLog);
    }

    /**
     * 建立額度不足記錄
     */
    private SmsLog createQuotaExceededLog(String tenantId, String phoneNumber, String message, SmsType smsType,
                                          String bookingId, String customerId) {
        SmsLog smsLog = SmsLog.builder()
                .tenantId(tenantId)
                .phoneNumber(normalizePhoneNumber(phoneNumber))
                .message(message)
                .smsType(smsType)
                .bookingId(bookingId)
                .customerId(customerId)
                .status(SmsStatus.QUOTA_EXCEEDED)
                .providerResponseCode("QUOTA_EXCEEDED")
                .providerResponseMessage("SMS 額度不足")
                .build();
        return smsLogRepository.save(smsLog);
    }

    // ========================================
    // 非同步發送
    // ========================================

    /**
     * 非同步發送 SMS
     */
    @Async("taskExecutor")
    @Transactional
    public void sendSmsAsync(String tenantId, String phoneNumber, String message, SmsType smsType,
                             String bookingId, String customerId) {
        sendSms(tenantId, phoneNumber, message, smsType, bookingId, customerId);
    }
}
