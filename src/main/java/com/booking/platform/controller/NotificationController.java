package com.booking.platform.controller;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.service.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 通知控制器
 *
 * <p>提供 Server-Sent Events 端點，用於即時推送通知到店家後台
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final SseNotificationService sseNotificationService;

    /**
     * 訂閱 SSE 通知串流
     *
     * <p>客戶端連線後會持續接收通知事件
     *
     * @return SSE Emitter
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            log.warn("SSE 訂閱失敗：缺少租戶 ID");
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }

        log.info("SSE 訂閱，租戶：{}", tenantId);
        return sseNotificationService.subscribe(tenantId);
    }
}
