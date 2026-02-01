package com.booking.platform.service.line;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.dto.request.CreateBookingRequest;
import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.enums.line.ConversationState;
import com.booking.platform.enums.line.LineEventType;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LINE Webhook 處理服務
 *
 * <p>處理 LINE 平台發送的各種事件
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineWebhookService {

    // ========================================
    // 依賴注入
    // ========================================

    private final ObjectMapper objectMapper;
    private final TenantLineConfigRepository lineConfigRepository;
    private final LineUserService lineUserService;
    private final LineMessageService messageService;
    private final LineConversationService conversationService;
    private final LineFlexMessageBuilder flexMessageBuilder;
    private final BookingService bookingService;

    // ========================================
    // 關鍵字
    // ========================================

    private static final String[] BOOKING_KEYWORDS = {"預約", "訂位", "預訂", "book", "booking"};
    private static final String[] CANCEL_KEYWORDS = {"取消", "cancel"};
    private static final String[] HELP_KEYWORDS = {"幫助", "help", "說明"};

    // ========================================
    // 公開方法
    // ========================================

    /**
     * 處理 Webhook 事件
     *
     * @param tenantId 租戶 ID
     * @param body     請求 body（JSON 字串）
     */
    @Async
    public void processWebhook(String tenantId, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode events = root.get("events");

            if (events == null || !events.isArray()) {
                log.debug("沒有事件需要處理");
                return;
            }

            for (JsonNode event : events) {
                processEvent(tenantId, event);
            }

        } catch (Exception e) {
            log.error("處理 Webhook 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
        }
    }

    // ========================================
    // 事件處理
    // ========================================

    /**
     * 處理單一事件
     */
    private void processEvent(String tenantId, JsonNode event) {
        String eventType = event.path("type").asText();
        LineEventType type = LineEventType.fromValue(eventType);

        log.debug("處理事件，租戶：{}，類型：{}", tenantId, type);

        switch (type) {
            case MESSAGE -> handleMessageEvent(tenantId, event);
            case POSTBACK -> handlePostbackEvent(tenantId, event);
            case FOLLOW -> handleFollowEvent(tenantId, event);
            case UNFOLLOW -> handleUnfollowEvent(tenantId, event);
            default -> log.debug("未處理的事件類型：{}", type);
        }
    }

    /**
     * 處理訊息事件
     */
    private void handleMessageEvent(String tenantId, JsonNode event) {
        String replyToken = event.path("replyToken").asText();
        String userId = event.path("source").path("userId").asText();
        JsonNode message = event.path("message");
        String messageType = message.path("type").asText();

        // 確保用戶存在
        ensureUserExists(tenantId, userId, event);

        // 目前只處理文字訊息
        if (!"text".equals(messageType)) {
            replyDefaultMessage(tenantId, replyToken);
            return;
        }

        String text = message.path("text").asText().trim().toLowerCase();

        // 檢查是否為預約關鍵字
        if (matchesKeyword(text, BOOKING_KEYWORDS)) {
            startBookingFlow(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為取消關鍵字
        if (matchesKeyword(text, CANCEL_KEYWORDS)) {
            cancelCurrentFlow(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為幫助關鍵字
        if (matchesKeyword(text, HELP_KEYWORDS)) {
            replyHelpMessage(tenantId, replyToken);
            return;
        }

        // 根據當前對話狀態處理
        ConversationContext context = conversationService.getContext(tenantId, userId);
        handleContextualMessage(tenantId, userId, replyToken, text, context);
    }

    /**
     * 處理 Postback 事件
     */
    private void handlePostbackEvent(String tenantId, JsonNode event) {
        String replyToken = event.path("replyToken").asText();
        String userId = event.path("source").path("userId").asText();
        String data = event.path("postback").path("data").asText();

        log.debug("Postback 事件，租戶：{}，用戶：{}，資料：{}", tenantId, userId, data);

        // 確保用戶存在
        ensureUserExists(tenantId, userId, event);

        // 解析 Postback 資料
        Map<String, String> params = parsePostbackData(data);
        String action = params.get("action");

        if (action == null) {
            log.warn("Postback 缺少 action 參數，資料：{}", data);
            return;
        }

        switch (action) {
            case "start_booking" -> startBookingFlow(tenantId, userId, replyToken);
            case "select_service" -> handleSelectService(tenantId, userId, replyToken, params);
            case "select_staff" -> handleSelectStaff(tenantId, userId, replyToken, params);
            case "select_date" -> handleSelectDate(tenantId, userId, replyToken, params);
            case "select_time" -> handleSelectTime(tenantId, userId, replyToken, params);
            case "confirm_booking" -> handleConfirmBooking(tenantId, userId, replyToken);
            case "cancel_booking" -> cancelCurrentFlow(tenantId, userId, replyToken);
            case "go_back" -> handleGoBack(tenantId, userId, replyToken);
            case "view_bookings" -> handleViewBookings(tenantId, userId, replyToken);
            case "main_menu" -> replyMainMenu(tenantId, userId, replyToken);
            default -> log.debug("未處理的 action：{}", action);
        }
    }

    /**
     * 處理追蹤事件
     */
    private void handleFollowEvent(String tenantId, JsonNode event) {
        String userId = event.path("source").path("userId").asText();
        String replyToken = event.path("replyToken").asText();

        log.info("用戶追蹤，租戶：{}，用戶：{}", tenantId, userId);

        // 取得用戶資料
        JsonNode profile = messageService.getProfile(tenantId, userId);
        String displayName = profile != null ? profile.path("displayName").asText() : null;
        String pictureUrl = profile != null ? profile.path("pictureUrl").asText() : null;
        String statusMessage = profile != null ? profile.path("statusMessage").asText() : null;

        // 建立或更新用戶
        lineUserService.handleFollow(tenantId, userId, displayName, pictureUrl, statusMessage);

        // 回覆歡迎訊息
        replyWelcomeMessage(tenantId, userId, replyToken);
    }

    /**
     * 處理取消追蹤事件
     */
    private void handleUnfollowEvent(String tenantId, JsonNode event) {
        String userId = event.path("source").path("userId").asText();

        log.info("用戶取消追蹤，租戶：{}，用戶：{}", tenantId, userId);

        lineUserService.handleUnfollow(tenantId, userId);

        // 清除對話狀態
        conversationService.deleteContext(tenantId, userId);
    }

    // ========================================
    // 預約流程處理
    // ========================================

    /**
     * 開始預約流程
     */
    private void startBookingFlow(String tenantId, String userId, String replyToken) {
        // 檢查是否啟用預約功能
        Optional<TenantLineConfig> configOpt = lineConfigRepository.findByTenantId(tenantId);
        if (configOpt.isEmpty() || !configOpt.get().getBookingEnabled()) {
            messageService.replyText(tenantId, replyToken, "抱歉，目前暫不開放線上預約。請直接聯繫店家。");
            return;
        }

        // 轉換狀態
        conversationService.startBooking(tenantId, userId);

        // 回覆服務選單
        JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
        messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
    }

    /**
     * 處理選擇服務
     */
    private void handleSelectService(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String serviceId = params.get("serviceId");
        String serviceName = params.get("serviceName");
        String duration = params.get("duration");
        String price = params.get("price");

        conversationService.setSelectedService(
                tenantId, userId, serviceId, serviceName,
                duration != null ? Integer.parseInt(duration) : null,
                price != null ? Integer.parseInt(price) : null
        );

        // 回覆員工選單
        JsonNode staffMenu = flexMessageBuilder.buildStaffMenu(tenantId, serviceId);
        messageService.replyFlex(tenantId, replyToken, "請選擇服務人員", staffMenu);
    }

    /**
     * 處理選擇員工
     */
    private void handleSelectStaff(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String staffId = params.get("staffId");
        String staffName = params.get("staffName");

        conversationService.setSelectedStaff(tenantId, userId, staffId, staffName);

        // 回覆日期選單
        JsonNode dateMenu = flexMessageBuilder.buildDateMenu();
        messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
    }

    /**
     * 處理選擇日期
     */
    private void handleSelectDate(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String dateStr = params.get("date");
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

        conversationService.setSelectedDate(tenantId, userId, date);

        // 取得對話上下文
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 回覆時段選單
        JsonNode timeMenu = flexMessageBuilder.buildTimeMenu(
                tenantId,
                context.getSelectedStaffId(),
                date,
                context.getSelectedServiceDuration()
        );
        messageService.replyFlex(tenantId, replyToken, "請選擇時段", timeMenu);
    }

    /**
     * 處理選擇時間
     */
    private void handleSelectTime(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String timeStr = params.get("time");
        LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_TIME);

        conversationService.setSelectedTime(tenantId, userId, time);

        // 取得對話上下文
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 回覆確認訊息
        JsonNode confirmMessage = flexMessageBuilder.buildBookingConfirmation(context);
        messageService.replyFlex(tenantId, replyToken, "請確認預約資訊", confirmMessage);
    }

    /**
     * 處理確認預約
     */
    private void handleConfirmBooking(String tenantId, String userId, String replyToken) {
        ConversationContext context = conversationService.getContext(tenantId, userId);

        if (!context.canConfirmBooking()) {
            messageService.replyText(tenantId, replyToken, "預約資訊不完整，請重新開始預約。");
            conversationService.reset(tenantId, userId);
            return;
        }

        try {
            // ========================================
            // 設定 TenantContext（因為是 @Async 方法）
            // ========================================
            TenantContext.setTenantId(tenantId);

            // ========================================
            // 建立預約請求
            // ========================================
            CreateBookingRequest request = CreateBookingRequest.builder()
                    .bookingDate(context.getSelectedDate())
                    .startTime(context.getSelectedTime())
                    .serviceId(context.getSelectedServiceId())
                    .staffId(context.getSelectedStaffId())
                    .customerId(context.getCustomerId())
                    .source("LINE")
                    .build();

            // ========================================
            // 呼叫 BookingService 建立預約
            // ========================================
            BookingResponse booking = bookingService.create(request);

            log.info("LINE 預約建立成功，租戶：{}，預約 ID：{}", tenantId, booking.getId());

            // 增加預約次數
            lineUserService.incrementBookingCount(tenantId, userId);

            // 回覆成功訊息
            JsonNode successMessage = flexMessageBuilder.buildBookingSuccess(
                    context,
                    booking.getId()
            );
            messageService.replyFlex(tenantId, replyToken, "預約成功", successMessage);

        } catch (Exception e) {
            log.error("LINE 預約建立失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "預約失敗：" + e.getMessage() + "\n請稍後再試或聯繫店家。");
        } finally {
            // 清除 TenantContext
            TenantContext.clear();
            // 重置對話
            conversationService.reset(tenantId, userId);
        }
    }

    /**
     * 處理返回上一步
     */
    private void handleGoBack(String tenantId, String userId, String replyToken) {
        ConversationContext context = conversationService.goBack(tenantId, userId);

        switch (context.getState()) {
            case SELECTING_SERVICE -> {
                JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
                messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
            }
            case SELECTING_STAFF -> {
                JsonNode staffMenu = flexMessageBuilder.buildStaffMenu(tenantId, context.getSelectedServiceId());
                messageService.replyFlex(tenantId, replyToken, "請選擇服務人員", staffMenu);
            }
            case SELECTING_DATE -> {
                JsonNode dateMenu = flexMessageBuilder.buildDateMenu();
                messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
            }
            case SELECTING_TIME -> {
                JsonNode timeMenu = flexMessageBuilder.buildTimeMenu(
                        tenantId, context.getSelectedStaffId(),
                        context.getSelectedDate(), context.getSelectedServiceDuration()
                );
                messageService.replyFlex(tenantId, replyToken, "請選擇時段", timeMenu);
            }
            default -> replyMainMenu(tenantId, userId, replyToken);
        }
    }

    /**
     * 取消當前流程
     */
    private void cancelCurrentFlow(String tenantId, String userId, String replyToken) {
        conversationService.reset(tenantId, userId);
        messageService.replyText(tenantId, replyToken, "已取消。如需預約，請輸入「預約」或點選下方選單。");
    }

    /**
     * 處理查看預約
     */
    private void handleViewBookings(String tenantId, String userId, String replyToken) {
        // TODO: 實際查詢預約列表
        messageService.replyText(tenantId, replyToken, "此功能開發中，敬請期待！");
    }

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 確保用戶存在
     */
    private void ensureUserExists(String tenantId, String userId, JsonNode event) {
        Optional<LineUser> userOpt = lineUserService.findByLineUserId(tenantId, userId);

        if (userOpt.isEmpty()) {
            // 取得用戶資料
            JsonNode profile = messageService.getProfile(tenantId, userId);
            String displayName = profile != null ? profile.path("displayName").asText() : null;
            String pictureUrl = profile != null ? profile.path("pictureUrl").asText() : null;
            String statusMessage = profile != null ? profile.path("statusMessage").asText() : null;

            lineUserService.getOrCreateUser(tenantId, userId, displayName, pictureUrl, statusMessage);
        }
    }

    /**
     * 根據對話狀態處理訊息
     */
    private void handleContextualMessage(String tenantId, String userId, String replyToken,
                                         String text, ConversationContext context) {
        // 如果不在對話中，回覆預設訊息
        if (context.getState() == ConversationState.IDLE) {
            replyDefaultMessage(tenantId, replyToken);
        } else {
            // 在對話中但收到非 Postback 訊息，提示用戶
            messageService.replyText(tenantId, replyToken,
                    "請點選上方選項繼續操作，或輸入「取消」取消預約。");
        }
    }

    /**
     * 回覆歡迎訊息
     */
    private void replyWelcomeMessage(String tenantId, String userId, String replyToken) {
        Optional<TenantLineConfig> configOpt = lineConfigRepository.findByTenantId(tenantId);
        String welcomeMessage = configOpt.map(TenantLineConfig::getWelcomeMessage)
                .orElse("歡迎加入！請點選下方選單開始預約服務。");

        JsonNode mainMenu = flexMessageBuilder.buildMainMenu(tenantId);
        messageService.replyFlex(tenantId, replyToken, welcomeMessage, mainMenu);
    }

    /**
     * 回覆預設訊息
     */
    private void replyDefaultMessage(String tenantId, String replyToken) {
        Optional<TenantLineConfig> configOpt = lineConfigRepository.findByTenantId(tenantId);

        if (configOpt.isPresent() && configOpt.get().getAutoReplyEnabled()) {
            String defaultReply = configOpt.get().getDefaultReply();
            messageService.replyText(tenantId, replyToken, defaultReply);
        }
    }

    /**
     * 回覆主選單
     */
    private void replyMainMenu(String tenantId, String userId, String replyToken) {
        JsonNode mainMenu = flexMessageBuilder.buildMainMenu(tenantId);
        messageService.replyFlex(tenantId, replyToken, "請選擇服務", mainMenu);
    }

    /**
     * 回覆幫助訊息
     */
    private void replyHelpMessage(String tenantId, String replyToken) {
        String helpText = """
                使用說明：

                1. 輸入「預約」開始預約服務
                2. 依照提示選擇服務、人員、日期和時段
                3. 確認預約資訊後即可完成預約

                如需取消目前操作，請輸入「取消」
                """;
        messageService.replyText(tenantId, replyToken, helpText);
    }

    /**
     * 檢查是否匹配關鍵字
     */
    private boolean matchesKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 Postback 資料
     */
    private Map<String, String> parsePostbackData(String data) {
        Map<String, String> params = new HashMap<>();

        if (data == null || data.isEmpty()) {
            return params;
        }

        String[] pairs = data.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        return params;
    }
}
