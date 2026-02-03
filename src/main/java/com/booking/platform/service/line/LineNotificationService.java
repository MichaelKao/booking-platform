package com.booking.platform.service.line;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.repository.line.LineUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * LINE 通知服務
 *
 * <p>負責向 LINE 用戶發送各種通知
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineNotificationService {

    private final LineUserRepository lineUserRepository;
    private final LineMessageService messageService;
    private final LineFlexMessageBuilder flexMessageBuilder;

    /**
     * 發送預約狀態變更通知
     *
     * @param booking   預約
     * @param newStatus 新狀態
     * @param message   附加訊息（可選）
     */
    @Async
    public void sendBookingStatusNotification(Booking booking, BookingStatus newStatus, String message) {
        try {
            String tenantId = booking.getTenantId();
            String customerId = booking.getCustomerId();

            // 查詢顧客的 LINE User
            Optional<LineUser> lineUserOpt = lineUserRepository
                    .findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customerId);

            if (lineUserOpt.isEmpty()) {
                log.debug("顧客沒有關聯的 LINE 用戶，跳過通知。顧客 ID：{}", customerId);
                return;
            }

            LineUser lineUser = lineUserOpt.get();

            // 檢查是否可以接收訊息
            if (!lineUser.canReceiveMessage()) {
                log.debug("LINE 用戶無法接收訊息（未追蹤或已刪除）。LINE User ID：{}", lineUser.getLineUserId());
                return;
            }

            // 建構通知訊息
            JsonNode notification = flexMessageBuilder.buildBookingStatusNotification(booking, newStatus, message);

            // 發送推播
            messageService.pushFlex(tenantId, lineUser.getLineUserId(), getNotificationAltText(newStatus), notification);

            log.info("已發送預約狀態通知，租戶：{}，預約 ID：{}，新狀態：{}",
                    tenantId, booking.getId(), newStatus);

        } catch (Exception e) {
            log.error("發送預約狀態通知失敗，預約 ID：{}，錯誤：{}", booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * 發送預約修改通知
     *
     * @param booking           預約
     * @param changeDescription 變更描述
     */
    @Async
    public void sendBookingModificationNotification(Booking booking, String changeDescription) {
        try {
            String tenantId = booking.getTenantId();
            String customerId = booking.getCustomerId();

            // 查詢顧客的 LINE User
            Optional<LineUser> lineUserOpt = lineUserRepository
                    .findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customerId);

            if (lineUserOpt.isEmpty()) {
                log.debug("顧客沒有關聯的 LINE 用戶，跳過通知。顧客 ID：{}", customerId);
                return;
            }

            LineUser lineUser = lineUserOpt.get();

            // 檢查是否可以接收訊息
            if (!lineUser.canReceiveMessage()) {
                log.debug("LINE 用戶無法接收訊息（未追蹤或已刪除）。LINE User ID：{}", lineUser.getLineUserId());
                return;
            }

            // 建構通知訊息
            JsonNode notification = flexMessageBuilder.buildBookingModificationNotification(booking, changeDescription);

            // 發送推播
            messageService.pushFlex(tenantId, lineUser.getLineUserId(), "您的預約資訊已更新", notification);

            log.info("已發送預約修改通知，租戶：{}，預約 ID：{}",
                    tenantId, booking.getId());

        } catch (Exception e) {
            log.error("發送預約修改通知失敗，預約 ID：{}，錯誤：{}", booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * 發送預約提醒通知
     *
     * @param booking 預約
     */
    @Async
    public void sendBookingReminder(Booking booking) {
        try {
            String tenantId = booking.getTenantId();
            String customerId = booking.getCustomerId();

            // 查詢顧客的 LINE User
            Optional<LineUser> lineUserOpt = lineUserRepository
                    .findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customerId);

            if (lineUserOpt.isEmpty()) {
                log.debug("顧客沒有關聯的 LINE 用戶，跳過提醒。顧客 ID：{}", customerId);
                return;
            }

            LineUser lineUser = lineUserOpt.get();

            // 檢查是否可以接收訊息
            if (!lineUser.canReceiveMessage()) {
                log.debug("LINE 用戶無法接收訊息（未追蹤或已刪除）。LINE User ID：{}", lineUser.getLineUserId());
                return;
            }

            // 建構提醒訊息
            JsonNode reminder = flexMessageBuilder.buildBookingReminder(booking);

            // 發送推播
            messageService.pushFlex(tenantId, lineUser.getLineUserId(), "預約提醒", reminder);

            log.info("已發送預約提醒，租戶：{}，預約 ID：{}", tenantId, booking.getId());

        } catch (Exception e) {
            log.error("發送預約提醒失敗，預約 ID：{}，錯誤：{}", booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * 發送生日祝福
     *
     * @param tenantId 租戶 ID
     * @param customer 顧客
     * @param message  祝福訊息
     */
    @Async
    public void sendBirthdayGreeting(String tenantId, com.booking.platform.entity.customer.Customer customer, String message) {
        try {
            // 查詢顧客的 LINE User
            Optional<LineUser> lineUserOpt = lineUserRepository
                    .findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customer.getId());

            if (lineUserOpt.isEmpty()) {
                log.debug("顧客沒有關聯的 LINE 用戶，跳過生日祝福。顧客 ID：{}", customer.getId());
                return;
            }

            LineUser lineUser = lineUserOpt.get();

            // 檢查是否可以接收訊息
            if (!lineUser.canReceiveMessage()) {
                log.debug("LINE 用戶無法接收訊息。LINE User ID：{}", lineUser.getLineUserId());
                return;
            }

            // 建構生日祝福訊息
            JsonNode greeting = flexMessageBuilder.buildBirthdayGreeting(customer.getDisplayName(), message);

            // 發送推播
            messageService.pushFlex(tenantId, lineUser.getLineUserId(), "生日快樂！🎂", greeting);

            log.info("已發送生日祝福，租戶：{}，顧客：{}", tenantId, customer.getDisplayName());

        } catch (Exception e) {
            log.error("發送生日祝福失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage(), e);
        }
    }

    /**
     * 發送顧客喚回通知
     *
     * @param tenantId 租戶 ID
     * @param customer 顧客
     * @param message  喚回訊息
     */
    @Async
    public void sendRecallNotification(String tenantId, com.booking.platform.entity.customer.Customer customer, String message) {
        try {
            // 查詢顧客的 LINE User
            Optional<LineUser> lineUserOpt = lineUserRepository
                    .findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customer.getId());

            if (lineUserOpt.isEmpty()) {
                log.debug("顧客沒有關聯的 LINE 用戶，跳過喚回通知。顧客 ID：{}", customer.getId());
                return;
            }

            LineUser lineUser = lineUserOpt.get();

            // 檢查是否可以接收訊息
            if (!lineUser.canReceiveMessage()) {
                log.debug("LINE 用戶無法接收訊息。LINE User ID：{}", lineUser.getLineUserId());
                return;
            }

            // 建構喚回訊息
            JsonNode recall = flexMessageBuilder.buildRecallNotification(customer.getDisplayName(), message);

            // 發送推播
            messageService.pushFlex(tenantId, lineUser.getLineUserId(), "好久不見！想念您了", recall);

            log.info("已發送喚回通知，租戶：{}，顧客：{}", tenantId, customer.getDisplayName());

        } catch (Exception e) {
            log.error("發送喚回通知失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage(), e);
        }
    }

    /**
     * 取得通知替代文字（用於無法顯示 Flex Message 時）
     */
    private String getNotificationAltText(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "您的預約已確認";
            case CANCELLED -> "您的預約已取消";
            case COMPLETED -> "服務已完成，感謝您的光臨";
            case NO_SHOW -> "預約已標記為未到";
            default -> "預約狀態已更新";
        };
    }
}
