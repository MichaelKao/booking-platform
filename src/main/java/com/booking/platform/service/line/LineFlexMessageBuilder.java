package com.booking.platform.service.line;

import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.enums.ServiceStatus;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
     * 建構日期選單
     *
     * @return Flex Message 內容
     */
    public JsonNode buildDateMenu() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", "請選擇日期");
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

        LocalDate today = LocalDate.now();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("M/d (E)");
        DateTimeFormatter dataFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            String displayDate = date.format(displayFormatter);
            String dataDate = date.format(dataFormatter);

            String label = i == 0 ? "今天 " + displayDate : displayDate;

            bodyContents.add(createDateButton(label, dataDate));
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 返回按鈕
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
        // TODO: 實際查詢可用時段
        List<LocalTime> availableSlots = generateAvailableSlots(date);

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
     * 產生可用時段（模擬）
     */
    private List<LocalTime> generateAvailableSlots(LocalDate date) {
        List<LocalTime> slots = new ArrayList<>();

        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(18, 0);

        // 如果是今天，從下一個整點開始
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            if (now.isAfter(start)) {
                start = now.plusHours(1).withMinute(0).withSecond(0);
            }
        }

        while (!start.isAfter(end.minusHours(1))) {
            slots.add(start);
            start = start.plusMinutes(30);
        }

        return slots;
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
}
