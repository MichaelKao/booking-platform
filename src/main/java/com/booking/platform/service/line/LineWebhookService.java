package com.booking.platform.service.line;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.dto.request.CreateBookingRequest;
import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.enums.line.ConversationState;
import com.booking.platform.enums.line.LineEventType;
import com.booking.platform.entity.catalog.ServiceCategory;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.CouponRepository;
import com.booking.platform.repository.CouponInstanceRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.ServiceCategoryRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.MembershipLevelRepository;
import com.booking.platform.repository.ProductRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.BookingService;
import com.booking.platform.service.CouponService;
import com.booking.platform.service.ai.AiAssistantService;
import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.entity.marketing.CouponInstance;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.product.Product;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.ProductStatus;
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
import java.util.List;
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
    private final BookingRepository bookingRepository;
    private final CouponRepository couponRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final CouponInstanceRepository couponInstanceRepository;
    private final CustomerRepository customerRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final ProductRepository productRepository;
    private final CouponService couponService;
    private final AiAssistantService aiAssistantService;
    private final com.booking.platform.service.ProductOrderService productOrderService;

    // ========================================
    // 關鍵字
    // ========================================

    private static final String[] BOOKING_KEYWORDS = {"預約", "訂位", "預訂", "book", "booking"};
    private static final String[] CANCEL_KEYWORDS = {"取消", "cancel"};
    private static final String[] HELP_KEYWORDS = {"幫助", "help", "說明"};
    private static final String[] COUPON_KEYWORDS = {"票券", "優惠券", "coupon"};
    private static final String[] PRODUCT_KEYWORDS = {"商品", "購買", "product", "shop"};
    private static final String[] MEMBER_KEYWORDS = {"會員", "點數", "member", "points"};

    // ========================================
    // 公開方法
    // ========================================

    /**
     * 調試會員資訊功能（同步執行，用於排查問題）
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 調試結果
     */
    public Map<String, Object> debugMemberInfo(String tenantId, String lineUserId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("lineUserId", lineUserId);

        try {
            // 1. 取得顧客 ID
            String customerId = lineUserService.getCustomerId(tenantId, lineUserId);
            result.put("customerId", customerId);

            if (customerId == null) {
                result.put("error", "用戶尚未成為會員（無 customerId）");
                return result;
            }

            // 2. 查詢顧客資料
            Optional<Customer> customerOpt = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId);
            if (customerOpt.isEmpty()) {
                result.put("error", "找不到顧客資料");
                return result;
            }

            Customer customer = customerOpt.get();
            result.put("customerName", customer.getName());
            result.put("customerPhone", customer.getPhone());
            result.put("pointBalance", customer.getPointBalance());
            result.put("membershipLevelId", customer.getMembershipLevelId());

            // 3. 查詢預約統計
            long bookingCount = bookingRepository.countByCustomerIdAndTenantId(customerId, tenantId);
            result.put("bookingCount", bookingCount);

            // 4. 查詢會員等級
            if (customer.getMembershipLevelId() != null) {
                String levelName = membershipLevelRepository
                        .findById(customer.getMembershipLevelId())
                        .map(level -> level.getName())
                        .orElse(null);
                result.put("membershipLevelName", levelName);
            }

            // 5. 嘗試建構 Flex Message
            try {
                JsonNode memberInfo = flexMessageBuilder.buildMemberInfo(customer, bookingCount,
                        (String) result.get("membershipLevelName"));
                result.put("flexMessageBuilt", true);
                result.put("flexMessagePreview", memberInfo.toString().substring(0, Math.min(500, memberInfo.toString().length())) + "...");
            } catch (Exception e) {
                result.put("flexMessageBuilt", false);
                result.put("flexMessageError", e.getMessage());
            }

            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getClass().getName() + ": " + e.getMessage());
        }

        return result;
    }

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

        String text = message.path("text").asText().trim();
        String textLower = text.toLowerCase();

        // ========================================
        // 優先檢查對話狀態 - 備註輸入狀態優先處理
        // ========================================
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 如果正在輸入備註，直接處理為備註（不檢查關鍵字）
        if (context.getState() == ConversationState.INPUTTING_NOTE) {
            handleNoteInput(tenantId, userId, replyToken, text);
            return;
        }

        // ========================================
        // 關鍵字檢查（僅在非備註輸入狀態時）
        // ========================================

        // 檢查是否為預約關鍵字
        if (matchesKeyword(textLower, BOOKING_KEYWORDS)) {
            startBookingFlow(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為取消關鍵字
        if (matchesKeyword(textLower, CANCEL_KEYWORDS)) {
            cancelCurrentFlow(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為幫助關鍵字
        if (matchesKeyword(textLower, HELP_KEYWORDS)) {
            replyHelpMessage(tenantId, replyToken);
            return;
        }

        // 檢查是否為票券關鍵字
        if (matchesKeyword(textLower, COUPON_KEYWORDS)) {
            handleViewCoupons(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為商品關鍵字
        if (matchesKeyword(textLower, PRODUCT_KEYWORDS)) {
            handleStartShopping(tenantId, userId, replyToken);
            return;
        }

        // 檢查是否為會員關鍵字
        if (matchesKeyword(textLower, MEMBER_KEYWORDS)) {
            handleViewMemberInfo(tenantId, userId, replyToken);
            return;
        }

        // 根據當前對話狀態處理其他情況
        handleContextualMessage(tenantId, userId, replyToken, text, context);
    }

    /**
     * 處理 Postback 事件
     */
    private void handlePostbackEvent(String tenantId, JsonNode event) {
        String replyToken = event.path("replyToken").asText();
        String userId = event.path("source").path("userId").asText();
        String data = event.path("postback").path("data").asText();

        log.info("Postback 事件，租戶：{}，用戶：{}，資料：{}", tenantId, userId, data);

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
            case "select_category" -> handleSelectCategory(tenantId, userId, replyToken, params);
            case "select_service" -> handleSelectService(tenantId, userId, replyToken, params);
            case "select_staff" -> handleSelectStaff(tenantId, userId, replyToken, params);
            case "select_date" -> handleSelectDate(tenantId, userId, replyToken, params);
            case "select_time" -> handleSelectTime(tenantId, userId, replyToken, params);
            case "skip_note" -> handleSkipNote(tenantId, userId, replyToken);
            case "confirm_booking" -> handleConfirmBooking(tenantId, userId, replyToken);
            case "cancel_booking" -> cancelCurrentFlow(tenantId, userId, replyToken);
            case "go_back" -> handleGoBack(tenantId, userId, replyToken);
            case "resume_booking" -> handleResumeBooking(tenantId, userId, replyToken);
            case "view_bookings" -> handleViewBookings(tenantId, userId, replyToken);
            case "main_menu" -> replyMainMenu(tenantId, userId, replyToken);
            // 取消當前流程
            case "cancel_flow" -> handleCancelFlowRequest(tenantId, userId, replyToken);
            case "confirm_cancel_flow" -> cancelCurrentFlow(tenantId, userId, replyToken);
            // 取消預約功能
            case "cancel_booking_request" -> handleCancelBookingRequest(tenantId, userId, replyToken, params);
            case "confirm_cancel_booking" -> handleConfirmCancelBooking(tenantId, userId, replyToken, params);
            // 票券功能
            case "view_coupons" -> handleViewCoupons(tenantId, userId, replyToken);
            case "receive_coupon" -> handleReceiveCoupon(tenantId, userId, replyToken, params);
            case "view_my_coupons" -> handleViewMyCoupons(tenantId, userId, replyToken);
            // 會員資訊
            case "view_member_info" -> handleViewMemberInfo(tenantId, userId, replyToken);
            // 聯絡店家
            case "contact_shop" -> handleContactShop(tenantId, userId, replyToken);
            // 商品功能
            case "start_shopping" -> handleStartShopping(tenantId, userId, replyToken);
            case "select_product" -> handleSelectProduct(tenantId, userId, replyToken, params);
            case "select_quantity" -> handleSelectQuantity(tenantId, userId, replyToken, params);
            case "confirm_purchase" -> handleConfirmPurchase(tenantId, userId, replyToken);
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
     * 有多個分類時顯示分類＋服務合併選單，否則直接選服務
     */
    private void startBookingFlow(String tenantId, String userId, String replyToken) {
        // 檢查是否啟用預約功能
        Optional<TenantLineConfig> configOpt = lineConfigRepository.findByTenantId(tenantId);
        if (configOpt.isEmpty() || !configOpt.get().getBookingEnabled()) {
            messageService.replyText(tenantId, replyToken, "抱歉，目前暫不開放線上預約。請直接聯繫店家。");
            return;
        }

        // 查詢啟用中的服務分類
        List<ServiceCategory> categories = serviceCategoryRepository.findByTenantId(tenantId, true);

        // 檢查有多少分類實際包含可預約的服務
        List<String> categoriesWithServices = serviceItemRepository.findDistinctBookableCategoryIds(tenantId);

        // 統一使用 SELECTING_SERVICE 狀態，分類與服務合併為一步
        conversationService.startBooking(tenantId, userId);

        if (categories.size() >= 2 && categoriesWithServices.size() >= 2) {
            // 多個分類 — 顯示分類＋服務合併選單，服務按分類分組
            try {
                JsonNode serviceMenu = flexMessageBuilder.buildCategoryServiceMenu(tenantId);
                messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
            } catch (Exception e) {
                log.error("建構分類服務選單失敗，改用一般服務選單，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
                JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
                messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
            }
        } else {
            // 沒有分類 — 直接顯示全部服務
            JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
            messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
        }
    }

    /**
     * 處理選擇分類
     * 設定分類後顯示該分類下的服務
     */
    private void handleSelectCategory(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String categoryId = params.get("categoryId");
        String categoryName = params.get("categoryName");

        conversationService.setSelectedCategory(tenantId, userId, categoryId, categoryName);

        // 顯示該分類下的服務
        JsonNode serviceMenu = flexMessageBuilder.buildServiceMenuByCategory(tenantId, categoryId);
        messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
    }

    /**
     * 處理選擇服務
     * 新流程：選服務 → 選日期 → 選員工 → 選時間
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

        // 新流程：選完服務後顯示日期選單（傳入服務時長過濾無可用時段的日期）
        Integer serviceDuration = duration != null ? Integer.parseInt(duration) : null;
        JsonNode dateMenu = flexMessageBuilder.buildDateMenu(tenantId, serviceDuration);
        messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
    }

    /**
     * 處理選擇員工
     * 新流程：選員工後顯示時段選單
     */
    private void handleSelectStaff(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String staffId = params.get("staffId");
        String staffName = params.get("staffName");

        // 空字串視為 null（不指定員工）
        if (staffId != null && staffId.isEmpty()) {
            staffId = null;
        }

        conversationService.setSelectedStaff(tenantId, userId, staffId, staffName);

        // 取得對話上下文
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 新流程：選完員工後顯示時段選單
        JsonNode timeMenu = flexMessageBuilder.buildTimeMenu(
                tenantId,
                staffId,
                context.getSelectedDate(),
                context.getSelectedServiceDuration()
        );
        messageService.replyFlex(tenantId, replyToken, "請選擇時段", timeMenu);
    }

    /**
     * 處理選擇日期
     * 新流程：選日期後顯示員工選單（只顯示當天有上班的員工）
     */
    private void handleSelectDate(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String dateStr = params.get("date");
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

        conversationService.setSelectedDate(tenantId, userId, date);

        // 取得對話上下文
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 新流程：選完日期後顯示員工選單（根據日期篩選可用員工，傳入服務時長檢查可預約時段）
        JsonNode staffMenu = flexMessageBuilder.buildStaffMenuByDate(
                tenantId, context.getSelectedServiceId(), date, context.getSelectedServiceDuration());
        messageService.replyFlex(tenantId, replyToken, "請選擇服務人員", staffMenu);
    }

    /**
     * 處理選擇時間
     */
    private void handleSelectTime(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String timeStr = params.get("time");
        LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_TIME);

        conversationService.setSelectedTime(tenantId, userId, time);

        // 回覆備註輸入提示
        JsonNode notePrompt = flexMessageBuilder.buildNoteInputPrompt();
        messageService.replyFlex(tenantId, replyToken, "請輸入備註或跳過", notePrompt);
    }

    /**
     * 處理跳過備註
     */
    private void handleSkipNote(String tenantId, String userId, String replyToken) {
        // 設定空備註並進入確認狀態
        conversationService.setCustomerNote(tenantId, userId, null);

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
            // 取得或建立顧客 ID
            // ========================================
            String customerId = lineUserService.getOrCreateCustomerId(tenantId, userId);
            if (customerId == null) {
                messageService.replyText(tenantId, replyToken, "無法建立顧客資料，請稍後再試或聯繫店家。");
                return;
            }

            // ========================================
            // 建立預約請求
            // ========================================
            CreateBookingRequest request = CreateBookingRequest.builder()
                    .bookingDate(context.getSelectedDate())
                    .startTime(context.getSelectedTime())
                    .serviceItemId(context.getSelectedServiceId())
                    .staffId(context.getSelectedStaffId())
                    .customerId(customerId)
                    .customerNote(context.getCustomerNote())
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
        log.info("返回上一步，租戶：{}，用戶：{}，返回到狀態：{}，日期：{}，員工：{}",
                tenantId, userId, context.getState(), context.getSelectedDate(), context.getSelectedStaffId());
        displayCurrentState(tenantId, userId, replyToken, context);
    }

    /**
     * 處理繼續預約（從取消確認對話框返回當前步驟）
     */
    private void handleResumeBooking(String tenantId, String userId, String replyToken) {
        ConversationContext context = conversationService.getContext(tenantId, userId);
        displayCurrentState(tenantId, userId, replyToken, context);
    }

    /**
     * 根據當前狀態顯示對應的 UI
     */
    private void displayCurrentState(String tenantId, String userId, String replyToken, ConversationContext context) {
        try {
            switch (context.getState()) {
                case SELECTING_CATEGORY -> {
                    JsonNode categoryMenu = flexMessageBuilder.buildCategoryMenu(tenantId);
                    messageService.replyFlex(tenantId, replyToken, "請選擇服務分類", categoryMenu);
                }
                case SELECTING_SERVICE -> {
                    // 檢查是否有多個分類，有則顯示分類＋服務合併選單
                    List<ServiceCategory> cats = serviceCategoryRepository.findByTenantId(tenantId, true);
                    List<String> catIds = serviceItemRepository.findDistinctBookableCategoryIds(tenantId);
                    if (cats.size() >= 2 && catIds.size() >= 2) {
                        try {
                            JsonNode serviceMenu = flexMessageBuilder.buildCategoryServiceMenu(tenantId);
                            messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
                        } catch (Exception e) {
                            log.warn("分類服務選單建構失敗，改用一般選單：{}", e.getMessage());
                            JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
                            messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
                        }
                    } else {
                        JsonNode serviceMenu = flexMessageBuilder.buildServiceMenu(tenantId);
                        messageService.replyFlex(tenantId, replyToken, "請選擇服務", serviceMenu);
                    }
                }
                case SELECTING_STAFF -> {
                    // 返回選員工時需要根據已選日期篩選，傳入服務時長檢查可預約時段
                    LocalDate selectedDate = context.getSelectedDate();
                    if (selectedDate == null) {
                        log.warn("SELECTING_STAFF 狀態但 selectedDate 為 null，返回日期選單");
                        JsonNode dateMenu = flexMessageBuilder.buildDateMenu(tenantId, context.getSelectedServiceDuration());
                        messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
                    } else {
                        JsonNode staffMenu = flexMessageBuilder.buildStaffMenuByDate(
                                tenantId, context.getSelectedServiceId(), selectedDate,
                                context.getSelectedServiceDuration());
                        messageService.replyFlex(tenantId, replyToken, "請選擇服務人員", staffMenu);
                    }
                }
                case SELECTING_DATE -> {
                    JsonNode dateMenu = flexMessageBuilder.buildDateMenu(tenantId, context.getSelectedServiceDuration());
                    messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
                }
                case SELECTING_TIME -> {
                    LocalDate selectedDate = context.getSelectedDate();
                    if (selectedDate == null) {
                        log.warn("SELECTING_TIME 狀態但 selectedDate 為 null，返回日期選單");
                        JsonNode dateMenu = flexMessageBuilder.buildDateMenu(tenantId, context.getSelectedServiceDuration());
                        messageService.replyFlex(tenantId, replyToken, "請選擇日期", dateMenu);
                    } else {
                        JsonNode timeMenu = flexMessageBuilder.buildTimeMenu(
                                tenantId, context.getSelectedStaffId(),
                                selectedDate, context.getSelectedServiceDuration()
                        );
                        messageService.replyFlex(tenantId, replyToken, "請選擇時段", timeMenu);
                    }
                }
                case INPUTTING_NOTE -> {
                    JsonNode notePrompt = flexMessageBuilder.buildNoteInputPrompt();
                    messageService.replyFlex(tenantId, replyToken, "請輸入備註或跳過", notePrompt);
                }
                case CONFIRMING_BOOKING -> {
                    JsonNode confirmMessage = flexMessageBuilder.buildBookingConfirmation(context);
                    messageService.replyFlex(tenantId, replyToken, "請確認預約資訊", confirmMessage);
                }
                default -> replyMainMenu(tenantId, userId, replyToken);
            }
        } catch (Exception e) {
            log.error("顯示當前狀態失敗，租戶：{}，用戶：{}，狀態：{}，錯誤：{}",
                    tenantId, userId, context.getState(), e.getMessage(), e);
            // 回覆提示訊息，避免用戶看不到任何回應
            try {
                messageService.replyText(tenantId, replyToken, "操作處理中發生錯誤，請重新開始預約。");
            } catch (Exception ex) {
                log.error("回覆錯誤訊息也失敗：{}", ex.getMessage());
            }
        }
    }

    /**
     * 處理取消流程請求（顯示確認對話框）
     */
    private void handleCancelFlowRequest(String tenantId, String userId, String replyToken) {
        JsonNode confirmMessage = flexMessageBuilder.buildCancelFlowConfirmation();
        messageService.replyFlex(tenantId, replyToken, "確認取消", confirmMessage);
    }

    /**
     * 取消當前流程
     */
    private void cancelCurrentFlow(String tenantId, String userId, String replyToken) {
        conversationService.reset(tenantId, userId);

        // 顯示主選單而非純文字，讓使用者知道可以做什麼
        JsonNode mainMenu = flexMessageBuilder.buildMainMenu(tenantId);
        messageService.replyFlex(tenantId, replyToken, "已取消。請選擇您需要的服務", mainMenu);
    }

    /**
     * 處理查看預約
     */
    private void handleViewBookings(String tenantId, String userId, String replyToken) {
        log.info("處理查看預約，租戶：{}，用戶：{}", tenantId, userId);
        try {
            // 取得顧客 ID
            String customerId = lineUserService.getCustomerId(tenantId, userId);
            log.info("顧客 ID：{}", customerId);

            if (customerId == null) {
                messageService.replyText(tenantId, replyToken, "您目前沒有預約記錄。");
                return;
            }

            // 查詢有效預約（僅 PENDING / CONFIRMED，依時間 ASC 排序）
            List<Booking> bookings = bookingRepository.findActiveByCustomerId(
                    tenantId, customerId
            );

            // 建構預約列表訊息（帶取消按鈕）
            JsonNode bookingList = flexMessageBuilder.buildBookingListWithCancel(bookings);
            messageService.replyFlex(tenantId, replyToken, "我的預約", bookingList);

        } catch (Exception e) {
            log.error("查詢預約失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "查詢預約失敗，請稍後再試。");
        }
    }

    // ========================================
    // 取消預約處理
    // ========================================

    /**
     * 處理取消預約請求
     */
    private void handleCancelBookingRequest(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String bookingId = params.get("bookingId");
        log.info("處理取消預約請求，租戶：{}，預約 ID：{}", tenantId, bookingId);

        try {
            // 查詢預約
            Optional<Booking> bookingOpt = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(bookingId, tenantId);
            if (bookingOpt.isEmpty()) {
                messageService.replyText(tenantId, replyToken, "找不到此預約記錄。");
                return;
            }

            Booking booking = bookingOpt.get();

            // 檢查是否可取消
            if (!booking.isCancellable()) {
                messageService.replyText(tenantId, replyToken, "此預約狀態無法取消。");
                return;
            }

            // 儲存待取消的預約 ID
            ConversationContext context = conversationService.getContext(tenantId, userId);
            context.setCancelBookingId(bookingId);
            context.transitionTo(ConversationState.CONFIRMING_CANCEL_BOOKING);
            conversationService.saveContext(context);

            // 顯示確認訊息
            JsonNode confirmMessage = flexMessageBuilder.buildCancelConfirmation(booking);
            messageService.replyFlex(tenantId, replyToken, "確認取消預約", confirmMessage);

        } catch (Exception e) {
            log.error("處理取消預約請求失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "處理失敗，請稍後再試。");
        }
    }

    /**
     * 處理確認取消預約
     */
    private void handleConfirmCancelBooking(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String bookingId = params.get("bookingId");
        log.info("確認取消預約，租戶：{}，預約 ID：{}", tenantId, bookingId);

        try {
            // 設定 TenantContext
            TenantContext.setTenantId(tenantId);

            // 取消預約
            bookingService.cancel(bookingId, "顧客透過 LINE 取消");

            // 回覆成功訊息
            messageService.replyText(tenantId, replyToken, "預約已取消成功。如需重新預約，請點選「開始預約」。");

        } catch (Exception e) {
            log.error("取消預約失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "取消預約失敗：" + e.getMessage());
        } finally {
            TenantContext.clear();
            conversationService.reset(tenantId, userId);
        }
    }

    // ========================================
    // 票券處理
    // ========================================

    /**
     * 處理查看可領取票券
     */
    private void handleViewCoupons(String tenantId, String userId, String replyToken) {
        log.info("處理查看可領取票券，租戶：{}，用戶：{}", tenantId, userId);

        try {
            // 查詢可領取的票券（已發布且未過期）
            List<Coupon> coupons = couponRepository.findByTenantIdAndStatusAndDeletedAtIsNull(
                    tenantId, CouponStatus.PUBLISHED
            );

            if (coupons.isEmpty()) {
                messageService.replyText(tenantId, replyToken, "目前沒有可領取的票券。");
                return;
            }

            // 建構票券列表訊息
            JsonNode couponList = flexMessageBuilder.buildAvailableCouponList(coupons);
            messageService.replyFlex(tenantId, replyToken, "可領取票券", couponList);

        } catch (Exception e) {
            log.error("查詢票券失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "查詢票券失敗，請稍後再試。");
        }
    }

    /**
     * 處理領取票券
     */
    private void handleReceiveCoupon(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String couponId = params.get("couponId");
        log.info("處理領取票券，租戶：{}，票券 ID：{}", tenantId, couponId);

        try {
            TenantContext.setTenantId(tenantId);

            // 取得顧客 ID
            String customerId = lineUserService.getOrCreateCustomerId(tenantId, userId);
            if (customerId == null) {
                messageService.replyText(tenantId, replyToken, "無法建立顧客資料，請稍後再試。");
                return;
            }

            // 查詢票券
            Optional<Coupon> couponOpt = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(couponId, tenantId);
            if (couponOpt.isEmpty()) {
                messageService.replyText(tenantId, replyToken, "找不到此票券。");
                return;
            }

            Coupon coupon = couponOpt.get();

            // 檢查票券是否可發放
            if (!coupon.canIssue()) {
                messageService.replyText(tenantId, replyToken, "此票券已停止發放或已發完。");
                return;
            }

            // 檢查每人限領數量
            Integer limitPerCustomer = coupon.getLimitPerCustomer();
            long alreadyClaimed = couponInstanceRepository.countByCustomerAndCoupon(tenantId, couponId, customerId);

            if (limitPerCustomer != null && limitPerCustomer > 0) {
                if (alreadyClaimed >= limitPerCustomer) {
                    messageService.replyText(tenantId, replyToken,
                            String.format("此票券每人限領 %d 張，您已領取 %d 張。", limitPerCustomer, alreadyClaimed));
                    return;
                }
            } else {
                // 沒有設定限領數量時，預設每人只能領 1 張
                if (alreadyClaimed > 0) {
                    messageService.replyText(tenantId, replyToken, "您已經領取過此票券了。");
                    return;
                }
            }

            // 發放票券
            CouponInstance instance = couponService.issueToCustomer(couponId, customerId);

            // 回覆成功訊息（使用 Flex Message）
            JsonNode successMessage = flexMessageBuilder.buildCouponReceiveSuccess(
                    coupon.getName(),
                    instance.getCode(),
                    instance.getExpiresAt()
            );
            messageService.replyFlex(tenantId, replyToken, "票券領取成功", successMessage);

        } catch (Exception e) {
            log.error("領取票券失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "領取票券失敗：" + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 處理查看已領取票券
     */
    private void handleViewMyCoupons(String tenantId, String userId, String replyToken) {
        log.info("處理查看已領取票券，租戶：{}，用戶：{}", tenantId, userId);

        try {
            // 取得顧客 ID
            String customerId = lineUserService.getCustomerId(tenantId, userId);
            if (customerId == null) {
                messageService.replyText(tenantId, replyToken, "您目前沒有票券。");
                return;
            }

            // 查詢已領取的票券
            List<CouponInstance> instances = couponInstanceRepository.findByCustomerIdAndTenantId(customerId, tenantId);

            if (instances.isEmpty()) {
                messageService.replyText(tenantId, replyToken, "您目前沒有票券。\n請點選「領取票券」領取新票券。");
                return;
            }

            // 取得票券名稱對應
            Map<String, String> couponNames = new HashMap<>();
            for (CouponInstance instance : instances) {
                couponRepository.findById(instance.getCouponId())
                        .ifPresent(coupon -> couponNames.put(coupon.getId(), coupon.getName()));
            }

            // 建構已領取票券列表
            JsonNode myCouponList = flexMessageBuilder.buildMyCouponList(instances, couponNames);
            messageService.replyFlex(tenantId, replyToken, "我的票券", myCouponList);

        } catch (Exception e) {
            log.error("查詢已領取票券失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "查詢失敗，請稍後再試。");
        }
    }

    // ========================================
    // 會員資訊處理
    // ========================================

    /**
     * 處理查看會員資訊
     */
    private void handleViewMemberInfo(String tenantId, String userId, String replyToken) {
        log.info("=== 開始處理查看會員資訊 ===");
        log.info("租戶：{}，用戶：{}，replyToken：{}", tenantId, userId, replyToken);

        try {
            // 取得顧客 ID
            String customerId = lineUserService.getCustomerId(tenantId, userId);
            log.info("查詢到顧客 ID：{}", customerId);

            if (customerId == null) {
                log.info("用戶尚未成為會員，回覆提示訊息");
                messageService.replyText(tenantId, replyToken, "您尚未成為會員。\n完成首次預約即可成為會員。");
                log.info("已回覆「尚未成為會員」訊息");
                return;
            }

            // 查詢顧客資料
            log.info("開始查詢顧客資料...");
            Optional<Customer> customerOpt = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId);
            if (customerOpt.isEmpty()) {
                log.warn("找不到顧客資料，customerId：{}", customerId);
                messageService.replyText(tenantId, replyToken, "找不到會員資料。");
                return;
            }

            Customer customer = customerOpt.get();
            log.info("找到顧客 - ID：{}，名稱：{}，點數：{}",
                    customer.getId(),
                    customer.getName(),
                    customer.getPointBalance());

            // 查詢預約統計
            log.info("開始查詢預約統計...");
            long bookingCount = bookingRepository.countByCustomerIdAndTenantId(customerId, tenantId);
            log.info("預約次數：{}", bookingCount);

            // 查詢會員等級名稱
            String membershipLevelName = null;
            if (customer.getMembershipLevelId() != null) {
                log.info("查詢會員等級，levelId：{}", customer.getMembershipLevelId());
                membershipLevelName = membershipLevelRepository
                        .findById(customer.getMembershipLevelId())
                        .map(level -> level.getName())
                        .orElse(null);
                log.info("會員等級名稱：{}", membershipLevelName);
            }

            // 使用簡化版 Flex Message（單一 Bubble，更穩定）
            log.info("準備建構會員資訊 Flex Message...");
            JsonNode memberInfoFlex = flexMessageBuilder.buildSimpleMemberInfo(customer, bookingCount, membershipLevelName);
            log.info("Flex Message 建構完成，準備發送...");

            messageService.replyFlex(tenantId, replyToken, "會員資訊", memberInfoFlex);
            log.info("=== 會員資訊 Flex Message 發送成功 ===");

        } catch (Exception e) {
            log.error("=== 查詢會員資訊失敗 ===");
            log.error("租戶：{}，用戶：{}，錯誤類型：{}，錯誤訊息：{}",
                    tenantId, userId, e.getClass().getName(), e.getMessage());
            log.error("完整堆疊：", e);
            try {
                messageService.replyText(tenantId, replyToken, "查詢失敗：" + e.getMessage());
            } catch (Exception replyError) {
                log.error("錯誤訊息回覆也失敗：{}", replyError.getMessage());
            }
        }
    }

    /**
     * 處理聯絡店家
     */
    private void handleContactShop(String tenantId, String userId, String replyToken) {
        log.info("處理聯絡店家，租戶：{}，用戶：{}", tenantId, userId);

        // 建構聯絡店家訊息
        JsonNode contactMessage = flexMessageBuilder.buildContactShopMessage(tenantId);
        messageService.replyFlex(tenantId, replyToken, "聯絡店家", contactMessage);
    }

    // ========================================
    // 商品購買處理
    // ========================================

    /**
     * 處理開始購物
     */
    private void handleStartShopping(String tenantId, String userId, String replyToken) {
        log.info("處理開始購物，租戶：{}，用戶：{}", tenantId, userId);

        try {
            // 查詢上架中的商品
            List<Product> products = productRepository.findByTenantIdAndStatusAndDeletedAtIsNull(
                    tenantId, ProductStatus.ON_SALE
            );

            if (products.isEmpty()) {
                messageService.replyText(tenantId, replyToken, "目前沒有上架商品。");
                return;
            }

            // 轉換狀態
            ConversationContext context = conversationService.getContext(tenantId, userId);
            context.transitionTo(ConversationState.BROWSING_PRODUCTS);
            conversationService.saveContext(context);

            // 建構商品列表訊息
            JsonNode productList = flexMessageBuilder.buildProductMenu(products);
            messageService.replyFlex(tenantId, replyToken, "商品列表", productList);

        } catch (Exception e) {
            log.error("查詢商品失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "查詢商品失敗，請稍後再試。");
        }
    }

    /**
     * 處理選擇商品
     */
    private void handleSelectProduct(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String productId = params.get("productId");
        String productName = params.get("productName");
        String priceStr = params.get("price");

        log.info("處理選擇商品，租戶：{}，商品 ID：{}", tenantId, productId);

        try {
            // 檢查商品庫存
            Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
                    .orElse(null);

            if (product == null) {
                messageService.replyText(tenantId, replyToken, "找不到該商品，請重新選擇。");
                return;
            }

            if (product.getStockQuantity() != null && product.getStockQuantity() <= 0) {
                messageService.replyText(tenantId, replyToken, "抱歉，「" + product.getName() + "」已售完。");
                return;
            }

            Integer price = priceStr != null ? Integer.parseInt(priceStr) : (product.getPrice() != null ? product.getPrice().intValue() : 0);

            // 儲存選擇的商品
            ConversationContext context = conversationService.getContext(tenantId, userId);
            context.setProduct(productId, productName != null ? productName : product.getName(), price);
            context.transitionTo(ConversationState.SELECTING_QUANTITY);
            conversationService.saveContext(context);

            // 顯示數量選擇（限制最大數量為庫存數）
            int maxQuantity = product.getStockQuantity() != null ? Math.min(product.getStockQuantity(), 10) : 10;
            JsonNode quantityMenu = flexMessageBuilder.buildQuantityMenu(
                    productName != null ? productName : product.getName(),
                    price,
                    maxQuantity
            );
            messageService.replyFlex(tenantId, replyToken, "選擇數量", quantityMenu);

        } catch (Exception e) {
            log.error("選擇商品失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "處理失敗，請稍後再試。");
        }
    }

    /**
     * 處理選擇數量
     */
    private void handleSelectQuantity(String tenantId, String userId, String replyToken, Map<String, String> params) {
        String quantityStr = params.get("quantity");
        log.info("處理選擇數量，租戶：{}，數量：{}", tenantId, quantityStr);

        try {
            int quantity = Integer.parseInt(quantityStr);

            ConversationContext context = conversationService.getContext(tenantId, userId);
            context.setQuantity(quantity);
            context.transitionTo(ConversationState.CONFIRMING_PURCHASE);
            conversationService.saveContext(context);

            // 顯示確認購買
            JsonNode confirmMessage = flexMessageBuilder.buildPurchaseConfirmation(context);
            messageService.replyFlex(tenantId, replyToken, "確認購買", confirmMessage);

        } catch (Exception e) {
            log.error("選擇數量失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "處理失敗，請稍後再試。");
        }
    }

    /**
     * 處理確認購買
     */
    private void handleConfirmPurchase(String tenantId, String userId, String replyToken) {
        log.info("處理確認購買，租戶：{}，用戶：{}", tenantId, userId);

        try {
            ConversationContext context = conversationService.getContext(tenantId, userId);

            if (!context.canConfirmPurchase()) {
                messageService.replyText(tenantId, replyToken, "購買資訊不完整，請重新選擇。");
                conversationService.reset(tenantId, userId);
                return;
            }

            String productId = context.getSelectedProductId();
            String productName = context.getSelectedProductName();
            Integer quantity = context.getSelectedQuantity();
            Integer unitPrice = context.getSelectedProductPrice();

            // 防禦性空值檢查
            if (productId == null || quantity == null || unitPrice == null) {
                messageService.replyText(tenantId, replyToken, "購買資訊不完整，請重新選擇。");
                conversationService.reset(tenantId, userId);
                return;
            }

            // 建立訂單並扣減庫存
            var orderResponse = productOrderService.createFromLine(tenantId, userId, productId, quantity);

            int totalPrice = unitPrice * quantity;

            // 回覆成功訊息
            String successMessage = String.format(
                    "訂單建立成功！\n\n" +
                    "訂單編號：%s\n" +
                    "商品：%s\n" +
                    "數量：%d\n" +
                    "金額：NT$ %d\n\n" +
                    "請至店家出示訂單編號完成付款取貨。",
                    orderResponse.getOrderNo(), productName, quantity, totalPrice
            );
            messageService.replyText(tenantId, replyToken, successMessage);

        } catch (com.booking.platform.common.exception.BusinessException e) {
            log.warn("確認購買失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
            messageService.replyText(tenantId, replyToken, e.getMessage());
        } catch (Exception e) {
            log.error("確認購買失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            messageService.replyText(tenantId, replyToken, "處理失敗，請稍後再試。");
        } finally {
            conversationService.reset(tenantId, userId);
        }
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
        // 如果不在對話中，嘗試使用 AI 回覆
        if (context.getState() == ConversationState.IDLE) {
            // 嘗試 AI 智慧回覆（完整包裹在 try-catch 中確保不影響主流程）
            try {
                if (aiAssistantService != null && aiAssistantService.shouldUseAi(text)) {
                    // 取得顧客 ID
                    String customerId = getCustomerIdByLineUser(tenantId, userId);

                    // 呼叫 AI（包含選單顯示標記）
                    var aiResponse = aiAssistantService.chatWithMenuFlag(tenantId, customerId, text);

                    if (aiResponse != null && !aiResponse.text().isEmpty()) {
                        // AI 回覆成功，根據 AI 判斷決定是否顯示選單
                        replyAiResponse(tenantId, replyToken, aiResponse.text(), aiResponse.showMenu());
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("AI 功能異常，改用預設回覆：{}", e.getMessage());
            }
            // AI 未啟用或失敗，顯示主選單
            replyDefaultMessage(tenantId, replyToken);
        } else if (context.getState() == ConversationState.INPUTTING_NOTE) {
            // 在備註輸入狀態，處理文字輸入作為備註
            handleNoteInput(tenantId, userId, replyToken, text);
        } else {
            // 在對話中但收到非 Postback 訊息，提示用戶並顯示取消按鈕
            JsonNode cancelMessage = flexMessageBuilder.buildCancelPrompt();
            messageService.replyFlex(tenantId, replyToken, "請點選上方選項繼續操作", cancelMessage);
        }
    }

    /**
     * 根據 LINE User ID 取得顧客 ID
     */
    private String getCustomerIdByLineUser(String tenantId, String userId) {
        return lineUserService.findByLineUserId(tenantId, userId)
                .map(LineUser::getCustomerId)
                .orElse(null);
    }

    /**
     * 回覆 AI 回應，根據 AI 判斷決定是否附帶主選單
     *
     * @param tenantId   租戶 ID
     * @param replyToken 回覆 Token
     * @param aiResponse AI 回覆文字
     * @param showMenu   是否顯示主選單（由 AI 根據對話內容判斷）
     */
    private void replyAiResponse(String tenantId, String replyToken, String aiResponse, boolean showMenu) {
        if (showMenu) {
            // AI 判斷用戶有服務意圖，顯示主選單
            JsonNode mainMenu = flexMessageBuilder.buildMainMenu(tenantId);
            messageService.replyTextAndFlex(tenantId, replyToken, aiResponse, "需要什麼服務呢？", mainMenu);
        } else {
            // AI 判斷用戶只是詢問，不顯示選單
            messageService.replyText(tenantId, replyToken, aiResponse);
        }
    }

    /**
     * 處理備註文字輸入
     */
    private void handleNoteInput(String tenantId, String userId, String replyToken, String text) {
        // 限制備註長度
        String note = text;
        if (note.length() > 500) {
            note = note.substring(0, 500);
        }

        // 設定備註並進入確認狀態
        conversationService.setCustomerNote(tenantId, userId, note);

        // 取得對話上下文
        ConversationContext context = conversationService.getContext(tenantId, userId);

        // 回覆確認訊息
        JsonNode confirmMessage = flexMessageBuilder.buildBookingConfirmation(context);
        messageService.replyFlex(tenantId, replyToken, "請確認預約資訊", confirmMessage);
    }

    /**
     * 回覆歡迎訊息
     */
    private void replyWelcomeMessage(String tenantId, String userId, String replyToken) {
        // 取得用戶名稱
        JsonNode profile = messageService.getProfile(tenantId, userId);
        String displayName = profile != null ? profile.path("displayName").asText(null) : null;

        // 使用新的歡迎訊息 Flex Message
        JsonNode welcomeMessage = flexMessageBuilder.buildWelcomeMessage(tenantId, displayName);
        messageService.replyFlex(tenantId, replyToken, "歡迎加入！點擊開始使用", welcomeMessage);
    }

    /**
     * 回覆預設訊息（顯示主選單）
     */
    private void replyDefaultMessage(String tenantId, String replyToken) {
        JsonNode mainMenu = flexMessageBuilder.buildMainMenu(tenantId);
        messageService.replyFlex(tenantId, replyToken, "請選擇您需要的服務", mainMenu);
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
        // 使用新的幫助訊息 Flex Message
        JsonNode helpMessage = flexMessageBuilder.buildHelpMessage(tenantId);
        messageService.replyFlex(tenantId, replyToken, "使用說明", helpMessage);
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
