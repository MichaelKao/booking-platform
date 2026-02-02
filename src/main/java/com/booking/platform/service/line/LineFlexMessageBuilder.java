package com.booking.platform.service.line;

import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.entity.marketing.CouponInstance;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.product.Product;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.enums.CouponInstanceStatus;
import com.booking.platform.enums.ServiceStatus;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.StaffScheduleRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.entity.staff.StaffSchedule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LINE Flex Message 建構器
 *
 * <p>建構各種 Flex Message 用於 LINE Bot 互動
 *
 * <p>支援的訊息類型：
 * <ul>
 *   <li>主選單 - 歡迎訊息 + 功能按鈕</li>
 *   <li>服務選單 - Carousel 服務項目</li>
 *   <li>員工選單 - 員工列表 + 不指定選項</li>
 *   <li>日期選單 - 未來 7 天按鈕</li>
 *   <li>時段選單 - 可用時段按鈕</li>
 *   <li>預約確認 - 資訊摘要 + 確認/取消按鈕</li>
 *   <li>預約成功 - 預約編號 + 詳細資訊</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 * @see <a href="https://developers.line.biz/en/docs/messaging-api/flex-message-elements/">LINE Flex Message</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineFlexMessageBuilder {

    // ========================================
    // 依賴注入
    // ========================================

    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final BookingRepository bookingRepository;

    // ========================================
    // 顏色常數
    // ========================================

    private static final String PRIMARY_COLOR = "#1DB446";
    private static final String SECONDARY_COLOR = "#666666";
    private static final String LINK_COLOR = "#0066CC";
    private static final String BACKGROUND_COLOR = "#FFFFFF";
    private static final String SEPARATOR_COLOR = "#EEEEEE";

    // ========================================
    // 1. 主選單
    // ========================================

    /**
     * 建構主選單
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildMainMenu(String tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        String shopName = tenantOpt.map(Tenant::getName).orElse("歡迎");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "20px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", shopName);
        headerText.put("color", "#FFFFFF");
        headerText.put("size", "xl");
        headerText.put("weight", "bold");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode welcomeText = objectMapper.createObjectNode();
        welcomeText.put("type", "text");
        welcomeText.put("text", "請選擇您需要的服務");
        welcomeText.put("size", "md");
        welcomeText.put("color", SECONDARY_COLOR);
        bodyContents.add(welcomeText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "20px");

        ArrayNode footerContents = objectMapper.createArrayNode();

        // 預約按鈕
        footerContents.add(createButton("開始預約", "action=start_booking", PRIMARY_COLOR));

        // 查詢預約按鈕
        footerContents.add(createButton("我的預約", "action=view_bookings", LINK_COLOR));

        // 商品按鈕
        footerContents.add(createButton("瀏覽商品", "action=start_shopping", "#FF9800"));

        // 票券按鈕
        footerContents.add(createButton("領取票券", "action=view_coupons", "#FF6B6B"));

        // 會員資訊按鈕
        footerContents.add(createButton("會員資訊", "action=view_member_info", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 2. 服務選單
    // ========================================

    /**
     * 建構服務選單（Carousel）
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildServiceMenu(String tenantId) {
        List<ServiceItem> services = serviceItemRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, ServiceStatus.ACTIVE);

        if (services.isEmpty()) {
            return buildNoServiceMessage();
        }

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (ServiceItem service : services) {
            bubbles.add(buildServiceBubble(service));
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構單一服務 Bubble
     */
    private ObjectNode buildServiceBubble(ServiceItem service) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 服務名稱
        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", service.getName());
        nameText.put("size", "lg");
        nameText.put("weight", "bold");
        nameText.put("wrap", true);
        bodyContents.add(nameText);

        // 時長
        ObjectNode durationText = objectMapper.createObjectNode();
        durationText.put("type", "text");
        durationText.put("text", String.format("時長：%d 分鐘", service.getDuration()));
        durationText.put("size", "sm");
        durationText.put("color", SECONDARY_COLOR);
        bodyContents.add(durationText);

        // 價格
        ObjectNode priceText = objectMapper.createObjectNode();
        priceText.put("type", "text");
        priceText.put("text", String.format("NT$ %d", service.getPrice().intValue()));
        priceText.put("size", "lg");
        priceText.put("weight", "bold");
        priceText.put("color", PRIMARY_COLOR);
        bodyContents.add(priceText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        String postbackData = String.format(
                "action=select_service&serviceId=%s&serviceName=%s&duration=%d&price=%d",
                service.getId(),
                service.getName(),
                service.getDuration(),
                service.getPrice().intValue()
        );

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("選擇此服務", postbackData, PRIMARY_COLOR)
        ));

        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構無服務訊息
     */
    private ObjectNode buildNoServiceMessage() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");

        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", "目前沒有可預約的服務");
        text.put("align", "center");
        text.put("color", SECONDARY_COLOR);

        body.set("contents", objectMapper.createArrayNode().add(text));
        bubble.set("body", body);

        return bubble;
    }

    // ========================================
    // 3. 員工選單
    // ========================================

    /**
     * 建構員工選單
     *
     * @param tenantId  租戶 ID
     * @param serviceId 服務 ID（用於篩選可提供該服務的員工）
     * @return Flex Message 內容
     */
    public JsonNode buildStaffMenu(String tenantId, String serviceId) {
        // TODO: 根據服務篩選員工
        List<Staff> staffList = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "請選擇服務人員");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 不指定選項
        bodyContents.add(createStaffButton("不指定", "由系統安排", null));

        // 員工列表
        for (Staff staff : staffList) {
            bodyContents.add(createStaffButton(
                    staff.getName(),
                    staff.getBio() != null ? staff.getBio() : "",
                    staff.getId()
            ));
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 返回按鈕
        bubble.set("footer", createBackFooter());

        return bubble;
    }

    /**
     * 建構員工按鈕
     */
    private ObjectNode createStaffButton(String name, String title, String staffId) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "horizontal");
        box.put("spacing", "md");
        box.put("paddingAll", "10px");
        box.put("borderWidth", "1px");
        box.put("borderColor", SEPARATOR_COLOR);
        box.put("cornerRadius", "8px");

        ArrayNode contents = objectMapper.createArrayNode();

        // 員工資訊
        ObjectNode infoBox = objectMapper.createObjectNode();
        infoBox.put("type", "box");
        infoBox.put("layout", "vertical");
        infoBox.put("flex", 3);

        ArrayNode infoContents = objectMapper.createArrayNode();

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", name);
        nameText.put("weight", "bold");
        infoContents.add(nameText);

        if (title != null && !title.isEmpty()) {
            ObjectNode titleText = objectMapper.createObjectNode();
            titleText.put("type", "text");
            titleText.put("text", title);
            titleText.put("size", "sm");
            titleText.put("color", SECONDARY_COLOR);
            infoContents.add(titleText);
        }

        infoBox.set("contents", infoContents);
        contents.add(infoBox);

        box.set("contents", contents);

        // 設定 action
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", "選擇");

        String data = staffId != null
                ? String.format("action=select_staff&staffId=%s&staffName=%s", staffId, name)
                : "action=select_staff&staffId=&staffName=不指定";
        action.put("data", data);

        box.set("action", action);

        return box;
    }

    // ========================================
    // 4. 日期選單
    // ========================================

    /**
     * 建構日期選單（支援完整可預約天數）
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容（Carousel 格式）
     */
    public JsonNode buildDateMenu(String tenantId) {
        // 取得店家設定
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        int maxAdvanceDays = tenantOpt.map(Tenant::getMaxAdvanceBookingDays).orElse(30);
        List<Integer> closedDays = parseClosedDays(tenantOpt.map(Tenant::getClosedDays).orElse(null));

        LocalDate today = LocalDate.now();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("M/d (E)", java.util.Locale.TAIWAN);
        DateTimeFormatter dataFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        // 收集所有可用日期
        List<LocalDate> availableDates = new java.util.ArrayList<>();
        int dayOffset = 0;

        while (availableDates.size() < maxAdvanceDays && dayOffset < maxAdvanceDays + 30) {
            LocalDate date = today.plusDays(dayOffset);
            int dayOfWeek = date.getDayOfWeek().getValue() % 7;

            if (!closedDays.contains(dayOfWeek)) {
                availableDates.add(date);
            }
            dayOffset++;
        }

        // 如果日期少於等於 10 個，使用單一 Bubble
        if (availableDates.size() <= 10) {
            return buildSingleDateBubble(availableDates, today, displayFormatter, dataFormatter);
        }

        // 日期多於 10 個，使用 Carousel（每個 Bubble 顯示 7 天）
        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();
        int datesPerBubble = 7;
        int totalBubbles = (int) Math.ceil((double) availableDates.size() / datesPerBubble);

        for (int bubbleIndex = 0; bubbleIndex < totalBubbles && bubbleIndex < 10; bubbleIndex++) {
            int startIdx = bubbleIndex * datesPerBubble;
            int endIdx = Math.min(startIdx + datesPerBubble, availableDates.size());
            List<LocalDate> bubbleDates = availableDates.subList(startIdx, endIdx);

            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // Header
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", PRIMARY_COLOR);
            header.put("paddingAll", "12px");

            ArrayNode headerContents = objectMapper.createArrayNode();

            ObjectNode headerText = objectMapper.createObjectNode();
            headerText.put("type", "text");
            headerText.put("text", bubbleIndex == 0 ? "📅 選擇日期" : "📅 更多日期");
            headerText.put("size", "md");
            headerText.put("weight", "bold");
            headerText.put("color", "#FFFFFF");
            headerText.put("align", "center");
            headerContents.add(headerText);

            // 顯示日期範圍
            if (!bubbleDates.isEmpty()) {
                LocalDate firstDate = bubbleDates.get(0);
                LocalDate lastDate = bubbleDates.get(bubbleDates.size() - 1);
                ObjectNode rangeText = objectMapper.createObjectNode();
                rangeText.put("type", "text");
                rangeText.put("text", firstDate.format(DateTimeFormatter.ofPattern("M/d")) + " - " + lastDate.format(DateTimeFormatter.ofPattern("M/d")));
                rangeText.put("size", "xs");
                rangeText.put("color", "#FFFFFF");
                rangeText.put("align", "center");
                headerContents.add(rangeText);
            }

            header.set("contents", headerContents);
            bubble.set("header", header);

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "xs");
            body.put("paddingAll", "12px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            for (LocalDate date : bubbleDates) {
                String displayDate = date.format(displayFormatter);
                String dataDate = date.format(dataFormatter);
                String label = date.equals(today) ? "今天 " + displayDate : displayDate;
                bodyContents.add(createDateButton(label, dataDate));
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            // 只在第一個 Bubble 顯示返回按鈕
            if (bubbleIndex == 0) {
                bubble.set("footer", createBackFooter());
            }

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構單一日期選擇 Bubble
     */
    private JsonNode buildSingleDateBubble(List<LocalDate> dates, LocalDate today,
                                           DateTimeFormatter displayFormatter, DateTimeFormatter dataFormatter) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "📅 選擇日期");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");
        headerText.put("color", "#FFFFFF");
        headerText.put("align", "center");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        for (LocalDate date : dates) {
            String displayDate = date.format(displayFormatter);
            String dataDate = date.format(dataFormatter);
            String label = date.equals(today) ? "今天 " + displayDate : displayDate;
            bodyContents.add(createDateButton(label, dataDate));
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        bubble.set("footer", createBackFooter());
        return bubble;
    }

    /**
     * 建構日期按鈕
     */
    private ObjectNode createDateButton(String label, String date) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        button.put("style", "secondary");
        button.put("height", "sm");

        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", label);
        action.put("data", "action=select_date&date=" + date);

        button.set("action", action);
        return button;
    }

    // ========================================
    // 5. 時段選單
    // ========================================

    /**
     * 建構時段選單
     *
     * @param tenantId 租戶 ID
     * @param staffId  員工 ID（可為 null）
     * @param date     日期
     * @param duration 服務時長（分鐘）
     * @return Flex Message 內容
     */
    public JsonNode buildTimeMenu(String tenantId, String staffId, LocalDate date, Integer duration) {
        // 根據店家營業時間、員工排班、已有預約產生可用時段
        List<LocalTime> availableSlots = generateAvailableSlots(tenantId, staffId, date, duration);

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "請選擇時段");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");

        ObjectNode dateText = objectMapper.createObjectNode();
        dateText.put("type", "text");
        dateText.put("text", date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
        dateText.put("size", "sm");
        dateText.put("color", SECONDARY_COLOR);

        header.set("contents", objectMapper.createArrayNode().add(headerText).add(dateText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 每行 3 個按鈕
        for (int i = 0; i < availableSlots.size(); i += 3) {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("type", "box");
            row.put("layout", "horizontal");
            row.put("spacing", "sm");

            ArrayNode rowContents = objectMapper.createArrayNode();

            for (int j = i; j < Math.min(i + 3, availableSlots.size()); j++) {
                LocalTime time = availableSlots.get(j);
                rowContents.add(createTimeButton(time));
            }

            // 補齊空位
            while (rowContents.size() < 3) {
                rowContents.add(createFillerBox());
            }

            row.set("contents", rowContents);
            bodyContents.add(row);
        }

        if (availableSlots.isEmpty()) {
            ObjectNode noSlotText = objectMapper.createObjectNode();
            noSlotText.put("type", "text");
            noSlotText.put("text", "此日期沒有可預約的時段");
            noSlotText.put("align", "center");
            noSlotText.put("color", SECONDARY_COLOR);
            bodyContents.add(noSlotText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 返回按鈕
        bubble.set("footer", createBackFooter());

        return bubble;
    }

    /**
     * 產生可用時段
     *
     * @param tenantId 租戶 ID
     * @param staffId  員工 ID（可為 null）
     * @param date     日期
     * @param duration 服務時長（分鐘）
     * @return 可用時段列表
     */
    private List<LocalTime> generateAvailableSlots(String tenantId, String staffId, LocalDate date, Integer duration) {
        List<LocalTime> slots = new ArrayList<>();

        // 取得店家設定
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        if (tenantOpt.isEmpty()) {
            return slots;
        }

        Tenant tenant = tenantOpt.get();
        LocalTime businessStart = tenant.getBusinessStartTime() != null ? tenant.getBusinessStartTime() : LocalTime.of(9, 0);
        LocalTime businessEnd = tenant.getBusinessEndTime() != null ? tenant.getBusinessEndTime() : LocalTime.of(21, 0);
        int interval = tenant.getBookingInterval() != null ? tenant.getBookingInterval() : 30;
        LocalTime breakStart = tenant.getBreakStartTime();
        LocalTime breakEnd = tenant.getBreakEndTime();

        // 如果有指定員工，取得員工排班
        LocalTime staffStart = null;
        LocalTime staffEnd = null;
        LocalTime staffBreakStart = null;
        LocalTime staffBreakEnd = null;

        if (staffId != null && !staffId.isEmpty()) {
            int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 轉換為 0=週日
            Optional<StaffSchedule> scheduleOpt = staffScheduleRepository.findByStaffIdAndDayOfWeek(staffId, tenantId, dayOfWeek);

            if (scheduleOpt.isPresent()) {
                StaffSchedule schedule = scheduleOpt.get();

                // 如果員工當天不上班，返回空
                if (!Boolean.TRUE.equals(schedule.getIsWorkingDay())) {
                    return slots;
                }

                staffStart = schedule.getStartTime();
                staffEnd = schedule.getEndTime();
                staffBreakStart = schedule.getBreakStartTime();
                staffBreakEnd = schedule.getBreakEndTime();
            }
        }

        // 確定可用時間範圍（取交集）
        LocalTime effectiveStart = businessStart;
        LocalTime effectiveEnd = businessEnd;

        if (staffStart != null) {
            effectiveStart = effectiveStart.isBefore(staffStart) ? staffStart : effectiveStart;
        }
        if (staffEnd != null) {
            effectiveEnd = effectiveEnd.isAfter(staffEnd) ? staffEnd : effectiveEnd;
        }

        // 如果是今天，過濾掉已過去的時段（加上 30 分鐘緩衝）
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            LocalTime minBookingTime = now.plusMinutes(30); // 至少要 30 分鐘後才能預約

            // 如果最早可預約時間已經超過營業結束時間，返回空
            if (minBookingTime.isAfter(effectiveEnd) || minBookingTime.equals(effectiveEnd)) {
                return slots;
            }

            // 調整開始時間為下一個可用時段
            if (minBookingTime.isAfter(effectiveStart)) {
                // 計算需要跳過幾個時段
                int minutesPastStart = (minBookingTime.getHour() * 60 + minBookingTime.getMinute())
                        - (effectiveStart.getHour() * 60 + effectiveStart.getMinute());
                int slotsToSkip = (int) Math.ceil((double) minutesPastStart / interval);
                effectiveStart = effectiveStart.plusMinutes((long) slotsToSkip * interval);
            }
        }

        // 服務時長
        int serviceDuration = duration != null ? duration : 60;

        // 產生時段
        LocalTime current = effectiveStart;
        while (!current.plusMinutes(serviceDuration).isAfter(effectiveEnd)) {
            boolean isAvailable = true;

            // 檢查是否在店家休息時間
            if (breakStart != null && breakEnd != null) {
                if (isTimeOverlapping(current, current.plusMinutes(serviceDuration), breakStart, breakEnd)) {
                    isAvailable = false;
                }
            }

            // 檢查是否在員工休息時間
            if (isAvailable && staffBreakStart != null && staffBreakEnd != null) {
                if (isTimeOverlapping(current, current.plusMinutes(serviceDuration), staffBreakStart, staffBreakEnd)) {
                    isAvailable = false;
                }
            }

            // 檢查是否有衝突預約（如有指定員工）
            if (isAvailable && staffId != null && !staffId.isEmpty()) {
                boolean hasConflict = bookingRepository.existsConflictingBooking(
                        tenantId, staffId, date, current, current.plusMinutes(serviceDuration)
                );
                if (hasConflict) {
                    isAvailable = false;
                }
            }

            if (isAvailable) {
                slots.add(current);
            }

            current = current.plusMinutes(interval);
        }

        return slots;
    }

    /**
     * 檢查兩個時間區間是否重疊
     */
    private boolean isTimeOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * 解析公休日設定
     *
     * @param closedDaysJson JSON 格式的公休日（例如：[0,6]）
     * @return 公休日列表
     */
    private List<Integer> parseClosedDays(String closedDaysJson) {
        if (closedDaysJson == null || closedDaysJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(closedDaysJson, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("解析公休日失敗：{}", closedDaysJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 建構時段按鈕
     */
    private ObjectNode createTimeButton(LocalTime time) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        button.put("style", "secondary");
        button.put("height", "sm");
        button.put("flex", 1);

        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", time.format(DateTimeFormatter.ofPattern("HH:mm")));
        action.put("data", "action=select_time&time=" + time.format(DateTimeFormatter.ISO_LOCAL_TIME));

        button.set("action", action);
        return button;
    }

    /**
     * 建構填充 Box
     */
    private ObjectNode createFillerBox() {
        ObjectNode filler = objectMapper.createObjectNode();
        filler.put("type", "filler");
        filler.put("flex", 1);
        return filler;
    }

    // ========================================
    // 6. 預約確認
    // ========================================

    /**
     * 建構預約確認訊息
     *
     * @param context 對話上下文
     * @return Flex Message 內容
     */
    public JsonNode buildBookingConfirmation(ConversationContext context) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "請確認預約資訊");
        headerText.put("color", "#FFFFFF");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 服務
        bodyContents.add(createInfoRow("服務項目", context.getSelectedServiceName()));

        // 員工
        String staffName = context.getSelectedStaffName() != null
                ? context.getSelectedStaffName()
                : "不指定";
        bodyContents.add(createInfoRow("服務人員", staffName));

        // 日期
        String dateStr = context.getSelectedDate()
                .format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"));
        bodyContents.add(createInfoRow("日期", dateStr));

        // 時間
        String timeStr = context.getSelectedTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        bodyContents.add(createInfoRow("時間", timeStr));

        // 時長
        if (context.getSelectedServiceDuration() != null) {
            bodyContents.add(createInfoRow("時長", context.getSelectedServiceDuration() + " 分鐘"));
        }

        // 價格
        if (context.getSelectedServicePrice() != null) {
            bodyContents.add(createInfoRow("預估金額", "NT$ " + context.getSelectedServicePrice()));
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("取消", "action=cancel_booking", SECONDARY_COLOR));
        footerContents.add(createButton("確認預約", "action=confirm_booking", PRIMARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構資訊行
     */
    private ObjectNode createInfoRow(String label, String value) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("type", "box");
        row.put("layout", "horizontal");
        row.put("spacing", "md");

        ArrayNode contents = objectMapper.createArrayNode();

        ObjectNode labelText = objectMapper.createObjectNode();
        labelText.put("type", "text");
        labelText.put("text", label);
        labelText.put("size", "sm");
        labelText.put("color", SECONDARY_COLOR);
        labelText.put("flex", 2);
        contents.add(labelText);

        ObjectNode valueText = objectMapper.createObjectNode();
        valueText.put("type", "text");
        valueText.put("text", value);
        valueText.put("size", "sm");
        valueText.put("weight", "bold");
        valueText.put("flex", 3);
        valueText.put("wrap", true);
        contents.add(valueText);

        row.set("contents", contents);
        return row;
    }

    // ========================================
    // 7. 預約成功
    // ========================================

    /**
     * 建構預約成功訊息
     *
     * @param context   對話上下文
     * @param bookingNo 預約編號
     * @return Flex Message 內容
     */
    public JsonNode buildBookingSuccess(ConversationContext context, String bookingNo) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", "\u2714"); // 勾選符號
        icon.put("size", "3xl");
        icon.put("color", "#FFFFFF");
        icon.put("align", "center");
        headerContents.add(icon);

        ObjectNode title = objectMapper.createObjectNode();
        title.put("type", "text");
        title.put("text", "預約成功！");
        title.put("size", "xl");
        title.put("weight", "bold");
        title.put("color", "#FFFFFF");
        title.put("align", "center");
        headerContents.add(title);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 預約編號
        ObjectNode bookingNoRow = objectMapper.createObjectNode();
        bookingNoRow.put("type", "box");
        bookingNoRow.put("layout", "vertical");
        bookingNoRow.put("spacing", "xs");

        ObjectNode bookingNoLabel = objectMapper.createObjectNode();
        bookingNoLabel.put("type", "text");
        bookingNoLabel.put("text", "預約編號");
        bookingNoLabel.put("size", "sm");
        bookingNoLabel.put("color", SECONDARY_COLOR);

        ObjectNode bookingNoValue = objectMapper.createObjectNode();
        bookingNoValue.put("type", "text");
        bookingNoValue.put("text", bookingNo);
        bookingNoValue.put("size", "xl");
        bookingNoValue.put("weight", "bold");

        bookingNoRow.set("contents", objectMapper.createArrayNode().add(bookingNoLabel).add(bookingNoValue));
        bodyContents.add(bookingNoRow);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 預約詳情
        bodyContents.add(createInfoRow("服務項目", context.getSelectedServiceName()));
        bodyContents.add(createInfoRow("服務人員",
                context.getSelectedStaffName() != null ? context.getSelectedStaffName() : "不指定"));
        bodyContents.add(createInfoRow("日期",
                context.getSelectedDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"))));
        bodyContents.add(createInfoRow("時間",
                context.getSelectedTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("查看我的預約", "action=view_bookings", LINK_COLOR));
        footerContents.add(createButton("返回主選單", "action=main_menu", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構預約列表訊息
     *
     * @param bookings 預約列表
     * @return Flex Message 內容
     */
    public JsonNode buildBookingList(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            // 無預約時顯示空狀態
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "md");
            body.put("paddingAll", "20px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            ObjectNode emptyIcon = objectMapper.createObjectNode();
            emptyIcon.put("type", "text");
            emptyIcon.put("text", "\uD83D\uDCC5");
            emptyIcon.put("size", "3xl");
            emptyIcon.put("align", "center");
            bodyContents.add(emptyIcon);

            ObjectNode emptyText = objectMapper.createObjectNode();
            emptyText.put("type", "text");
            emptyText.put("text", "目前沒有預約");
            emptyText.put("size", "lg");
            emptyText.put("align", "center");
            emptyText.put("color", SECONDARY_COLOR);
            emptyText.put("margin", "lg");
            bodyContents.add(emptyText);

            body.set("contents", bodyContents);
            bubble.set("body", body);

            // Footer
            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("paddingAll", "15px");

            footer.set("contents", objectMapper.createArrayNode().add(
                    createButton("立即預約", "action=start_booking", PRIMARY_COLOR)
            ));
            bubble.set("footer", footer);

            return bubble;
        }

        // 有預約時顯示列表
        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (Booking booking : bookings) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // Header with status
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", getStatusColor(booking.getStatus()));
            header.put("paddingAll", "10px");

            ObjectNode statusText = objectMapper.createObjectNode();
            statusText.put("type", "text");
            statusText.put("text", getStatusText(booking.getStatus()));
            statusText.put("size", "sm");
            statusText.put("color", "#FFFFFF");
            statusText.put("align", "center");
            statusText.put("weight", "bold");

            header.set("contents", objectMapper.createArrayNode().add(statusText));
            bubble.set("header", header);

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "sm");
            body.put("paddingAll", "15px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            // 服務名稱
            ObjectNode serviceName = objectMapper.createObjectNode();
            serviceName.put("type", "text");
            serviceName.put("text", booking.getServiceName());
            serviceName.put("size", "lg");
            serviceName.put("weight", "bold");
            bodyContents.add(serviceName);

            // 日期時間
            ObjectNode dateTime = objectMapper.createObjectNode();
            dateTime.put("type", "text");
            dateTime.put("text", booking.getBookingDate().format(DateTimeFormatter.ofPattern("M/d (E)")) +
                    " " + booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            dateTime.put("size", "md");
            dateTime.put("color", SECONDARY_COLOR);
            bodyContents.add(dateTime);

            // 員工
            if (booking.getStaffName() != null) {
                ObjectNode staffName = objectMapper.createObjectNode();
                staffName.put("type", "text");
                staffName.put("text", "服務人員：" + booking.getStaffName());
                staffName.put("size", "sm");
                staffName.put("color", SECONDARY_COLOR);
                bodyContents.add(staffName);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構預約狀態通知訊息
     *
     * @param booking   預約
     * @param newStatus 新狀態
     * @param message   附加訊息
     * @return Flex Message 內容
     */
    public JsonNode buildBookingStatusNotification(Booking booking, BookingStatus newStatus, String message) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", getStatusColor(newStatus));
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode title = objectMapper.createObjectNode();
        title.put("type", "text");
        title.put("text", getNotificationTitle(newStatus));
        title.put("size", "lg");
        title.put("weight", "bold");
        title.put("color", "#FFFFFF");
        title.put("align", "center");
        headerContents.add(title);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 預約詳情
        bodyContents.add(createInfoRow("服務項目", booking.getServiceName()));
        bodyContents.add(createInfoRow("日期",
                booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"))));
        bodyContents.add(createInfoRow("時間",
                booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        booking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        if (booking.getStaffName() != null) {
            bodyContents.add(createInfoRow("服務人員", booking.getStaffName()));
        }

        // 附加訊息
        if (message != null && !message.isEmpty()) {
            ObjectNode separator = objectMapper.createObjectNode();
            separator.put("type", "separator");
            separator.put("margin", "lg");
            bodyContents.add(separator);

            ObjectNode messageText = objectMapper.createObjectNode();
            messageText.put("type", "text");
            messageText.put("text", message);
            messageText.put("size", "sm");
            messageText.put("color", SECONDARY_COLOR);
            messageText.put("wrap", true);
            messageText.put("margin", "lg");
            bodyContents.add(messageText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("查看我的預約", "action=view_bookings", LINK_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 取得狀態顏色
     */
    private String getStatusColor(BookingStatus status) {
        if (status == null) return SECONDARY_COLOR;
        return switch (status) {
            case PENDING -> "#FFA500";       // 橙色 - 待確認
            case CONFIRMED -> PRIMARY_COLOR; // 綠色 - 已確認
            case IN_PROGRESS -> "#2196F3";   // 藍色 - 進行中
            case COMPLETED -> "#4CAF50";     // 綠色 - 已完成
            case CANCELLED -> "#9E9E9E";     // 灰色 - 已取消
            case NO_SHOW -> "#F44336";       // 紅色 - 未到
        };
    }

    /**
     * 取得狀態文字
     */
    private String getStatusText(BookingStatus status) {
        if (status == null) return "未知";
        return switch (status) {
            case PENDING -> "待確認";
            case CONFIRMED -> "已確認";
            case IN_PROGRESS -> "進行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case NO_SHOW -> "未到";
        };
    }

    /**
     * 取得通知標題
     */
    private String getNotificationTitle(BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> "預約已確認 ✓";
            case CANCELLED -> "預約已取消";
            case COMPLETED -> "服務已完成";
            case NO_SHOW -> "預約標記為未到";
            default -> "預約狀態更新";
        };
    }

    // ========================================
    // 8. 預約修改通知
    // ========================================

    /**
     * 建構預約修改通知訊息
     *
     * @param booking           預約
     * @param changeDescription 變更描述
     * @return Flex Message 內容
     */
    public JsonNode buildBookingModificationNotification(Booking booking, String changeDescription) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#FF9800");  // 橙色 - 表示修改
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode title = objectMapper.createObjectNode();
        title.put("type", "text");
        title.put("text", "預約資訊已更新");
        title.put("size", "lg");
        title.put("weight", "bold");
        title.put("color", "#FFFFFF");
        title.put("align", "center");
        headerContents.add(title);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 預約詳情
        bodyContents.add(createInfoRow("服務項目", booking.getServiceName()));
        bodyContents.add(createInfoRow("日期",
                booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"))));
        bodyContents.add(createInfoRow("時間",
                booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        booking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        if (booking.getStaffName() != null) {
            bodyContents.add(createInfoRow("服務人員", booking.getStaffName()));
        }

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 變更說明
        ObjectNode changeLabel = objectMapper.createObjectNode();
        changeLabel.put("type", "text");
        changeLabel.put("text", "變更內容");
        changeLabel.put("size", "sm");
        changeLabel.put("color", SECONDARY_COLOR);
        changeLabel.put("margin", "lg");
        bodyContents.add(changeLabel);

        ObjectNode changeText = objectMapper.createObjectNode();
        changeText.put("type", "text");
        changeText.put("text", changeDescription);
        changeText.put("size", "sm");
        changeText.put("wrap", true);
        changeText.put("margin", "sm");
        bodyContents.add(changeText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("查看我的預約", "action=view_bookings", LINK_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 共用元件
    // ========================================

    /**
     * 建構按鈕
     */
    private ObjectNode createButton(String label, String data, String color) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        button.put("style", "primary");
        button.put("color", color);
        button.put("flex", 1);

        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", label);
        action.put("data", data);

        button.set("action", action);
        return button;
    }

    /**
     * 建構返回 Footer
     */
    private ObjectNode createBackFooter() {
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("返回上一步", "action=go_back", SECONDARY_COLOR)
        ));

        return footer;
    }

    // ========================================
    // 9. 預約列表（含取消按鈕）
    // ========================================

    /**
     * 建構預約列表訊息（含取消按鈕）
     *
     * @param bookings 預約列表
     * @return Flex Message 內容
     */
    public JsonNode buildBookingListWithCancel(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return buildBookingList(bookings);
        }

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (Booking booking : bookings) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // Header with status
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", getStatusColor(booking.getStatus()));
            header.put("paddingAll", "10px");

            ObjectNode statusText = objectMapper.createObjectNode();
            statusText.put("type", "text");
            statusText.put("text", getStatusText(booking.getStatus()));
            statusText.put("size", "sm");
            statusText.put("color", "#FFFFFF");
            statusText.put("align", "center");
            statusText.put("weight", "bold");

            header.set("contents", objectMapper.createArrayNode().add(statusText));
            bubble.set("header", header);

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "sm");
            body.put("paddingAll", "15px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            // 服務名稱
            ObjectNode serviceName = objectMapper.createObjectNode();
            serviceName.put("type", "text");
            serviceName.put("text", booking.getServiceName());
            serviceName.put("size", "lg");
            serviceName.put("weight", "bold");
            bodyContents.add(serviceName);

            // 日期時間
            ObjectNode dateTime = objectMapper.createObjectNode();
            dateTime.put("type", "text");
            dateTime.put("text", booking.getBookingDate().format(DateTimeFormatter.ofPattern("M/d (E)")) +
                    " " + booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            dateTime.put("size", "md");
            dateTime.put("color", SECONDARY_COLOR);
            bodyContents.add(dateTime);

            // 員工
            if (booking.getStaffName() != null) {
                ObjectNode staffName = objectMapper.createObjectNode();
                staffName.put("type", "text");
                staffName.put("text", "服務人員：" + booking.getStaffName());
                staffName.put("size", "sm");
                staffName.put("color", SECONDARY_COLOR);
                bodyContents.add(staffName);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            // Footer - 可取消的預約顯示取消按鈕
            if (booking.isCancellable()) {
                ObjectNode footer = objectMapper.createObjectNode();
                footer.put("type", "box");
                footer.put("layout", "vertical");
                footer.put("paddingAll", "10px");

                footer.set("contents", objectMapper.createArrayNode().add(
                        createButton("取消預約", "action=cancel_booking_request&bookingId=" + booking.getId(), "#DC3545")
                ));
                bubble.set("footer", footer);
            }

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    // ========================================
    // 10. 取消預約確認
    // ========================================

    /**
     * 建構取消預約確認訊息
     *
     * @param booking 預約
     * @return Flex Message 內容
     */
    public JsonNode buildCancelConfirmation(Booking booking) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#DC3545");
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "確認取消預約？");
        headerText.put("color", "#FFFFFF");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");
        headerText.put("align", "center");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        bodyContents.add(createInfoRow("服務項目", booking.getServiceName()));
        bodyContents.add(createInfoRow("日期",
                booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"))));
        bodyContents.add(createInfoRow("時間",
                booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        if (booking.getStaffName() != null) {
            bodyContents.add(createInfoRow("服務人員", booking.getStaffName()));
        }

        // 警告文字
        ObjectNode warning = objectMapper.createObjectNode();
        warning.put("type", "text");
        warning.put("text", "取消後無法復原，確定要取消嗎？");
        warning.put("size", "sm");
        warning.put("color", "#DC3545");
        warning.put("wrap", true);
        warning.put("margin", "lg");
        bodyContents.add(warning);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("返回", "action=view_bookings", SECONDARY_COLOR));
        footerContents.add(createButton("確認取消", "action=confirm_cancel_booking&bookingId=" + booking.getId(), "#DC3545"));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 11. 票券相關
    // ========================================

    /**
     * 建構可領取票券列表
     *
     * @param coupons 票券列表
     * @return Flex Message 內容
     */
    public JsonNode buildAvailableCouponList(List<Coupon> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "20px");

            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", "目前沒有可領取的票券");
            text.put("align", "center");
            text.put("color", SECONDARY_COLOR);

            body.set("contents", objectMapper.createArrayNode().add(text));
            bubble.set("body", body);

            return bubble;
        }

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (Coupon coupon : coupons) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // Header
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", "#FF6B6B");
            header.put("paddingAll", "10px");

            ObjectNode headerText = objectMapper.createObjectNode();
            headerText.put("type", "text");
            headerText.put("text", "\uD83C\uDF81 優惠券");
            headerText.put("size", "sm");
            headerText.put("color", "#FFFFFF");
            headerText.put("align", "center");

            header.set("contents", objectMapper.createArrayNode().add(headerText));
            bubble.set("header", header);

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "sm");
            body.put("paddingAll", "15px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            // 票券名稱
            ObjectNode nameText = objectMapper.createObjectNode();
            nameText.put("type", "text");
            nameText.put("text", coupon.getName());
            nameText.put("size", "lg");
            nameText.put("weight", "bold");
            nameText.put("wrap", true);
            bodyContents.add(nameText);

            // 票券描述
            if (coupon.getDescription() != null) {
                ObjectNode descText = objectMapper.createObjectNode();
                descText.put("type", "text");
                descText.put("text", coupon.getDescription());
                descText.put("size", "sm");
                descText.put("color", SECONDARY_COLOR);
                descText.put("wrap", true);
                bodyContents.add(descText);
            }

            // 有效期限
            if (coupon.getValidEndAt() != null) {
                ObjectNode dateText = objectMapper.createObjectNode();
                dateText.put("type", "text");
                dateText.put("text", "有效期限：" + coupon.getValidEndAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
                dateText.put("size", "xs");
                dateText.put("color", SECONDARY_COLOR);
                bodyContents.add(dateText);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            // Footer
            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("paddingAll", "10px");

            footer.set("contents", objectMapper.createArrayNode().add(
                    createButton("領取", "action=receive_coupon&couponId=" + coupon.getId(), PRIMARY_COLOR)
            ));
            bubble.set("footer", footer);

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構已領取票券列表
     *
     * @param instances   票券實例列表
     * @param couponNames 票券 ID 對應名稱
     * @return Flex Message 內容
     */
    public JsonNode buildMyCouponList(List<CouponInstance> instances, Map<String, String> couponNames) {
        if (instances == null || instances.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "20px");

            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", "您目前沒有票券");
            text.put("align", "center");
            text.put("color", SECONDARY_COLOR);

            body.set("contents", objectMapper.createArrayNode().add(text));
            bubble.set("body", body);

            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("paddingAll", "15px");

            footer.set("contents", objectMapper.createArrayNode().add(
                    createButton("領取票券", "action=view_coupons", PRIMARY_COLOR)
            ));
            bubble.set("footer", footer);

            return bubble;
        }

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (CouponInstance instance : instances) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // 狀態顏色
            String statusColor = switch (instance.getStatus()) {
                case UNUSED -> PRIMARY_COLOR;
                case USED -> SECONDARY_COLOR;
                case EXPIRED -> "#9E9E9E";
                case VOIDED -> "#9E9E9E";
            };

            // Header
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", statusColor);
            header.put("paddingAll", "10px");

            ObjectNode headerText = objectMapper.createObjectNode();
            headerText.put("type", "text");
            headerText.put("text", getCouponStatusText(instance.getStatus()));
            headerText.put("size", "sm");
            headerText.put("color", "#FFFFFF");
            headerText.put("align", "center");

            header.set("contents", objectMapper.createArrayNode().add(headerText));
            bubble.set("header", header);

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "sm");
            body.put("paddingAll", "15px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            // 票券名稱
            ObjectNode nameText = objectMapper.createObjectNode();
            nameText.put("type", "text");
            String couponName = couponNames.getOrDefault(instance.getCouponId(), "票券");
            nameText.put("text", couponName);
            nameText.put("size", "lg");
            nameText.put("weight", "bold");
            nameText.put("wrap", true);
            bodyContents.add(nameText);

            // 票券代碼
            ObjectNode codeText = objectMapper.createObjectNode();
            codeText.put("type", "text");
            codeText.put("text", "序號：" + instance.getCode());
            codeText.put("size", "sm");
            codeText.put("color", SECONDARY_COLOR);
            bodyContents.add(codeText);

            // 有效期限
            if (instance.getExpiresAt() != null) {
                ObjectNode expiryText = objectMapper.createObjectNode();
                expiryText.put("type", "text");
                expiryText.put("text", "有效至：" + instance.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
                expiryText.put("size", "xs");
                expiryText.put("color", SECONDARY_COLOR);
                bodyContents.add(expiryText);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 取得票券狀態文字
     */
    private String getCouponStatusText(CouponInstanceStatus status) {
        return switch (status) {
            case UNUSED -> "可使用";
            case USED -> "已使用";
            case EXPIRED -> "已過期";
            case VOIDED -> "已作廢";
        };
    }

    // ========================================
    // 12. 會員資訊
    // ========================================

    /**
     * 建構會員資訊訊息
     *
     * @param customer            顧客
     * @param bookingCount        預約次數
     * @param membershipLevelName 會員等級名稱（可為 null）
     * @return Flex Message 內容
     */
    public JsonNode buildMemberInfo(Customer customer, long bookingCount, String membershipLevelName) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", "\uD83D\uDC64");
        icon.put("size", "3xl");
        icon.put("align", "center");
        headerContents.add(icon);

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", customer.getName());
        nameText.put("size", "xl");
        nameText.put("weight", "bold");
        nameText.put("color", "#FFFFFF");
        nameText.put("align", "center");
        headerContents.add(nameText);

        // 會員等級
        if (membershipLevelName != null) {
            ObjectNode levelText = objectMapper.createObjectNode();
            levelText.put("type", "text");
            levelText.put("text", membershipLevelName);
            levelText.put("size", "sm");
            levelText.put("color", "#FFFFFF");
            levelText.put("align", "center");
            headerContents.add(levelText);
        }

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 點數
        ObjectNode pointsBox = objectMapper.createObjectNode();
        pointsBox.put("type", "box");
        pointsBox.put("layout", "horizontal");
        pointsBox.put("paddingAll", "15px");
        pointsBox.put("backgroundColor", "#F5F5F5");
        pointsBox.put("cornerRadius", "10px");

        ArrayNode pointsContents = objectMapper.createArrayNode();

        ObjectNode pointsLabel = objectMapper.createObjectNode();
        pointsLabel.put("type", "text");
        pointsLabel.put("text", "\uD83D\uDCB0 點數餘額");
        pointsLabel.put("flex", 2);
        pointsContents.add(pointsLabel);

        ObjectNode pointsValue = objectMapper.createObjectNode();
        pointsValue.put("type", "text");
        pointsValue.put("text", String.format("%d 點", customer.getPointBalance() != null ? customer.getPointBalance() : 0));
        pointsValue.put("weight", "bold");
        pointsValue.put("align", "end");
        pointsValue.put("flex", 1);
        pointsContents.add(pointsValue);

        pointsBox.set("contents", pointsContents);
        bodyContents.add(pointsBox);

        // 預約次數
        bodyContents.add(createInfoRow("累計預約", bookingCount + " 次"));

        // 電話
        if (customer.getPhone() != null) {
            bodyContents.add(createInfoRow("聯絡電話", customer.getPhone()));
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("開始預約", "action=start_booking", PRIMARY_COLOR));
        footerContents.add(createButton("我的票券", "action=view_my_coupons", LINK_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 13. 商品相關
    // ========================================

    /**
     * 建構商品選單（Carousel）
     *
     * @param products 商品列表
     * @return Flex Message 內容
     */
    public JsonNode buildProductMenu(List<Product> products) {
        if (products == null || products.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "20px");

            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", "目前沒有上架商品");
            text.put("align", "center");
            text.put("color", SECONDARY_COLOR);

            body.set("contents", objectMapper.createArrayNode().add(text));
            bubble.set("body", body);

            return bubble;
        }

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        for (Product product : products) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");
            bubble.put("size", "kilo");

            // Body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "sm");
            body.put("paddingAll", "15px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            // 商品名稱
            ObjectNode nameText = objectMapper.createObjectNode();
            nameText.put("type", "text");
            nameText.put("text", product.getName());
            nameText.put("size", "lg");
            nameText.put("weight", "bold");
            nameText.put("wrap", true);
            bodyContents.add(nameText);

            // 商品描述
            if (product.getDescription() != null) {
                ObjectNode descText = objectMapper.createObjectNode();
                descText.put("type", "text");
                descText.put("text", product.getDescription());
                descText.put("size", "sm");
                descText.put("color", SECONDARY_COLOR);
                descText.put("wrap", true);
                descText.put("maxLines", 2);
                bodyContents.add(descText);
            }

            // 價格
            ObjectNode priceText = objectMapper.createObjectNode();
            priceText.put("type", "text");
            priceText.put("text", String.format("NT$ %d", product.getPrice().intValue()));
            priceText.put("size", "lg");
            priceText.put("weight", "bold");
            priceText.put("color", PRIMARY_COLOR);
            bodyContents.add(priceText);

            // 庫存
            if (product.getStockQuantity() != null && product.getStockQuantity() <= 10) {
                ObjectNode stockText = objectMapper.createObjectNode();
                stockText.put("type", "text");
                stockText.put("text", "僅剩 " + product.getStockQuantity() + " 件");
                stockText.put("size", "xs");
                stockText.put("color", "#FF6B6B");
                bodyContents.add(stockText);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            // Footer
            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("paddingAll", "10px");

            String postbackData = String.format(
                    "action=select_product&productId=%s&productName=%s&price=%d",
                    product.getId(),
                    product.getName(),
                    product.getPrice().intValue()
            );

            footer.set("contents", objectMapper.createArrayNode().add(
                    createButton("選購", postbackData, PRIMARY_COLOR)
            ));
            bubble.set("footer", footer);

            bubbles.add(bubble);
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構數量選擇選單
     *
     * @param productName 商品名稱
     * @param price       單價
     * @return Flex Message 內容
     */
    public JsonNode buildQuantityMenu(String productName, Integer price) {
        return buildQuantityMenu(productName, price, 5);
    }

    /**
     * 建構數量選擇選單（指定最大數量）
     *
     * @param productName 商品名稱
     * @param price       單價
     * @param maxQuantity 最大數量
     * @return Flex Message 內容
     */
    public JsonNode buildQuantityMenu(String productName, Integer price, int maxQuantity) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "選擇數量");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 商品資訊
        ObjectNode productInfo = objectMapper.createObjectNode();
        productInfo.put("type", "text");
        productInfo.put("text", productName);
        productInfo.put("weight", "bold");
        productInfo.put("wrap", true);
        bodyContents.add(productInfo);

        ObjectNode priceInfo = objectMapper.createObjectNode();
        priceInfo.put("type", "text");
        priceInfo.put("text", String.format("單價：NT$ %d", price));
        priceInfo.put("size", "sm");
        priceInfo.put("color", SECONDARY_COLOR);
        bodyContents.add(priceInfo);

        // 數量按鈕
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 數量選項（1-maxQuantity，最多顯示 5 個按鈕）
        int displayCount = Math.min(maxQuantity, 5);
        for (int i = 1; i <= displayCount; i++) {
            int total = price * i;
            ObjectNode quantityBtn = createQuantityButton(i, total);
            bodyContents.add(quantityBtn);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        bubble.set("footer", createBackFooter());

        return bubble;
    }

    /**
     * 建構數量按鈕
     */
    private ObjectNode createQuantityButton(int quantity, int total) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        button.put("style", "secondary");
        button.put("height", "sm");
        button.put("margin", "sm");

        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", String.format("%d 件 - NT$ %d", quantity, total));
        action.put("data", "action=select_quantity&quantity=" + quantity);

        button.set("action", action);
        return button;
    }

    /**
     * 建構購買確認訊息
     *
     * @param context 對話上下文
     * @return Flex Message 內容
     */
    public JsonNode buildPurchaseConfirmation(ConversationContext context) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "確認購買");
        headerText.put("color", "#FFFFFF");
        headerText.put("size", "lg");
        headerText.put("weight", "bold");
        headerText.put("align", "center");

        header.set("contents", objectMapper.createArrayNode().add(headerText));
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 商品名稱
        bodyContents.add(createInfoRow("商品", context.getSelectedProductName()));

        // 數量
        bodyContents.add(createInfoRow("數量", context.getSelectedQuantity() + " 件"));

        // 單價
        bodyContents.add(createInfoRow("單價", "NT$ " + context.getSelectedProductPrice()));

        // 總金額
        int total = context.getSelectedProductPrice() * context.getSelectedQuantity();
        ObjectNode totalRow = objectMapper.createObjectNode();
        totalRow.put("type", "box");
        totalRow.put("layout", "horizontal");
        totalRow.put("spacing", "md");
        totalRow.put("margin", "lg");

        ArrayNode totalContents = objectMapper.createArrayNode();

        ObjectNode totalLabel = objectMapper.createObjectNode();
        totalLabel.put("type", "text");
        totalLabel.put("text", "總金額");
        totalLabel.put("size", "lg");
        totalLabel.put("weight", "bold");
        totalLabel.put("flex", 2);
        totalContents.add(totalLabel);

        ObjectNode totalValue = objectMapper.createObjectNode();
        totalValue.put("type", "text");
        totalValue.put("text", "NT$ " + total);
        totalValue.put("size", "xl");
        totalValue.put("weight", "bold");
        totalValue.put("color", PRIMARY_COLOR);
        totalValue.put("flex", 3);
        totalValue.put("align", "end");
        totalContents.add(totalValue);

        totalRow.set("contents", totalContents);
        bodyContents.add(totalRow);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("取消", "action=main_menu", SECONDARY_COLOR));
        footerContents.add(createButton("確認購買", "action=confirm_purchase", PRIMARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 通用提示訊息
    // ========================================

    /**
     * 建構取消操作提示
     *
     * @return Flex Message 內容
     */
    public JsonNode buildCancelPrompt() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", "請點選上方選項繼續操作");
        text.put("size", "md");
        text.put("align", "center");
        text.put("color", SECONDARY_COLOR);
        bodyContents.add(text);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("取消目前操作", "action=cancel_booking", "#FF6B6B"));
        footerContents.add(createButton("返回主選單", "action=main_menu", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 預約提醒
    // ========================================

    /**
     * 建構預約提醒訊息
     *
     * @param booking 預約
     * @return Flex Message 內容
     */
    public JsonNode buildBookingReminder(Booking booking) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header - 使用橙色作為提醒色
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#FF9800");
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode bellIcon = objectMapper.createObjectNode();
        bellIcon.put("type", "text");
        bellIcon.put("text", "\uD83D\uDD14 預約提醒");
        bellIcon.put("size", "lg");
        bellIcon.put("weight", "bold");
        bellIcon.put("color", "#FFFFFF");
        bellIcon.put("align", "center");
        headerContents.add(bellIcon);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 友善問候語
        ObjectNode greeting = objectMapper.createObjectNode();
        greeting.put("type", "text");
        greeting.put("text", String.format("親愛的 %s 您好", booking.getCustomerName() != null ? booking.getCustomerName() : "顧客"));
        greeting.put("size", "md");
        greeting.put("weight", "bold");
        bodyContents.add(greeting);

        ObjectNode reminderText = objectMapper.createObjectNode();
        reminderText.put("type", "text");
        reminderText.put("text", "提醒您明天有一個預約：");
        reminderText.put("size", "sm");
        reminderText.put("color", SECONDARY_COLOR);
        reminderText.put("margin", "sm");
        bodyContents.add(reminderText);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 預約詳情
        bodyContents.add(createInfoRow("服務項目", booking.getServiceName()));
        bodyContents.add(createInfoRow("預約日期",
                booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日 (E)"))));
        bodyContents.add(createInfoRow("預約時間",
                booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        booking.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        if (booking.getStaffName() != null) {
            bodyContents.add(createInfoRow("服務人員", booking.getStaffName()));
        }

        // 店家備註
        if (booking.getStoreNoteToCustomer() != null && !booking.getStoreNoteToCustomer().isEmpty()) {
            ObjectNode noteSeparator = objectMapper.createObjectNode();
            noteSeparator.put("type", "separator");
            noteSeparator.put("margin", "lg");
            bodyContents.add(noteSeparator);

            ObjectNode noteText = objectMapper.createObjectNode();
            noteText.put("type", "text");
            noteText.put("text", "店家備註：" + booking.getStoreNoteToCustomer());
            noteText.put("size", "sm");
            noteText.put("color", SECONDARY_COLOR);
            noteText.put("wrap", true);
            noteText.put("margin", "lg");
            bodyContents.add(noteText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("查看我的預約", "action=view_bookings", LINK_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }
}
