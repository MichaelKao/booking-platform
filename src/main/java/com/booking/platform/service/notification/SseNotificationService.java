package com.booking.platform.service.notification;

import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.dto.response.ProductOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 通知服務
 *
 * <p>管理 Server-Sent Events 連線並推送即時通知
 *
 * <p>功能：
 * <ul>
 *   <li>管理租戶的 SSE 連線</li>
 *   <li>推送新預約通知</li>
 *   <li>推送預約狀態變更通知</li>
 *   <li>推送預約編輯通知</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseNotificationService {

    private final ObjectMapper objectMapper;

    /**
     * 租戶 SSE 連線映射表
     * Key: tenantId, Value: 該租戶的所有 SSE 連線
     */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 連線超時時間（30 分鐘）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // ========================================
    // 連線管理
    // ========================================

    /**
     * 訂閱 SSE 通知
     *
     * @param tenantId 租戶 ID
     * @return SSE Emitter
     */
    public SseEmitter subscribe(String tenantId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 取得或建立該租戶的連線列表
        List<SseEmitter> tenantEmitters = emitters.computeIfAbsent(
                tenantId,
                k -> new CopyOnWriteArrayList<>()
        );

        // 新增連線
        tenantEmitters.add(emitter);

        log.info("SSE 連線建立，租戶：{}，目前連線數：{}", tenantId, tenantEmitters.size());

        // 連線完成時移除
        emitter.onCompletion(() -> {
            tenantEmitters.remove(emitter);
            log.debug("SSE 連線完成，租戶：{}，剩餘連線數：{}", tenantId, tenantEmitters.size());
        });

        // 連線超時時移除
        emitter.onTimeout(() -> {
            tenantEmitters.remove(emitter);
            log.debug("SSE 連線超時，租戶：{}，剩餘連線數：{}", tenantId, tenantEmitters.size());
        });

        // 連線錯誤時移除
        emitter.onError(e -> {
            tenantEmitters.remove(emitter);
            log.debug("SSE 連線錯誤，租戶：{}，錯誤：{}", tenantId, e.getMessage());
        });

        // 發送連線成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.error("發送連線確認失敗，租戶：{}", tenantId, e);
        }

        return emitter;
    }

    // ========================================
    // 通知推送
    // ========================================

    /**
     * 推送新預約通知
     *
     * @param tenantId 租戶 ID
     * @param booking  預約資訊
     */
    public void notifyNewBooking(String tenantId, BookingResponse booking) {
        sendEvent(tenantId, "new_booking", booking);
    }

    /**
     * 推送預約更新通知
     *
     * @param tenantId 租戶 ID
     * @param booking  預約資訊
     */
    public void notifyBookingUpdated(String tenantId, BookingResponse booking) {
        sendEvent(tenantId, "booking_updated", booking);
    }

    /**
     * 推送預約狀態變更通知
     *
     * @param tenantId  租戶 ID
     * @param booking   預約資訊
     * @param newStatus 新狀態
     */
    public void notifyBookingStatusChanged(String tenantId, BookingResponse booking, String newStatus) {
        Map<String, Object> data = Map.of(
                "booking", booking,
                "newStatus", newStatus
        );
        sendEvent(tenantId, "booking_status_changed", data);
    }

    /**
     * 推送預約取消通知
     *
     * @param tenantId 租戶 ID
     * @param booking  預約資訊
     */
    public void notifyBookingCancelled(String tenantId, BookingResponse booking) {
        sendEvent(tenantId, "booking_cancelled", booking);
    }

    // ========================================
    // 商品訂單通知
    // ========================================

    /**
     * 推送新商品訂單通知
     */
    public void notifyNewProductOrder(String tenantId, ProductOrderResponse order) {
        sendEvent(tenantId, "new_product_order", order);
    }

    /**
     * 推送商品訂單狀態變更通知
     */
    public void notifyProductOrderStatusChanged(String tenantId, ProductOrderResponse order, String newStatus) {
        sendEvent(tenantId, "product_order_status_changed", Map.of("order", order, "newStatus", newStatus));
    }

    // ========================================
    // 票券通知
    // ========================================

    /**
     * 推送票券領取通知
     */
    public void notifyCouponClaimed(String tenantId, Map<String, Object> data) {
        sendEvent(tenantId, "coupon_claimed", data);
    }

    // ========================================
    // 顧客通知
    // ========================================

    /**
     * 推送新顧客通知
     */
    public void notifyNewCustomer(String tenantId, Map<String, Object> data) {
        sendEvent(tenantId, "new_customer", data);
    }

    // ========================================
    // 內部方法
    // ========================================

    /**
     * 發送 SSE 事件到指定租戶的所有連線
     *
     * @param tenantId  租戶 ID
     * @param eventName 事件名稱
     * @param data      事件資料
     */
    private void sendEvent(String tenantId, String eventName, Object data) {
        List<SseEmitter> tenantEmitters = emitters.get(tenantId);

        if (tenantEmitters == null || tenantEmitters.isEmpty()) {
            log.debug("沒有活躍的 SSE 連線，租戶：{}，事件：{}", tenantId, eventName);
            return;
        }

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("序列化 SSE 資料失敗，租戶：{}，事件：{}", tenantId, eventName, e);
            return;
        }

        log.info("推送 SSE 事件，租戶：{}，事件：{}，連線數：{}", tenantId, eventName, tenantEmitters.size());

        // 建立要移除的失效連線列表
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : tenantEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonData));
            } catch (IOException e) {
                log.debug("SSE 發送失敗，標記移除，租戶：{}", tenantId);
                deadEmitters.add(emitter);
            }
        }

        // 移除失效連線
        tenantEmitters.removeAll(deadEmitters);
    }

    /**
     * 取得租戶目前的連線數
     *
     * @param tenantId 租戶 ID
     * @return 連線數
     */
    public int getConnectionCount(String tenantId) {
        List<SseEmitter> tenantEmitters = emitters.get(tenantId);
        return tenantEmitters != null ? tenantEmitters.size() : 0;
    }
}
