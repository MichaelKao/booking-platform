package com.booking.platform.service.line;

import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.catalog.ServiceCategory;
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
import com.booking.platform.repository.ServiceCategoryRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffLeaveRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.StaffScheduleRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.entity.line.TenantLineConfig;
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
import java.time.LocalDateTime;
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

    @org.springframework.beans.factory.annotation.Value("${app.base-url:https://booking-platform-production-1e08.up.railway.app}")
    private String appBaseUrl;

    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;
    private final TenantLineConfigRepository lineConfigRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffLeaveRepository staffLeaveRepository;
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
        String shopName = tenantOpt.map(Tenant::getName).orElse("歡迎光臨");

        // ========================================
        // 讀取自訂配置
        // ========================================
        JsonNode menuConfig = loadFlexMenuConfig(tenantId);

        // ========================================
        // 檢查是否使用輪播卡片模式
        // ========================================
        if (menuConfig != null && "carousel".equals(getConfigText(menuConfig, "menuType", ""))) {
            JsonNode cardsConfig = menuConfig.get("cards");
            if (cardsConfig != null && cardsConfig.isArray() && cardsConfig.size() > 0) {
                return buildCarouselMainMenu(shopName, menuConfig, cardsConfig);
            }
        }

        // Header 設定
        String headerColor = getConfigText(menuConfig, "color", PRIMARY_COLOR);
        String headerTitle = getConfigText(menuConfig, "title", "✨ " + shopName)
                .replace("{shopName}", shopName);
        String headerSubtitle = getConfigText(menuConfig, "headerSubtitle", "歡迎光臨！請問需要什麼服務呢？");
        boolean showTip = menuConfig != null && menuConfig.has("showTip") ? menuConfig.get("showTip").asBoolean(true) : true;

        // 按鈕設定（7 個預設按鈕）
        String[][] defaultButtons = {
            {"start_booking",   PRIMARY_COLOR, "📅", "開始預約",  "快速預約服務"},
            {"view_bookings",   LINK_COLOR,    "📋", "我的預約",  "查看或取消預約"},
            {"start_shopping",  "#FF9800",     "🛍️", "瀏覽商品",  "購買優惠商品"},
            {"view_coupons",    "#E91E63",     "🎁", "領取票券",  null},
            {"view_my_coupons", "#9C27B0",     "🎫", "我的票券",  null},
            {"view_member_info","#673AB7",     "👤", "會員資訊",  "查看點數與等級"},
            {"contact_shop",    "#5C6BC0",     "📞", "聯絡店家",  "地址、電話、營業時間"}
        };
        JsonNode buttonsConfig = menuConfig != null ? menuConfig.get("buttons") : null;

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header - 店家名稱與歡迎語
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", headerColor);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode shopNameText = objectMapper.createObjectNode();
        shopNameText.put("type", "text");
        shopNameText.put("text", headerTitle);
        shopNameText.put("color", "#FFFFFF");
        shopNameText.put("size", "xl");
        shopNameText.put("weight", "bold");
        shopNameText.put("align", "center");
        headerContents.add(shopNameText);

        ObjectNode welcomeText = objectMapper.createObjectNode();
        welcomeText.put("type", "text");
        welcomeText.put("text", headerSubtitle);
        welcomeText.put("color", "#FFFFFF");
        welcomeText.put("size", "sm");
        welcomeText.put("align", "center");
        welcomeText.put("margin", "md");
        headerContents.add(welcomeText);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body - 使用提示
        if (showTip) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("spacing", "md");
            body.put("paddingAll", "20px");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            ObjectNode tipBox = objectMapper.createObjectNode();
            tipBox.put("type", "box");
            tipBox.put("layout", "vertical");
            tipBox.put("backgroundColor", "#F5F5F5");
            tipBox.put("cornerRadius", "8px");
            tipBox.put("paddingAll", "12px");

            ArrayNode tipContents = objectMapper.createArrayNode();

            ObjectNode tipTitle = objectMapper.createObjectNode();
            tipTitle.put("type", "text");
            tipTitle.put("text", "💡 使用提示");
            tipTitle.put("size", "sm");
            tipTitle.put("weight", "bold");
            tipTitle.put("color", "#333333");
            tipContents.add(tipTitle);

            ObjectNode tipText = objectMapper.createObjectNode();
            tipText.put("type", "text");
            tipText.put("text", "點擊下方按鈕開始使用，或直接輸入「預約」、「幫助」等關鍵字");
            tipText.put("size", "xs");
            tipText.put("color", SECONDARY_COLOR);
            tipText.put("wrap", true);
            tipText.put("margin", "sm");
            tipContents.add(tipText);

            tipBox.set("contents", tipContents);
            bodyContents.add(tipBox);

            body.set("contents", bodyContents);
            bubble.set("body", body);
        }

        // Footer - 功能按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "20px");

        ArrayNode footerContents = objectMapper.createArrayNode();

        // 按鈕 0~2：全寬按鈕（開始預約、我的預約、瀏覽商品）
        for (int i = 0; i < 3; i++) {
            String color = getButtonField(buttonsConfig, i, "color", defaultButtons[i][1]);
            String icon = getButtonField(buttonsConfig, i, "icon", defaultButtons[i][2]);
            String title = getButtonField(buttonsConfig, i, "title", defaultButtons[i][3]);
            String subtitle = getButtonField(buttonsConfig, i, "subtitle", defaultButtons[i][4]);
            footerContents.add(createMenuButton(icon + " " + title, subtitle, "action=" + defaultButtons[i][0], color));
        }

        // 按鈕 3~4：票券並排按鈕
        ObjectNode couponRow = objectMapper.createObjectNode();
        couponRow.put("type", "box");
        couponRow.put("layout", "horizontal");
        couponRow.put("spacing", "sm");
        couponRow.put("margin", "sm");

        ArrayNode couponRowContents = objectMapper.createArrayNode();
        for (int i = 3; i <= 4; i++) {
            String color = getButtonField(buttonsConfig, i, "color", defaultButtons[i][1]);
            String icon = getButtonField(buttonsConfig, i, "icon", defaultButtons[i][2]);
            String title = getButtonField(buttonsConfig, i, "title", defaultButtons[i][3]);
            couponRowContents.add(createCompactMenuButton(icon + " " + title, "action=" + defaultButtons[i][0], color));
        }
        couponRow.set("contents", couponRowContents);
        footerContents.add(couponRow);

        // 按鈕 5~6：會員資訊、聯絡店家
        for (int i = 5; i < 7; i++) {
            String color = getButtonField(buttonsConfig, i, "color", defaultButtons[i][1]);
            String icon = getButtonField(buttonsConfig, i, "icon", defaultButtons[i][2]);
            String title = getButtonField(buttonsConfig, i, "title", defaultButtons[i][3]);
            String subtitle = getButtonField(buttonsConfig, i, "subtitle", defaultButtons[i][4]);
            footerContents.add(createMenuButton(icon + " " + title, subtitle, "action=" + defaultButtons[i][0], color));
        }

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 1-B. 輪播卡片主選單
    // ========================================

    /**
     * 建構輪播卡片主選單（每個功能一張卡片，可左右滑動）
     *
     * @param shopName    店家名稱
     * @param menuConfig  完整配置
     * @param cardsConfig 卡片陣列
     * @return Carousel Flex Message
     */
    private JsonNode buildCarouselMainMenu(String shopName, JsonNode menuConfig, JsonNode cardsConfig) {
        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode contents = objectMapper.createArrayNode();

        for (JsonNode card : cardsConfig) {
            ObjectNode bubble = buildCardBubble(card, shopName);
            contents.add(bubble);
        }

        carousel.set("contents", contents);
        return carousel;
    }

    /**
     * 建構單張卡片 Bubble
     */
    private ObjectNode buildCardBubble(JsonNode card, String shopName) {
        String imageUrl = card.path("imageUrl").asText("");
        // 相對路徑轉絕對路徑（供 LINE 存取）
        if (imageUrl.startsWith("/api/public/")) {
            imageUrl = appBaseUrl + imageUrl;
        }
        int imageHeight = card.path("imageHeight").asInt(50);
        String title = card.path("title").asText("功能").replace("{shopName}", shopName);
        String subtitle = card.path("subtitle").asText("").replace("{shopName}", shopName);
        String icon = card.path("icon").asText("");
        String color = card.path("color").asText(PRIMARY_COLOR);
        String action = card.path("action").asText("show_menu");
        String buttonLabel = card.path("buttonLabel").asText("前往");
        String cardSize = card.path("cardSize").asText("");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        if (!cardSize.isEmpty() && !"auto".equals(cardSize)) {
            bubble.put("size", cardSize);
        }

        // ── Hero 圖片 ──
        if (!imageUrl.isEmpty() && imageHeight > 0) {
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", imageUrl);
            hero.put("size", "full");

            // 將百分比轉為 aspectRatio（0~100 → 20:4~20:30）
            int h = Math.max(4, Math.round(imageHeight * 0.3f));
            hero.put("aspectRatio", "20:" + h);
            hero.put("aspectMode", "cover");

            // 圖片可點擊
            ObjectNode heroAction = objectMapper.createObjectNode();
            heroAction.put("type", "postback");
            heroAction.put("label", title.length() > 20 ? title.substring(0, 20) : title);
            heroAction.put("data", "action=" + action);
            hero.set("action", heroAction);

            bubble.set("hero", hero);
        }

        // ── Body ──
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "16px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 標題（含圖示）
        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", (icon.isEmpty() ? "" : icon + " ") + title);
        titleText.put("weight", "bold");
        titleText.put("size", "lg");
        titleText.put("wrap", true);
        bodyContents.add(titleText);

        // 副標題
        if (!subtitle.isEmpty()) {
            ObjectNode subText = objectMapper.createObjectNode();
            subText.put("type", "text");
            subText.put("text", subtitle);
            subText.put("size", "sm");
            subText.put("color", SECONDARY_COLOR);
            subText.put("wrap", true);
            subText.put("margin", "sm");
            bodyContents.add(subText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // ── Footer 按鈕 ──
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");

        ArrayNode footerContents = objectMapper.createArrayNode();

        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        button.put("style", "primary");
        button.put("color", color);
        button.put("height", "sm");

        ObjectNode btnAction = objectMapper.createObjectNode();
        btnAction.put("type", "postback");
        btnAction.put("label", buttonLabel.length() > 20 ? buttonLabel.substring(0, 20) : buttonLabel);
        btnAction.put("data", "action=" + action);
        button.set("action", btnAction);
        footerContents.add(button);

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 讀取 Flex Menu 自訂配置
     */
    private JsonNode loadFlexMenuConfig(String tenantId) {
        try {
            return lineConfigRepository.findByTenantId(tenantId)
                    .map(TenantLineConfig::getFlexMenuConfig)
                    .filter(config -> config != null && !config.isBlank())
                    .map(config -> {
                        try {
                            return objectMapper.readTree(config);
                        } catch (Exception e) {
                            log.warn("解析 flexMenuConfig 失敗，租戶：{}", tenantId);
                            return null;
                        }
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("讀取 flexMenuConfig 失敗，租戶：{}", tenantId);
            return null;
        }
    }

    /**
     * 從配置取得文字值，如無則用預設值
     */
    private String getConfigText(JsonNode config, String field, String defaultValue) {
        if (config != null && config.has(field) && !config.get(field).isNull()) {
            String value = config.get(field).asText();
            return value.isEmpty() ? defaultValue : value;
        }
        return defaultValue;
    }

    /**
     * 讀取預約流程步驟的自訂配置
     *
     * @param tenantId 租戶 ID
     * @param stepKey  步驟 Key（service, date, staff, time, confirm, success, note）
     * @param field    欄位名稱（color, title, icon, subtitle, imageUrl）
     * @param defaultValue 預設值
     * @return 自訂值或預設值
     */
    private String getStepConfig(String tenantId, String stepKey, String field, String defaultValue) {
        JsonNode config = loadFlexMenuConfig(tenantId);
        if (config != null && config.has("steps")) {
            JsonNode steps = config.get("steps");
            if (steps.has(stepKey)) {
                JsonNode step = steps.get(stepKey);
                if (step.has(field) && !step.get(field).isNull()) {
                    String value = step.get(field).asText();
                    if (!value.isEmpty()) return value;
                }
            }
        }
        return defaultValue;
    }

    /**
     * 讀取功能頁面的自訂配置（functions 區塊）
     *
     * @param tenantId 租戶 ID
     * @param functionKey 功能 Key（bookingList, productMenu, couponList, myCoupons, memberInfo, contactShop, bookingFlow）
     * @param field 欄位名稱（color, icon, title, subtitle, imageUrl）
     * @param defaultValue 預設值
     * @return 自訂值或預設值
     */
    private String getFunctionConfig(String tenantId, String functionKey, String field, String defaultValue) {
        JsonNode config = loadFlexMenuConfig(tenantId);
        if (config != null && config.has("functions")) {
            JsonNode functions = config.get("functions");
            if (functions.has(functionKey)) {
                JsonNode func = functions.get(functionKey);
                if (func.has(field) && !func.get(field).isNull()) {
                    String value = func.get(field).asText();
                    if (!value.isEmpty()) return value;
                }
            }
        }
        return defaultValue;
    }

    /**
     * 套用功能頁面的自訂 Header（含 icon、副標題、Hero 圖片支援）
     *
     * @param bubble 要設定 header/hero 的 Bubble
     * @param tenantId 租戶 ID
     * @param functionKey 功能 Key
     * @param defaultColor 預設背景色
     * @param defaultIcon 預設圖示
     * @param defaultTitle 預設標題
     */
    private void applyFunctionHeader(ObjectNode bubble, String tenantId, String functionKey,
                                      String defaultColor, String defaultIcon, String defaultTitle) {
        String color = getFunctionConfig(tenantId, functionKey, "color", defaultColor);
        String icon = getFunctionConfig(tenantId, functionKey, "icon", defaultIcon);
        String title = getFunctionConfig(tenantId, functionKey, "title", defaultTitle);
        String subtitle = getFunctionConfig(tenantId, functionKey, "subtitle", "");
        String heroImageUrl = getFunctionConfig(tenantId, functionKey, "imageUrl", "");

        // 組合標題
        if (!icon.isEmpty() && !title.startsWith(icon)) {
            title = icon + " " + title;
        }

        // Hero 圖片
        if (!heroImageUrl.isEmpty()) {
            if (heroImageUrl.startsWith("/api/public/")) {
                heroImageUrl = appBaseUrl + heroImageUrl;
            }
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", heroImageUrl);
            hero.put("size", "full");
            hero.put("aspectRatio", "20:8");
            hero.put("aspectMode", "cover");
            bubble.set("hero", hero);
        }

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", color);
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        // 標題
        ObjectNode headerTitle = objectMapper.createObjectNode();
        headerTitle.put("type", "text");
        headerTitle.put("text", title);
        headerTitle.put("size", "lg");
        headerTitle.put("weight", "bold");
        headerTitle.put("color", "#FFFFFF");
        headerTitle.put("align", "center");
        headerContents.add(headerTitle);

        // 副標題
        if (!subtitle.isEmpty()) {
            ObjectNode subText = objectMapper.createObjectNode();
            subText.put("type", "text");
            subText.put("text", subtitle);
            subText.put("size", "xs");
            subText.put("color", "#FFFFFF");
            subText.put("align", "center");
            subText.put("margin", "sm");
            headerContents.add(subText);
        }

        header.set("contents", headerContents);
        bubble.set("header", header);
    }

    /**
     * 建構步驟 Header（含 icon、副標題、Hero 圖片支援）
     *
     * @param stepColor 背景色
     * @param stepTitle 標題（可含 icon）
     * @param stepCounter 步驟計數文字（如 "步驟 1/4"），null 則不顯示
     * @param tenantId 租戶 ID
     * @param stepKey 步驟 Key
     * @param bubble 要設定 header/hero 的 Bubble
     */
    private void applyStepHeader(ObjectNode bubble, String tenantId, String stepKey,
                                  String stepColor, String stepTitle, String stepCounter) {
        // 讀取自訂 icon 和副標題
        String icon = getStepConfig(tenantId, stepKey, "icon", "");
        String subtitle = getStepConfig(tenantId, stepKey, "subtitle", "");
        String heroImageUrl = getStepConfig(tenantId, stepKey, "imageUrl", "");

        // 如果有 icon 且標題不以 icon 開頭，加到標題前
        if (!icon.isEmpty() && !stepTitle.startsWith(icon)) {
            stepTitle = icon + " " + stepTitle;
        }

        // Hero 圖片
        if (!heroImageUrl.isEmpty()) {
            // 相對路徑轉絕對路徑
            if (heroImageUrl.startsWith("/api/public/")) {
                heroImageUrl = appBaseUrl + heroImageUrl;
            }
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", heroImageUrl);
            hero.put("size", "full");
            hero.put("aspectRatio", "20:8");
            hero.put("aspectMode", "cover");
            bubble.set("hero", hero);
        }

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", stepColor);
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        // 步驟計數
        if (stepCounter != null && !stepCounter.isEmpty()) {
            ObjectNode stepText = objectMapper.createObjectNode();
            stepText.put("type", "text");
            stepText.put("text", stepCounter);
            stepText.put("size", "xs");
            stepText.put("color", "#FFFFFF");
            stepText.put("align", "center");
            headerContents.add(stepText);
        }

        // 標題
        ObjectNode headerTitle = objectMapper.createObjectNode();
        headerTitle.put("type", "text");
        headerTitle.put("text", stepTitle);
        headerTitle.put("size", "lg");
        headerTitle.put("weight", "bold");
        headerTitle.put("color", "#FFFFFF");
        headerTitle.put("align", "center");
        if (stepCounter != null) headerTitle.put("margin", "sm");
        headerContents.add(headerTitle);

        // 副標題
        if (!subtitle.isEmpty()) {
            ObjectNode subText = objectMapper.createObjectNode();
            subText.put("type", "text");
            subText.put("text", subtitle);
            subText.put("size", "xs");
            subText.put("color", "#FFFFFF");
            subText.put("align", "center");
            subText.put("margin", "sm");
            subText.set("offsetTop", objectMapper.valueToTree("2px"));
            headerContents.add(subText);
        }

        header.set("contents", headerContents);
        bubble.set("header", header);
    }

    /**
     * 從按鈕配置取得欄位值
     */
    private String getButtonField(JsonNode buttonsConfig, int index, String field, String defaultValue) {
        if (buttonsConfig != null && buttonsConfig.isArray() && index < buttonsConfig.size()) {
            JsonNode btn = buttonsConfig.get(index);
            if (btn.has(field) && !btn.get(field).isNull()) {
                String value = btn.get(field).asText();
                if (!value.isEmpty()) return value;
            }
        }
        return defaultValue;
    }

    /**
     * 建構主選單按鈕（帶說明文字）
     */
    private ObjectNode createMenuButton(String title, String subtitle, String postbackData, String color) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "horizontal");
        box.put("backgroundColor", color);
        box.put("cornerRadius", "8px");
        box.put("paddingAll", "12px");
        box.put("margin", "sm");

        ArrayNode contents = objectMapper.createArrayNode();

        // 文字區域
        ObjectNode textBox = objectMapper.createObjectNode();
        textBox.put("type", "box");
        textBox.put("layout", "vertical");
        textBox.put("flex", 1);

        ArrayNode textContents = objectMapper.createArrayNode();

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", title);
        titleText.put("size", "md");
        titleText.put("weight", "bold");
        titleText.put("color", "#FFFFFF");
        textContents.add(titleText);

        ObjectNode subtitleText = objectMapper.createObjectNode();
        subtitleText.put("type", "text");
        subtitleText.put("text", subtitle);
        subtitleText.put("size", "xs");
        subtitleText.put("color", "#FFFFFF");
        subtitleText.put("margin", "xs");
        textContents.add(subtitleText);

        textBox.set("contents", textContents);
        contents.add(textBox);

        // 箭頭
        ObjectNode arrow = objectMapper.createObjectNode();
        arrow.put("type", "text");
        arrow.put("text", "›");
        arrow.put("size", "xxl");
        arrow.put("color", "#FFFFFF");
        arrow.put("align", "end");
        arrow.put("gravity", "center");
        contents.add(arrow);

        box.set("contents", contents);

        // 點擊動作
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", title);
        action.put("data", postbackData);
        box.set("action", action);

        return box;
    }

    /**
     * 建構主選單精簡按鈕（無副標題，用於並排顯示）
     */
    private ObjectNode createCompactMenuButton(String title, String postbackData, String color) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "vertical");
        box.put("backgroundColor", color);
        box.put("cornerRadius", "8px");
        box.put("paddingAll", "12px");
        box.put("flex", 1);

        ArrayNode contents = objectMapper.createArrayNode();

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", title);
        titleText.put("size", "sm");
        titleText.put("weight", "bold");
        titleText.put("color", "#FFFFFF");
        titleText.put("align", "center");
        contents.add(titleText);

        box.set("contents", contents);

        // 點擊動作
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "postback");
        action.put("label", title);
        action.put("data", postbackData);
        box.set("action", action);

        return box;
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

        // 讀取步驟自訂配置
        String stepColor = getStepConfig(tenantId, "service", "color", "#4A90D9");
        String stepTitle = getStepConfig(tenantId, "service", "title", "✂️ 選擇服務");

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        // 第一個 Bubble：指引說明
        bubbles.add(buildServiceGuide(tenantId, stepColor, stepTitle));

        for (ServiceItem service : services) {
            bubbles.add(buildServiceBubble(service, stepColor, tenantId));
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構服務選單指引
     */
    private ObjectNode buildServiceGuide(String tenantId, String stepColor, String stepTitle) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "service", stepColor, stepTitle, "步驟 1/4");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode guideText = objectMapper.createObjectNode();
        guideText.put("type", "text");
        guideText.put("text", "👈 往左滑動查看所有服務項目\n\n點擊「選擇此服務」繼續下一步");
        guideText.put("size", "sm");
        guideText.put("color", SECONDARY_COLOR);
        guideText.put("wrap", true);
        bodyContents.add(guideText);

        // 流程說明
        ObjectNode flowBox = objectMapper.createObjectNode();
        flowBox.put("type", "box");
        flowBox.put("layout", "vertical");
        flowBox.put("backgroundColor", "#F5F5F5");
        flowBox.put("cornerRadius", "8px");
        flowBox.put("paddingAll", "10px");
        flowBox.put("margin", "md");

        ArrayNode flowContents = objectMapper.createArrayNode();

        String[] steps = {"1️⃣ 選擇服務", "2️⃣ 選擇日期", "3️⃣ 選擇人員", "4️⃣ 選擇時間"};
        for (String step : steps) {
            ObjectNode stepItem = objectMapper.createObjectNode();
            stepItem.put("type", "text");
            stepItem.put("text", step);
            stepItem.put("size", "xs");
            stepItem.put("color", "#666666");
            flowContents.add(stepItem);
        }

        flowBox.set("contents", flowContents);
        bodyContents.add(flowBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        bubble.set("footer", createCancelFooter());

        return bubble;
    }

    /**
     * 建構單一服務 Bubble
     */
    private ObjectNode buildServiceBubble(ServiceItem service, String stepColor, String tenantId) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 套用統一步驟 Header（含 Hero 圖片）
        applyStepHeader(bubble, tenantId, "service", stepColor, service.getName(), null);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 服務說明（如有）
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            ObjectNode descText = objectMapper.createObjectNode();
            descText.put("type", "text");
            descText.put("text", service.getDescription());
            descText.put("size", "sm");
            descText.put("color", SECONDARY_COLOR);
            descText.put("wrap", true);
            bodyContents.add(descText);

            // 分隔線
            ObjectNode separator = objectMapper.createObjectNode();
            separator.put("type", "separator");
            separator.put("margin", "md");
            bodyContents.add(separator);
        }

        // 時長與價格資訊
        ObjectNode infoBox = objectMapper.createObjectNode();
        infoBox.put("type", "box");
        infoBox.put("layout", "vertical");
        infoBox.put("spacing", "sm");
        infoBox.put("margin", "md");

        ArrayNode infoContents = objectMapper.createArrayNode();

        // 時長
        ObjectNode durationRow = objectMapper.createObjectNode();
        durationRow.put("type", "box");
        durationRow.put("layout", "horizontal");

        ArrayNode durationContents = objectMapper.createArrayNode();

        ObjectNode durationIcon = objectMapper.createObjectNode();
        durationIcon.put("type", "text");
        durationIcon.put("text", "⏱️");
        durationIcon.put("size", "sm");
        durationIcon.put("flex", 0);
        durationContents.add(durationIcon);

        ObjectNode durationText = objectMapper.createObjectNode();
        durationText.put("type", "text");
        durationText.put("text", String.format("服務時長 %d 分鐘", service.getDuration() != null ? service.getDuration() : 0));
        durationText.put("size", "sm");
        durationText.put("color", SECONDARY_COLOR);
        durationText.put("margin", "sm");
        durationContents.add(durationText);

        durationRow.set("contents", durationContents);
        infoContents.add(durationRow);

        // 價格
        ObjectNode priceRow = objectMapper.createObjectNode();
        priceRow.put("type", "box");
        priceRow.put("layout", "horizontal");
        priceRow.put("margin", "sm");

        ArrayNode priceContents = objectMapper.createArrayNode();

        ObjectNode priceIcon = objectMapper.createObjectNode();
        priceIcon.put("type", "text");
        priceIcon.put("text", "💰");
        priceIcon.put("size", "sm");
        priceIcon.put("flex", 0);
        priceContents.add(priceIcon);

        ObjectNode priceText = objectMapper.createObjectNode();
        priceText.put("type", "text");
        priceText.put("text", String.format("NT$ %,d", service.getPrice() != null ? service.getPrice().intValue() : 0));
        priceText.put("size", "lg");
        priceText.put("weight", "bold");
        priceText.put("color", PRIMARY_COLOR);
        priceText.put("margin", "sm");
        priceContents.add(priceText);

        priceRow.set("contents", priceContents);
        infoContents.add(priceRow);

        infoBox.set("contents", infoContents);
        bodyContents.add(infoBox);

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
                service.getDuration() != null ? service.getDuration() : 0,
                service.getPrice() != null ? service.getPrice().intValue() : 0
        );

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("✓ 選擇此服務", postbackData, PRIMARY_COLOR)
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
    // 2.5 服務分類選單
    // ========================================

    /**
     * 建構服務分類選單（Carousel）
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildCategoryMenu(String tenantId) {
        List<ServiceCategory> categories = serviceCategoryRepository.findByTenantId(tenantId, true);

        // 只顯示有可預約服務的分類
        List<String> categoryIdsWithServices = serviceItemRepository.findDistinctBookableCategoryIds(tenantId);
        List<ServiceCategory> filteredCategories = categories.stream()
                .filter(c -> categoryIdsWithServices.contains(c.getId()))
                .toList();

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        // 讀取步驟自訂配置
        String stepColor = getStepConfig(tenantId, "service", "color", "#4A90D9");
        String stepTitle = getStepConfig(tenantId, "service", "title", "📂 選擇分類");

        // 第一個 Bubble：指引說明
        bubbles.add(buildCategoryGuide(tenantId, stepColor));

        for (ServiceCategory category : filteredCategories) {
            bubbles.add(buildCategoryBubble(category, tenantId, stepColor));
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構分類選單指引
     */
    private ObjectNode buildCategoryGuide(String tenantId, String stepColor) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 使用統一步驟 Header（含 Hero 圖片）
        applyStepHeader(bubble, tenantId, "service", stepColor, "📂 選擇分類", "步驟 1/5");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode guideText = objectMapper.createObjectNode();
        guideText.put("type", "text");
        guideText.put("text", "\uD83D\uDC48 往左滑動查看所有分類\n\n點擊「選擇此分類」繼續下一步");
        guideText.put("size", "sm");
        guideText.put("color", SECONDARY_COLOR);
        guideText.put("wrap", true);
        bodyContents.add(guideText);

        // 流程說明
        ObjectNode flowBox = objectMapper.createObjectNode();
        flowBox.put("type", "box");
        flowBox.put("layout", "vertical");
        flowBox.put("backgroundColor", "#F5F5F5");
        flowBox.put("cornerRadius", "8px");
        flowBox.put("paddingAll", "10px");
        flowBox.put("margin", "md");

        ArrayNode flowContents = objectMapper.createArrayNode();

        String[] steps = {"1️⃣ 選擇分類", "2️⃣ 選擇服務", "3️⃣ 選擇日期", "4️⃣ 選擇人員", "5️⃣ 選擇時間"};
        for (String step : steps) {
            ObjectNode stepItem = objectMapper.createObjectNode();
            stepItem.put("type", "text");
            stepItem.put("text", step);
            stepItem.put("size", "xs");
            stepItem.put("color", "#666666");
            flowContents.add(stepItem);
        }

        flowBox.set("contents", flowContents);
        bodyContents.add(flowBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        bubble.set("footer", createCancelFooter());

        return bubble;
    }

    /**
     * 建構單一分類 Bubble
     */
    private ObjectNode buildCategoryBubble(ServiceCategory category, String tenantId, String stepColor) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 套用統一步驟 Header（含 Hero 圖片）
        applyStepHeader(bubble, tenantId, "service", stepColor, category.getName(), null);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 分類說明（如有）
        if (category.getDescription() != null && !category.getDescription().isEmpty()) {
            ObjectNode descText = objectMapper.createObjectNode();
            descText.put("type", "text");
            descText.put("text", category.getDescription());
            descText.put("size", "sm");
            descText.put("color", SECONDARY_COLOR);
            descText.put("wrap", true);
            bodyContents.add(descText);
        } else {
            ObjectNode placeholderText = objectMapper.createObjectNode();
            placeholderText.put("type", "text");
            placeholderText.put("text", "點擊下方按鈕查看此分類的服務");
            placeholderText.put("size", "sm");
            placeholderText.put("color", SECONDARY_COLOR);
            placeholderText.put("wrap", true);
            bodyContents.add(placeholderText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        String postbackData = String.format(
                "action=select_category&categoryId=%s&categoryName=%s",
                category.getId(),
                category.getName()
        );

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("✓ 選擇此分類", postbackData, PRIMARY_COLOR)
        ));

        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構按分類篩選的服務選單（Carousel）
     *
     * @param tenantId   租戶 ID
     * @param categoryId 分類 ID
     * @return Flex Message 內容
     */
    public JsonNode buildServiceMenuByCategory(String tenantId, String categoryId) {
        List<ServiceItem> services = serviceItemRepository
                .findBookableServicesByCategory(tenantId, categoryId);

        if (services.isEmpty()) {
            return buildNoServiceMessage();
        }

        // 讀取步驟自訂配置
        String stepColor = getStepConfig(tenantId, "service", "color", "#4A90D9");
        String stepTitle = getStepConfig(tenantId, "service", "title", "✂️ 選擇服務");

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        // 第一個 Bubble：指引說明（分類流程用 5 步版本）
        bubbles.add(buildServiceGuideWithCategory(tenantId, stepColor, stepTitle));

        for (ServiceItem service : services) {
            bubbles.add(buildServiceBubble(service, stepColor, tenantId));
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構服務選單指引（分類流程版，步驟 2/5）
     */
    private ObjectNode buildServiceGuideWithCategory(String tenantId, String stepColor, String stepTitle) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 使用統一步驟 Header（含 Hero 圖片）
        applyStepHeader(bubble, tenantId, "service", stepColor, stepTitle, "步驟 2/5");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode guideText = objectMapper.createObjectNode();
        guideText.put("type", "text");
        guideText.put("text", "\uD83D\uDC48 往左滑動查看所有服務項目\n\n點擊「選擇此服務」繼續下一步");
        guideText.put("size", "sm");
        guideText.put("color", SECONDARY_COLOR);
        guideText.put("wrap", true);
        bodyContents.add(guideText);

        // 流程說明
        ObjectNode flowBox = objectMapper.createObjectNode();
        flowBox.put("type", "box");
        flowBox.put("layout", "vertical");
        flowBox.put("backgroundColor", "#F5F5F5");
        flowBox.put("cornerRadius", "8px");
        flowBox.put("paddingAll", "10px");
        flowBox.put("margin", "md");

        ArrayNode flowContents = objectMapper.createArrayNode();

        String[] steps = {"1️⃣ 選擇分類 ✓", "2️⃣ 選擇服務", "3️⃣ 選擇日期", "4️⃣ 選擇人員", "5️⃣ 選擇時間"};
        for (String step : steps) {
            ObjectNode stepItem = objectMapper.createObjectNode();
            stepItem.put("type", "text");
            stepItem.put("text", step);
            stepItem.put("size", "xs");
            stepItem.put("color", "#666666");
            flowContents.add(stepItem);
        }

        flowBox.set("contents", flowContents);
        bodyContents.add(flowBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        bubble.set("footer", createCancelFooter());

        return bubble;
    }

    // ========================================
    // 2.6 分類＋服務合併選單
    // ========================================

    /**
     * 建構分類與服務合併選單（一步完成選擇）
     * 每個分類一張卡片，卡片內列出該分類下的所有服務供直接選擇
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildCategoryServiceMenu(String tenantId) {
        List<ServiceCategory> categories = serviceCategoryRepository.findByTenantId(tenantId, true);
        List<String> categoryIdsWithServices = serviceItemRepository.findDistinctBookableCategoryIds(tenantId);
        List<ServiceCategory> filteredCategories = categories.stream()
                .filter(c -> categoryIdsWithServices.contains(c.getId()))
                .toList();

        // 讀取步驟自訂配置
        String stepColor = getStepConfig(tenantId, "service", "color", "#4A90D9");
        String stepTitle = getStepConfig(tenantId, "service", "title", "✂️ 選擇服務");

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        // 指引 Bubble
        bubbles.add(buildServiceGuide(tenantId, stepColor, stepTitle));

        // 每個分類一張卡片
        for (ServiceCategory category : filteredCategories) {
            List<ServiceItem> services = serviceItemRepository
                    .findBookableServicesByCategory(tenantId, category.getId());
            if (!services.isEmpty()) {
                bubbles.add(buildCategoryBubbleWithServices(category, services, tenantId, stepColor));
            }
        }

        // 無分類的服務用一般方式顯示
        List<ServiceItem> allServices = serviceItemRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, ServiceStatus.ACTIVE);
        List<ServiceItem> uncategorized = allServices.stream()
                .filter(s -> s.getCategoryId() == null || s.getCategoryId().isEmpty())
                .toList();
        for (ServiceItem service : uncategorized) {
            bubbles.add(buildServiceBubble(service, stepColor, tenantId));
        }

        if (bubbles.size() <= 1) {
            return buildNoServiceMessage();
        }

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構分類卡片（包含該分類下所有服務，每個服務為一個可點擊區塊）
     */
    private ObjectNode buildCategoryBubbleWithServices(ServiceCategory category, List<ServiceItem> services,
                                                        String tenantId, String stepColor) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // 套用統一步驟 Header（含 Hero 圖片）
        applyStepHeader(bubble, tenantId, "service", stepColor, "📂 " + category.getName(), null);

        // Body - 每個服務一個可點擊的 box
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "12px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        for (int i = 0; i < services.size(); i++) {
            ServiceItem service = services.get(i);

            // 分隔線（第二個服務起）
            if (i > 0) {
                ObjectNode separator = objectMapper.createObjectNode();
                separator.put("type", "separator");
                separator.put("margin", "sm");
                bodyContents.add(separator);
            }

            // 可點擊的服務區塊
            String postbackData = String.format(
                    "action=select_service&serviceId=%s&serviceName=%s&duration=%d&price=%d",
                    service.getId(),
                    service.getName(),
                    service.getDuration() != null ? service.getDuration() : 0,
                    service.getPrice() != null ? service.getPrice().intValue() : 0
            );

            ObjectNode serviceBox = objectMapper.createObjectNode();
            serviceBox.put("type", "box");
            serviceBox.put("layout", "vertical");
            serviceBox.put("paddingAll", "10px");
            serviceBox.put("cornerRadius", "8px");
            serviceBox.put("backgroundColor", "#F8F8F8");
            if (i > 0) serviceBox.put("margin", "sm");

            // 設定整個 box 可點擊
            ObjectNode boxAction = objectMapper.createObjectNode();
            boxAction.put("type", "postback");
            boxAction.put("label", service.getName());
            boxAction.put("data", postbackData);
            serviceBox.set("action", boxAction);

            ArrayNode serviceContents = objectMapper.createArrayNode();

            // 服務名稱
            ObjectNode nameText = objectMapper.createObjectNode();
            nameText.put("type", "text");
            nameText.put("text", service.getName());
            nameText.put("size", "sm");
            nameText.put("weight", "bold");
            nameText.put("color", "#333333");
            nameText.put("wrap", true);
            serviceContents.add(nameText);

            // 時長 + 價格
            ObjectNode infoRow = objectMapper.createObjectNode();
            infoRow.put("type", "box");
            infoRow.put("layout", "horizontal");
            infoRow.put("margin", "xs");

            ArrayNode infoContents = objectMapper.createArrayNode();

            ObjectNode durationText = objectMapper.createObjectNode();
            durationText.put("type", "text");
            durationText.put("text", String.format("⏱ %d分鐘", service.getDuration() != null ? service.getDuration() : 0));
            durationText.put("size", "xxs");
            durationText.put("color", SECONDARY_COLOR);
            durationText.put("flex", 1);
            infoContents.add(durationText);

            ObjectNode priceText = objectMapper.createObjectNode();
            priceText.put("type", "text");
            priceText.put("text", String.format("NT$%,d", service.getPrice() != null ? service.getPrice().intValue() : 0));
            priceText.put("size", "sm");
            priceText.put("weight", "bold");
            priceText.put("color", PRIMARY_COLOR);
            priceText.put("flex", 0);
            infoContents.add(priceText);

            infoRow.set("contents", infoContents);
            serviceContents.add(infoRow);

            serviceBox.set("contents", serviceContents);
            bodyContents.add(serviceBox);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 取消按鈕
        bubble.set("footer", createCancelFooter());

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
        // 目前設計為所有員工都可以提供所有服務
        // 未來如需實作服務與員工的關聯，可在此處根據 serviceId 篩選
        List<Staff> staffList = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        // 讀取步驟自訂配置
        String staffStepColor = getStepConfig(tenantId, "staff", "color", "#4A90D9");
        String staffStepTitle = getStepConfig(tenantId, "staff", "title", "👤 選擇服務人員");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "staff", staffStepColor, staffStepTitle, "步驟 2/4");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 提示文字
        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "可選擇指定服務人員，或由系統自動安排");
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("wrap", true);
        tipText.put("margin", "none");
        bodyContents.add(tipText);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "md");
        bodyContents.add(separator);

        // 不指定選項（推薦）
        bodyContents.add(createStaffButton("🎲 不指定（推薦）", "系統自動安排最佳人員", null));

        // 員工列表
        for (Staff staff : staffList) {
            String bio = staff.getBio() != null && !staff.getBio().isEmpty()
                    ? staff.getBio()
                    : "專業服務人員";
            bodyContents.add(createStaffButton(
                    staff.getName(),
                    bio,
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
     * 建構員工選單（根據日期篩選可用員工）
     *
     * <p>只顯示在指定日期有上班且未請假的員工
     *
     * @param tenantId  租戶 ID
     * @param serviceId 服務 ID
     * @param date      預約日期
     * @return Flex Message 內容
     */
    public JsonNode buildStaffMenuByDate(String tenantId, String serviceId, LocalDate date, Integer duration) {
        // 如果日期為 null，使用今天
        if (date == null) {
            date = LocalDate.now();
            log.warn("buildStaffMenuByDate 收到 null date，使用今天");
        }

        // 取得所有活躍員工
        List<Staff> allStaff = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        // 篩選在該日期有上班的員工
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;  // 轉換為 0=週日
        List<Staff> availableStaff = new java.util.ArrayList<>();

        for (Staff staff : allStaff) {
            // 檢查排班
            Optional<StaffSchedule> scheduleOpt = staffScheduleRepository
                    .findByStaffIdAndDayOfWeek(staff.getId(), tenantId, dayOfWeek);

            // 如果沒有排班設定，預設為可上班；如果有設定，檢查是否為工作日
            boolean isWorkingDay = scheduleOpt.isEmpty() ||
                    Boolean.TRUE.equals(scheduleOpt.get().getIsWorkingDay());

            if (isWorkingDay) {
                // 檢查請假
                boolean onLeave = staffLeaveRepository
                        .findByStaffIdAndLeaveDateAndDeletedAtIsNull(staff.getId(), date)
                        .isPresent();

                if (!onLeave) {
                    availableStaff.add(staff);
                }
            }
        }

        // 檢查每位員工的可預約時段數量
        Map<String, Integer> staffSlotCounts = new java.util.LinkedHashMap<>();
        int totalStaffWithSlots = 0;
        for (Staff staff : availableStaff) {
            int slotCount = getAvailableSlotCount(tenantId, staff.getId(), date, duration);
            staffSlotCounts.put(staff.getId(), slotCount);
            if (slotCount > 0) {
                totalStaffWithSlots++;
            }
        }

        // 讀取步驟自訂配置
        String staffStepColor = getStepConfig(tenantId, "staff", "color", "#4A90D9");
        String staffStepTitle = getStepConfig(tenantId, "staff", "title", "👤 選擇服務人員");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "staff", staffStepColor, staffStepTitle, "步驟 3/4");

        // 在 header 中追加日期提示
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("M/d（E）", java.util.Locale.TAIWAN);
        ObjectNode dateHint = objectMapper.createObjectNode();
        dateHint.put("type", "text");
        dateHint.put("text", "📅 " + date.format(formatter));
        dateHint.put("size", "sm");
        dateHint.put("color", "#FFFFFF");
        dateHint.put("align", "center");
        dateHint.put("margin", "sm");
        ((ArrayNode) bubble.get("header").get("contents")).add(dateHint);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        if (availableStaff.isEmpty() || totalStaffWithSlots == 0) {
            // 沒有可用員工或全部員工都沒有可預約時段
            ObjectNode noStaffText = objectMapper.createObjectNode();
            noStaffText.put("type", "text");
            noStaffText.put("text", "此日期沒有可預約的時段");
            noStaffText.put("size", "sm");
            noStaffText.put("color", SECONDARY_COLOR);
            noStaffText.put("wrap", true);
            noStaffText.put("align", "center");
            bodyContents.add(noStaffText);

            ObjectNode tipText = objectMapper.createObjectNode();
            tipText.put("type", "text");
            tipText.put("text", "請返回選擇其他日期");
            tipText.put("size", "xs");
            tipText.put("color", SECONDARY_COLOR);
            tipText.put("wrap", true);
            tipText.put("align", "center");
            tipText.put("margin", "md");
            bodyContents.add(tipText);
        } else {
            // 提示文字
            ObjectNode tipText = objectMapper.createObjectNode();
            tipText.put("type", "text");
            tipText.put("text", "以下為此日期可預約的服務人員");
            tipText.put("size", "xs");
            tipText.put("color", SECONDARY_COLOR);
            tipText.put("wrap", true);
            tipText.put("margin", "none");
            bodyContents.add(tipText);

            // 分隔線
            ObjectNode separator = objectMapper.createObjectNode();
            separator.put("type", "separator");
            separator.put("margin", "md");
            bodyContents.add(separator);

            // 不指定選項（推薦）- 只在有可用員工時顯示
            bodyContents.add(createStaffButton("🎲 不指定（推薦）", "系統自動安排最佳人員", null));

            // 可用員工列表（區分有無可預約時段）
            for (Staff staff : availableStaff) {
                int slotCount = staffSlotCounts.getOrDefault(staff.getId(), 0);
                if (slotCount > 0) {
                    // 有可預約時段 - 可點擊
                    String bio = staff.getBio() != null && !staff.getBio().isEmpty()
                            ? staff.getBio() + "（" + slotCount + " 個時段）"
                            : "可預約 " + slotCount + " 個時段";
                    bodyContents.add(createStaffButton(
                            staff.getName(),
                            bio,
                            staff.getId()
                    ));
                } else {
                    // 無可預約時段 - 不可點擊，灰色顯示
                    bodyContents.add(createDisabledStaffRow(staff.getName(), "今日無可預約時段"));
                }
            }
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

    /**
     * 建構不可點擊的員工列（無可預約時段）
     */
    private ObjectNode createDisabledStaffRow(String name, String reason) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "horizontal");
        box.put("spacing", "md");
        box.put("paddingAll", "10px");
        box.put("borderWidth", "1px");
        box.put("borderColor", "#E0E0E0");
        box.put("cornerRadius", "8px");
        box.put("backgroundColor", "#F5F5F5");

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
        nameText.put("color", "#BDBDBD");
        infoContents.add(nameText);

        ObjectNode reasonText = objectMapper.createObjectNode();
        reasonText.put("type", "text");
        reasonText.put("text", reason);
        reasonText.put("size", "xs");
        reasonText.put("color", "#BDBDBD");
        infoContents.add(reasonText);

        infoBox.set("contents", infoContents);
        contents.add(infoBox);

        box.set("contents", contents);

        // 不設定 action，讓此區塊不可點擊

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
    public JsonNode buildDateMenu(String tenantId, Integer duration) {
        // 取得店家設定
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        Tenant tenant = tenantOpt.orElse(null);
        int maxAdvanceDays = tenant != null ? (tenant.getMaxAdvanceBookingDays() != null ? tenant.getMaxAdvanceBookingDays() : 30) : 30;
        List<Integer> closedDays = parseClosedDays(tenant != null ? tenant.getClosedDays() : null);

        // 預先取得店家營業設定
        LocalTime businessStart = tenant != null && tenant.getBusinessStartTime() != null
                ? tenant.getBusinessStartTime() : LocalTime.of(9, 0);
        LocalTime businessEnd = tenant != null && tenant.getBusinessEndTime() != null
                ? tenant.getBusinessEndTime() : LocalTime.of(18, 0);
        int interval = tenant != null && tenant.getBookingInterval() != null
                ? tenant.getBookingInterval() : 30;
        LocalTime tenantBreakStart = tenant != null ? tenant.getBreakStartTime() : null;
        LocalTime tenantBreakEnd = tenant != null ? tenant.getBreakEndTime() : null;
        int serviceDuration = duration != null ? duration : 60;

        // 預先取得所有活躍員工和排班（含工作時段，減少 DB 查詢次數）
        List<Staff> allActiveStaff = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        // 建立排班快取：staffId → dayOfWeek → StaffSchedule（含工時）
        Map<String, Map<Integer, StaffSchedule>> scheduleCache = new java.util.HashMap<>();
        for (Staff staff : allActiveStaff) {
            Map<Integer, StaffSchedule> staffScheduleMap = new java.util.HashMap<>();
            for (int dow = 0; dow <= 6; dow++) {
                Optional<StaffSchedule> scheduleOpt = staffScheduleRepository
                        .findByStaffIdAndDayOfWeek(staff.getId(), tenantId, dow);
                scheduleOpt.ifPresent(s -> staffScheduleMap.put(s.getDayOfWeek(), s));
            }
            scheduleCache.put(staff.getId(), staffScheduleMap);
        }

        LocalDate today = LocalDate.now();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("M/d (E)", java.util.Locale.TAIWAN);
        DateTimeFormatter dataFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        // 批次取得日期範圍內所有 CONFIRMED 預約（一次查詢，避免逐日逐時段查 DB）
        LocalDate endDate = today.plusDays(maxAdvanceDays + 30);
        List<Booking> confirmedBookings = bookingRepository.findConfirmedBookingsInDateRange(tenantId, today, endDate);

        // 建立快取：staffId → date → List<Booking>
        Map<String, Map<LocalDate, List<Booking>>> bookingCache = new java.util.HashMap<>();
        for (Booking b : confirmedBookings) {
            bookingCache
                    .computeIfAbsent(b.getStaffId(), k -> new java.util.HashMap<>())
                    .computeIfAbsent(b.getBookingDate(), k -> new java.util.ArrayList<>())
                    .add(b);
        }

        // 批次取得所有員工的請假資料
        Map<String, java.util.Set<LocalDate>> leaveCache = new java.util.HashMap<>();
        for (Staff staff : allActiveStaff) {
            var leaves = staffLeaveRepository.findByStaffIdAndDateRange(
                    staff.getId(), today, endDate);
            if (leaves != null && !leaves.isEmpty()) {
                java.util.Set<LocalDate> leaveDates = new java.util.HashSet<>();
                for (var leave : leaves) {
                    leaveDates.add(leave.getLeaveDate());
                }
                leaveCache.put(staff.getId(), leaveDates);
            }
        }

        // 收集所有可用日期（實際檢查是否有可預約時段）
        List<LocalDate> availableDates = new java.util.ArrayList<>();
        int dayOffset = 0;

        while (availableDates.size() < maxAdvanceDays && dayOffset < maxAdvanceDays + 30) {
            LocalDate date = today.plusDays(dayOffset);
            int dayOfWeek = date.getDayOfWeek().getValue() % 7;

            if (!closedDays.contains(dayOfWeek)) {
                // 逐一檢查員工，只要有任何一位員工的任何一個時段可用即可
                boolean dateHasSlot = false;

                for (Staff staff : allActiveStaff) {
                    // 檢查排班：是否上班
                    StaffSchedule schedule = scheduleCache
                            .getOrDefault(staff.getId(), java.util.Collections.emptyMap())
                            .get(dayOfWeek);
                    boolean isWorking = (schedule == null) || Boolean.TRUE.equals(schedule.getIsWorkingDay());
                    if (!isWorking) continue;

                    // 檢查請假（使用快取）
                    java.util.Set<LocalDate> staffLeaves = leaveCache.get(staff.getId());
                    if (staffLeaves != null && staffLeaves.contains(date)) continue;

                    // 計算此員工在此日的有效工作時段（取店家營業時間與員工排班的交集）
                    LocalTime effStart = businessStart;
                    LocalTime effEnd = businessEnd;
                    if (schedule != null && schedule.getStartTime() != null) {
                        effStart = effStart.isBefore(schedule.getStartTime()) ? schedule.getStartTime() : effStart;
                    }
                    if (schedule != null && schedule.getEndTime() != null) {
                        effEnd = effEnd.isAfter(schedule.getEndTime()) ? schedule.getEndTime() : effEnd;
                    }

                    // 今天額外檢查：跳過已過去的時段
                    if (date.equals(today)) {
                        LocalTime minTime = LocalTime.now().plusMinutes(30);
                        if (!minTime.isBefore(effEnd)) continue;
                        if (minTime.isAfter(effStart)) {
                            int minutesPast = (minTime.getHour() * 60 + minTime.getMinute())
                                    - (effStart.getHour() * 60 + effStart.getMinute());
                            int skip = (int) Math.ceil((double) minutesPast / interval);
                            effStart = effStart.plusMinutes((long) skip * interval);
                        }
                    }

                    // 取得員工休息時間
                    LocalTime staffBreakStart = schedule != null ? schedule.getBreakStartTime() : null;
                    LocalTime staffBreakEnd = schedule != null ? schedule.getBreakEndTime() : null;

                    // 取得此員工此日的已確認預約（從快取）
                    List<Booking> staffDateBookings = bookingCache
                            .getOrDefault(staff.getId(), java.util.Collections.emptyMap())
                            .getOrDefault(date, java.util.Collections.emptyList());

                    // 遍歷時段，找到至少一個可用時段即可
                    LocalTime current = effStart;
                    while (!current.plusMinutes(serviceDuration).isAfter(effEnd)) {
                        boolean slotOk = true;

                        // 店家休息時間
                        if (tenantBreakStart != null && tenantBreakEnd != null) {
                            if (isTimeOverlapping(current, current.plusMinutes(serviceDuration), tenantBreakStart, tenantBreakEnd)) {
                                slotOk = false;
                            }
                        }
                        // 員工休息時間
                        if (slotOk && staffBreakStart != null && staffBreakEnd != null) {
                            if (isTimeOverlapping(current, current.plusMinutes(serviceDuration), staffBreakStart, staffBreakEnd)) {
                                slotOk = false;
                            }
                        }
                        // 預約衝突（記憶體內比對，無 DB 查詢）
                        if (slotOk) {
                            LocalTime slotEnd = current.plusMinutes(serviceDuration);
                            for (Booking b : staffDateBookings) {
                                if (b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(current)) {
                                    slotOk = false;
                                    break;
                                }
                            }
                        }

                        if (slotOk) {
                            dateHasSlot = true;
                            break;
                        }
                        current = current.plusMinutes(interval);
                    }

                    if (dateHasSlot) break;
                }

                if (dateHasSlot) {
                    availableDates.add(date);
                }
            }
            dayOffset++;
        }

        // 如果沒有任何可預約日期，顯示提示訊息
        if (availableDates.isEmpty()) {
            return buildNoAvailableDateBubble();
        }

        // 讀取步驟自訂配置
        String dateStepColor = getStepConfig(tenantId, "date", "color", PRIMARY_COLOR);
        String dateStepTitle = getStepConfig(tenantId, "date", "title", "📅 選擇日期");

        // 如果日期少於等於 10 個，使用單一 Bubble
        if (availableDates.size() <= 10) {
            return buildSingleDateBubble(tenantId, availableDates, today, displayFormatter, dataFormatter, dateStepColor, dateStepTitle);
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

            // 所有 Bubble 統一套用步驟 Header（含 Hero 圖片）
            String bubbleTitle = (bubbleIndex == 0) ? dateStepTitle : "📅 更多日期";
            applyStepHeader(bubble, tenantId, "date", dateStepColor, bubbleTitle, null);

            // 在 header 中追加日期範圍
            if (!bubbleDates.isEmpty()) {
                LocalDate firstDate = bubbleDates.get(0);
                LocalDate lastDate = bubbleDates.get(bubbleDates.size() - 1);
                ObjectNode rangeText = objectMapper.createObjectNode();
                rangeText.put("type", "text");
                rangeText.put("text", firstDate.format(DateTimeFormatter.ofPattern("M/d")) + " - " + lastDate.format(DateTimeFormatter.ofPattern("M/d")));
                rangeText.put("size", "xs");
                rangeText.put("color", "#FFFFFF");
                rangeText.put("align", "center");
                ((ArrayNode) bubble.get("header").get("contents")).add(rangeText);
            }

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
    private JsonNode buildSingleDateBubble(String tenantId, List<LocalDate> dates, LocalDate today,
                                           DateTimeFormatter displayFormatter, DateTimeFormatter dataFormatter,
                                           String stepColor, String stepTitle) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "date", stepColor, stepTitle, null);

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
     * 建構無可預約日期提示 Bubble
     */
    private JsonNode buildNoAvailableDateBubble() {
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
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode noDateText = objectMapper.createObjectNode();
        noDateText.put("type", "text");
        noDateText.put("text", "目前沒有可預約的日期");
        noDateText.put("size", "md");
        noDateText.put("weight", "bold");
        noDateText.put("color", SECONDARY_COLOR);
        noDateText.put("align", "center");
        noDateText.put("wrap", true);
        bodyContents.add(noDateText);

        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "所有日期的時段都已額滿，請稍後再試或聯繫店家");
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("align", "center");
        tipText.put("wrap", true);
        tipText.put("margin", "md");
        bodyContents.add(tipText);

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

        // 讀取步驟自訂配置
        String timeStepColor = getStepConfig(tenantId, "time", "color", "#4A90D9");
        String timeStepTitle = getStepConfig(tenantId, "time", "title", "⏰ 選擇時段");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "time", timeStepColor, timeStepTitle, "步驟 4/4 - 最後一步！");

        // 在 header 中追加日期提示
        ObjectNode dateText = objectMapper.createObjectNode();
        dateText.put("type", "text");
        dateText.put("text", "📅 " + date.format(DateTimeFormatter.ofPattern("M月d日 (E)", java.util.Locale.TAIWAN)));
        dateText.put("size", "sm");
        dateText.put("color", "#FFFFFF");
        dateText.put("align", "center");
        dateText.put("margin", "sm");
        ((ArrayNode) bubble.get("header").get("contents")).add(dateText);

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

        // 防護：日期為 null 時返回空
        if (date == null) {
            log.warn("generateAvailableSlots 收到 null date，返回空時段");
            return slots;
        }

        // 取得店家設定
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        if (tenantOpt.isEmpty()) {
            return slots;
        }

        Tenant tenant = tenantOpt.get();
        LocalTime businessStart = tenant.getBusinessStartTime() != null ? tenant.getBusinessStartTime() : LocalTime.of(9, 0);
        LocalTime businessEnd = tenant.getBusinessEndTime() != null ? tenant.getBusinessEndTime() : LocalTime.of(18, 0);
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

            // 檢查是否有衝突預約
            if (isAvailable && staffId != null && !staffId.isEmpty()) {
                // 指定員工：檢查該員工是否有衝突
                boolean hasConflict = bookingRepository.existsConflictingBooking(
                        tenantId, staffId, date, current, current.plusMinutes(serviceDuration)
                );
                if (hasConflict) {
                    isAvailable = false;
                }
            } else if (isAvailable) {
                // 不指定員工：檢查是否至少有一位員工可用
                List<Staff> availableStaffForSlot = getAvailableStaffForDate(tenantId, date);
                boolean anyStaffFree = false;
                for (Staff s : availableStaffForSlot) {
                    boolean conflict = bookingRepository.existsConflictingBooking(
                            tenantId, s.getId(), date, current, current.plusMinutes(serviceDuration)
                    );
                    if (!conflict) {
                        anyStaffFree = true;
                        break;
                    }
                }
                if (!anyStaffFree) {
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
     * 取得指定員工在指定日期的可預約時段數量
     *
     * @param tenantId 租戶 ID
     * @param staffId  員工 ID
     * @param date     日期
     * @param duration 服務時長（分鐘）
     * @return 可預約時段數量
     */
    private int getAvailableSlotCount(String tenantId, String staffId, LocalDate date, Integer duration) {
        List<LocalTime> slots = generateAvailableSlots(tenantId, staffId, date, duration);
        return slots.size();
    }

    /**
     * 取得指定日期的可用員工（活躍 + 有上班 + 沒請假）
     */
    private List<Staff> getAvailableStaffForDate(String tenantId, LocalDate date) {
        List<Staff> allStaff = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        List<Staff> available = new ArrayList<>();

        for (Staff staff : allStaff) {
            Optional<StaffSchedule> scheduleOpt = staffScheduleRepository
                    .findByStaffIdAndDayOfWeek(staff.getId(), tenantId, dayOfWeek);

            boolean isWorkingDay = scheduleOpt.isEmpty() ||
                    Boolean.TRUE.equals(scheduleOpt.get().getIsWorkingDay());

            if (isWorkingDay) {
                boolean onLeave = staffLeaveRepository
                        .findByStaffIdAndLeaveDateAndDeletedAtIsNull(staff.getId(), date)
                        .isPresent();
                if (!onLeave) {
                    available.add(staff);
                }
            }
        }

        return available;
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
    public JsonNode buildBookingConfirmation(String tenantId, ConversationContext context) {
        // 讀取步驟自訂配置
        String confirmColor = getStepConfig(tenantId, "confirm", "color", PRIMARY_COLOR);
        String confirmTitle = getStepConfig(tenantId, "confirm", "title", "請確認預約資訊");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "confirm", confirmColor, confirmTitle, null);

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

        // 備註
        if (context.getCustomerNote() != null && !context.getCustomerNote().isEmpty()) {
            bodyContents.add(createInfoRow("備註", context.getCustomerNote()));
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
    public JsonNode buildBookingSuccess(String tenantId, ConversationContext context, String bookingNo) {
        // 讀取步驟自訂配置
        String successColor = getStepConfig(tenantId, "success", "color", PRIMARY_COLOR);

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", successColor);
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

    // ========================================
    // 8. 備註輸入提示
    // ========================================

    /**
     * 建構備註輸入提示訊息
     *
     * @return Flex Message 內容
     */
    public JsonNode buildNoteInputPrompt(String tenantId) {
        // 讀取步驟自訂配置
        String noteColor = getStepConfig(tenantId, "note", "color", "#5C6BC0");
        String noteTitle = getStepConfig(tenantId, "note", "title", "是否需要備註？");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // 使用統一的步驟 Header 建構
        applyStepHeader(bubble, tenantId, "note", noteColor, noteTitle, null);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 重點說明 - 告訴用戶在哪裡輸入
        ObjectNode inputHint = objectMapper.createObjectNode();
        inputHint.put("type", "box");
        inputHint.put("layout", "vertical");
        inputHint.put("backgroundColor", "#E3F2FD");
        inputHint.put("cornerRadius", "8px");
        inputHint.put("paddingAll", "12px");

        ArrayNode inputHintContents = objectMapper.createArrayNode();

        ObjectNode inputIcon = objectMapper.createObjectNode();
        inputIcon.put("type", "text");
        inputIcon.put("text", "⌨️ 請在下方聊天輸入框輸入");
        inputIcon.put("size", "sm");
        inputIcon.put("weight", "bold");
        inputIcon.put("color", "#1565C0");
        inputIcon.put("align", "center");
        inputHintContents.add(inputIcon);

        ObjectNode inputDesc = objectMapper.createObjectNode();
        inputDesc.put("type", "text");
        inputDesc.put("text", "直接打字輸入備註內容即可");
        inputDesc.put("size", "xs");
        inputDesc.put("color", "#1976D2");
        inputDesc.put("align", "center");
        inputDesc.put("margin", "sm");
        inputHintContents.add(inputDesc);

        inputHint.set("contents", inputHintContents);
        bodyContents.add(inputHint);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "md");
        bodyContents.add(separator);

        // 提示範例
        ObjectNode tipBox = objectMapper.createObjectNode();
        tipBox.put("type", "box");
        tipBox.put("layout", "vertical");
        tipBox.put("backgroundColor", "#F5F5F5");
        tipBox.put("cornerRadius", "8px");
        tipBox.put("paddingAll", "12px");
        tipBox.put("margin", "md");

        ArrayNode tipContents = objectMapper.createArrayNode();

        ObjectNode tipTitle = objectMapper.createObjectNode();
        tipTitle.put("type", "text");
        tipTitle.put("text", "💡 備註範例：");
        tipTitle.put("size", "xs");
        tipTitle.put("color", SECONDARY_COLOR);
        tipContents.add(tipTitle);

        ObjectNode tipExample = objectMapper.createObjectNode();
        tipExample.put("type", "text");
        tipExample.put("text", "「希望靠窗座位」\n「有過敏體質」\n「第一次來」");
        tipExample.put("size", "xs");
        tipExample.put("color", SECONDARY_COLOR);
        tipExample.put("wrap", true);
        tipExample.put("margin", "sm");
        tipContents.add(tipExample);

        tipBox.set("contents", tipContents);
        bodyContents.add(tipBox);

        // 無備註說明
        ObjectNode noNoteText = objectMapper.createObjectNode();
        noNoteText.put("type", "text");
        noNoteText.put("text", "沒有特殊需求？點選「跳過」即可");
        noNoteText.put("size", "xs");
        noNoteText.put("color", "#9E9E9E");
        noNoteText.put("align", "center");
        noNoteText.put("margin", "md");
        bodyContents.add(noNoteText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 跳過按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("↩ 返回", "action=go_back", SECONDARY_COLOR));
        footerContents.add(createButton("跳過 →", "action=skip_note", PRIMARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 9. 預約列表
    // ========================================

    /**
     * 建構預約列表訊息
     *
     * @param bookings 預約列表
     * @param tenantId 租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildBookingList(List<Booking> bookings, String tenantId) {
        if (bookings == null || bookings.isEmpty()) {
            // 無預約時顯示空狀態
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            // 套用自訂 Header
            if (tenantId != null) {
                applyFunctionHeader(bubble, tenantId, "bookingList", "#0066CC", "📋", "我的預約");
            }

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
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("查看我的預約", "action=view_bookings", LINK_COLOR));
        footerContents.add(createButton("返回主選單", "action=main_menu", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 聯絡店家
    // ========================================

    /**
     * 建構聯絡店家訊息
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildContactShopMessage(String tenantId) {
        // 取得店家資訊
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        String shopName = tenantOpt.map(Tenant::getName).orElse("店家");
        String phone = tenantOpt.map(Tenant::getPhone).orElse(null);
        String address = tenantOpt.map(Tenant::getAddress).orElse(null);
        String email = tenantOpt.map(Tenant::getEmail).orElse(null);
        String description = tenantOpt.map(Tenant::getDescription).orElse(null);

        // 營業時間
        String businessHours = tenantOpt.map(t -> {
            if (t.getBusinessStartTime() != null && t.getBusinessEndTime() != null) {
                return t.getBusinessStartTime().toString() + " - " + t.getBusinessEndTime().toString();
            }
            return null;
        }).orElse(null);

        // 公休日
        String closedDaysStr = tenantOpt.map(t -> {
            String closedDays = t.getClosedDays();
            if (closedDays == null || closedDays.isEmpty()) {
                return null;
            }
            // 解析 JSON 格式的公休日 [0,6] -> "週日、週六"
            try {
                String[] dayNames = {"週日", "週一", "週二", "週三", "週四", "週五", "週六"};
                java.util.List<String> closedList = new java.util.ArrayList<>();
                String cleanedDays = closedDays.replace("[", "").replace("]", "").trim();
                if (!cleanedDays.isEmpty()) {
                    for (String day : cleanedDays.split(",")) {
                        int dayNum = Integer.parseInt(day.trim());
                        if (dayNum >= 0 && dayNum < 7) {
                            closedList.add(dayNames[dayNum]);
                        }
                    }
                }
                return closedList.isEmpty() ? null : String.join("、", closedList);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);

        // 讀取自訂配色
        String contactColor = getFunctionConfig(tenantId, "contactShop", "color", "#5C6BC0");
        String contactIcon = getFunctionConfig(tenantId, "contactShop", "icon", "📞");
        String contactTitle = getFunctionConfig(tenantId, "contactShop", "title", "");
        String heroImageUrl = getFunctionConfig(tenantId, "contactShop", "imageUrl", "");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Hero 圖片
        if (!heroImageUrl.isEmpty()) {
            if (heroImageUrl.startsWith("/api/public/")) {
                heroImageUrl = appBaseUrl + heroImageUrl;
            }
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", heroImageUrl);
            hero.put("size", "full");
            hero.put("aspectRatio", "20:8");
            hero.put("aspectMode", "cover");
            bubble.set("hero", hero);
        }

        // Header
        String headerTitle = contactTitle.isEmpty() ? "聯絡 " + shopName : contactTitle;
        if (!contactIcon.isEmpty() && !headerTitle.startsWith(contactIcon)) {
            headerTitle = contactIcon + " " + headerTitle;
        }

        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", contactColor);
        header.put("paddingAll", "15px");

        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "text");
        headerText.put("text", headerTitle);
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

        // 店家介紹
        if (description != null && !description.isEmpty()) {
            ObjectNode descBox = objectMapper.createObjectNode();
            descBox.put("type", "box");
            descBox.put("layout", "vertical");
            descBox.put("paddingAll", "12px");
            descBox.put("backgroundColor", "#F8F9FA");
            descBox.put("cornerRadius", "8px");

            ObjectNode descText = objectMapper.createObjectNode();
            descText.put("type", "text");
            descText.put("text", description);
            descText.put("size", "sm");
            descText.put("color", "#333333");
            descText.put("wrap", true);

            descBox.set("contents", objectMapper.createArrayNode().add(descText));
            bodyContents.add(descBox);

            // 分隔線
            ObjectNode separator = objectMapper.createObjectNode();
            separator.put("type", "separator");
            separator.put("margin", "lg");
            bodyContents.add(separator);
        }

        // 電話
        if (phone != null && !phone.isEmpty()) {
            bodyContents.add(createContactRow("\uD83D\uDCDE", "電話", phone));
        }

        // 地址
        if (address != null && !address.isEmpty()) {
            bodyContents.add(createContactRow("\uD83D\uDCCD", "地址", address));
        }

        // 信箱
        if (email != null && !email.isEmpty()) {
            bodyContents.add(createContactRow("\u2709", "信箱", email));
        }

        // 營業時間
        if (businessHours != null && !businessHours.isEmpty()) {
            bodyContents.add(createContactRow("🕐", "營業時間", businessHours));
        }

        // 公休日
        if (closedDaysStr != null && !closedDaysStr.isEmpty()) {
            bodyContents.add(createContactRow("📅", "公休日", closedDaysStr));
        }

        // 如果沒有任何聯絡資訊且沒有介紹
        if (bodyContents.isEmpty() || (bodyContents.size() == 2 && description != null)) {
            ObjectNode noInfoText = objectMapper.createObjectNode();
            noInfoText.put("type", "text");
            noInfoText.put("text", "店家尚未設定聯絡資訊，請透過 LINE 訊息聯繫。");
            noInfoText.put("size", "sm");
            noInfoText.put("color", SECONDARY_COLOR);
            noInfoText.put("wrap", true);
            bodyContents.add(noInfoText);
        }

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 返回主選單
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("返回主選單", "action=main_menu", SECONDARY_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構聯絡資訊行
     */
    private ObjectNode createContactRow(String icon, String label, String value) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("type", "box");
        row.put("layout", "horizontal");
        row.put("spacing", "md");
        row.put("margin", "md");

        ArrayNode contents = objectMapper.createArrayNode();

        // 圖示
        ObjectNode iconText = objectMapper.createObjectNode();
        iconText.put("type", "text");
        iconText.put("text", icon);
        iconText.put("size", "lg");
        iconText.put("flex", 0);
        contents.add(iconText);

        // 內容區塊
        ObjectNode contentBox = objectMapper.createObjectNode();
        contentBox.put("type", "box");
        contentBox.put("layout", "vertical");
        contentBox.put("flex", 1);

        ArrayNode contentBoxContents = objectMapper.createArrayNode();

        ObjectNode labelText = objectMapper.createObjectNode();
        labelText.put("type", "text");
        labelText.put("text", label);
        labelText.put("size", "xs");
        labelText.put("color", SECONDARY_COLOR);
        contentBoxContents.add(labelText);

        ObjectNode valueText = objectMapper.createObjectNode();
        valueText.put("type", "text");
        valueText.put("text", value);
        valueText.put("size", "sm");
        valueText.put("wrap", true);
        contentBoxContents.add(valueText);

        contentBox.set("contents", contentBoxContents);
        contents.add(contentBox);

        row.set("contents", contents);
        return row;
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
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("↩ 返回上一步", "action=go_back", SECONDARY_COLOR));
        footerContents.add(createButton("✕ 取消預約", "action=cancel_flow", "#E74C3C"));

        footer.set("contents", footerContents);
        return footer;
    }

    /**
     * 建構取消 Footer（用於第一步）
     */
    private ObjectNode createCancelFooter() {
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("✕ 取消預約", "action=cancel_flow", SECONDARY_COLOR)
        ));

        return footer;
    }

    /**
     * 建構 Carousel 導航 Bubble（用於長列表末端的返回按鈕）
     *
     * @return 導航 Bubble 節點
     */
    private ObjectNode buildCarouselNavigationBubble() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // Body - 導航提示
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");
        body.put("justifyContent", "center");
        body.put("alignItems", "center");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 圖示
        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", "🏠");
        icon.put("size", "3xl");
        icon.put("align", "center");
        bodyContents.add(icon);

        // 提示文字
        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "需要其他服務嗎？");
        tipText.put("size", "sm");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("align", "center");
        tipText.put("margin", "md");
        bodyContents.add(tipText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 返回主選單按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("返回主選單", "action=main_menu", SECONDARY_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 9. 預約列表（含取消按鈕）
    // ========================================

    /**
     * 建構預約列表訊息（含取消按鈕）
     *
     * @param bookings 預約列表
     * @param tenantId 租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildBookingListWithCancel(List<Booking> bookings, String tenantId) {
        if (bookings == null || bookings.isEmpty()) {
            return buildBookingList(bookings, tenantId);
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

            // 狀態描述文字
            ObjectNode statusDesc = objectMapper.createObjectNode();
            statusDesc.put("type", "text");
            if (BookingStatus.PENDING.equals(booking.getStatus())) {
                statusDesc.put("text", "⏳ 等待店家確認中");
                statusDesc.put("color", "#FFA500");
            } else {
                statusDesc.put("text", "✓ 預約已確認");
                statusDesc.put("color", "#4CAF50");
            }
            statusDesc.put("size", "sm");
            statusDesc.put("weight", "bold");
            statusDesc.put("margin", "md");
            bodyContents.add(statusDesc);

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

        // 末端導航卡片 — 返回主選單
        bubbles.add(buildCarouselNavigationBubble());

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
     * @param tenantId 租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildAvailableCouponList(List<Coupon> coupons, String tenantId) {
        // 讀取自訂配色
        String couponColor = tenantId != null ? getFunctionConfig(tenantId, "couponList", "color", "#E91E63") : "#E91E63";

        if (coupons == null || coupons.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            // 套用自訂 Header
            if (tenantId != null) {
                applyFunctionHeader(bubble, tenantId, "couponList", "#E91E63", "🎁", "領取票券");
            }

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

            // Header（使用自訂配色）
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", couponColor);
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

        // 末端添加導航 Bubble
        bubbles.add(buildCarouselNavigationBubble());

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構已領取票券列表
     *
     * @param instances   票券實例列表
     * @param couponNames 票券 ID 對應名稱
     * @param tenantId    租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildMyCouponList(List<CouponInstance> instances, Map<String, String> couponNames, String tenantId) {
        if (instances == null || instances.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            // 套用自訂 Header
            if (tenantId != null) {
                applyFunctionHeader(bubble, tenantId, "myCoupons", "#9C27B0", "🎫", "我的票券");
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "20px");
            body.put("spacing", "md");

            ArrayNode bodyContents = objectMapper.createArrayNode();

            ObjectNode icon = objectMapper.createObjectNode();
            icon.put("type", "text");
            icon.put("text", "🎫");
            icon.put("size", "3xl");
            icon.put("align", "center");
            bodyContents.add(icon);

            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "text");
            text.put("text", "您目前沒有票券");
            text.put("align", "center");
            text.put("color", SECONDARY_COLOR);
            text.put("margin", "md");
            bodyContents.add(text);

            ObjectNode tipText = objectMapper.createObjectNode();
            tipText.put("type", "text");
            tipText.put("text", "快去領取優惠券吧！");
            tipText.put("align", "center");
            tipText.put("size", "sm");
            tipText.put("color", SECONDARY_COLOR);
            bodyContents.add(tipText);

            body.set("contents", bodyContents);
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

        // 第一個 Bubble：使用說明
        bubbles.add(buildCouponUsageGuide());

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

            // 分隔線
            ObjectNode separator = objectMapper.createObjectNode();
            separator.put("type", "separator");
            separator.put("margin", "md");
            bodyContents.add(separator);

            // 票券代碼（大字顯示，方便給店家看）
            if (instance.getStatus() == CouponInstanceStatus.UNUSED) {
                ObjectNode codeLabel = objectMapper.createObjectNode();
                codeLabel.put("type", "text");
                codeLabel.put("text", "核銷代碼");
                codeLabel.put("size", "xs");
                codeLabel.put("color", SECONDARY_COLOR);
                codeLabel.put("margin", "md");
                bodyContents.add(codeLabel);

                ObjectNode codeBox = objectMapper.createObjectNode();
                codeBox.put("type", "box");
                codeBox.put("layout", "vertical");
                codeBox.put("backgroundColor", "#FFF3E0");
                codeBox.put("cornerRadius", "8px");
                codeBox.put("paddingAll", "10px");
                codeBox.put("margin", "sm");

                ObjectNode codeText = objectMapper.createObjectNode();
                codeText.put("type", "text");
                codeText.put("text", instance.getCode());
                codeText.put("size", "xl");
                codeText.put("weight", "bold");
                codeText.put("align", "center");
                codeText.put("color", "#E65100");

                codeBox.set("contents", objectMapper.createArrayNode().add(codeText));
                bodyContents.add(codeBox);

                // 使用提示
                ObjectNode tipText = objectMapper.createObjectNode();
                tipText.put("type", "text");
                tipText.put("text", "👆 出示此代碼給店家核銷");
                tipText.put("size", "xs");
                tipText.put("color", PRIMARY_COLOR);
                tipText.put("align", "center");
                tipText.put("margin", "sm");
                bodyContents.add(tipText);
            } else {
                // 已使用或已過期的票券，代碼顯示較小
                ObjectNode codeText = objectMapper.createObjectNode();
                codeText.put("type", "text");
                codeText.put("text", "序號：" + instance.getCode());
                codeText.put("size", "sm");
                codeText.put("color", SECONDARY_COLOR);
                codeText.put("margin", "md");
                bodyContents.add(codeText);
            }

            // 有效期限
            if (instance.getExpiresAt() != null) {
                ObjectNode expiryText = objectMapper.createObjectNode();
                expiryText.put("type", "text");
                String expiryPrefix = instance.getStatus() == CouponInstanceStatus.UNUSED ? "⏰ 有效至：" : "有效至：";
                expiryText.put("text", expiryPrefix + instance.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                expiryText.put("size", "xs");
                expiryText.put("color", instance.getStatus() == CouponInstanceStatus.UNUSED ? "#FF5722" : SECONDARY_COLOR);
                expiryText.put("margin", "sm");
                bodyContents.add(expiryText);
            }

            // 已使用時間
            if (instance.getStatus() == CouponInstanceStatus.USED && instance.getUsedAt() != null) {
                ObjectNode usedText = objectMapper.createObjectNode();
                usedText.put("type", "text");
                usedText.put("text", "使用時間：" + instance.getUsedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                usedText.put("size", "xs");
                usedText.put("color", SECONDARY_COLOR);
                usedText.put("margin", "sm");
                bodyContents.add(usedText);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);

            bubbles.add(bubble);
        }

        // 末端添加導航 Bubble
        bubbles.add(buildCarouselNavigationBubble());

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構票券使用說明 Bubble
     */
    private ObjectNode buildCouponUsageGuide() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");
        bubble.put("size", "kilo");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#FF6B6B");
        header.put("paddingAll", "15px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode headerIcon = objectMapper.createObjectNode();
        headerIcon.put("type", "text");
        headerIcon.put("text", "📋 票券使用說明");
        headerIcon.put("size", "md");
        headerIcon.put("weight", "bold");
        headerIcon.put("color", "#FFFFFF");
        headerIcon.put("align", "center");
        headerContents.add(headerIcon);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "15px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 步驟 1
        bodyContents.add(createUsageStep("1️⃣", "消費時告知店家要使用票券"));
        // 步驟 2
        bodyContents.add(createUsageStep("2️⃣", "出示票券代碼給店家"));
        // 步驟 3
        bodyContents.add(createUsageStep("3️⃣", "店家輸入代碼完成核銷"));

        // 注意事項
        ObjectNode noteBox = objectMapper.createObjectNode();
        noteBox.put("type", "box");
        noteBox.put("layout", "vertical");
        noteBox.put("backgroundColor", "#FFF8E1");
        noteBox.put("cornerRadius", "8px");
        noteBox.put("paddingAll", "10px");
        noteBox.put("margin", "md");

        ArrayNode noteContents = objectMapper.createArrayNode();

        ObjectNode noteTitle = objectMapper.createObjectNode();
        noteTitle.put("type", "text");
        noteTitle.put("text", "⚠️ 注意事項");
        noteTitle.put("size", "xs");
        noteTitle.put("weight", "bold");
        noteTitle.put("color", "#F57C00");
        noteContents.add(noteTitle);

        ObjectNode noteText = objectMapper.createObjectNode();
        noteText.put("type", "text");
        noteText.put("text", "• 票券核銷後即無法再次使用\n• 請留意有效期限\n• 無法與其他優惠併用");
        noteText.put("size", "xs");
        noteText.put("color", SECONDARY_COLOR);
        noteText.put("wrap", true);
        noteText.put("margin", "sm");
        noteContents.add(noteText);

        noteBox.set("contents", noteContents);
        bodyContents.add(noteBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 領取更多票券按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "10px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("領取更多票券", "action=view_coupons", PRIMARY_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構使用步驟項目
     */
    private ObjectNode createUsageStep(String number, String text) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "horizontal");
        box.put("spacing", "sm");

        ArrayNode contents = objectMapper.createArrayNode();

        ObjectNode numText = objectMapper.createObjectNode();
        numText.put("type", "text");
        numText.put("text", number);
        numText.put("size", "sm");
        numText.put("flex", 0);
        contents.add(numText);

        ObjectNode stepText = objectMapper.createObjectNode();
        stepText.put("type", "text");
        stepText.put("text", text);
        stepText.put("size", "sm");
        stepText.put("color", SECONDARY_COLOR);
        stepText.put("wrap", true);
        stepText.put("flex", 1);
        contents.add(stepText);

        box.set("contents", contents);
        return box;
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

    /**
     * 建構票券領取成功訊息
     *
     * @param couponName 票券名稱
     * @param couponCode 票券代碼
     * @param expiresAt  有效期限（可為 null）
     * @return Flex Message 內容
     */
    public JsonNode buildCouponReceiveSuccess(String couponName, String couponCode, LocalDateTime expiresAt) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header - 成功標示
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode successIcon = objectMapper.createObjectNode();
        successIcon.put("type", "text");
        successIcon.put("text", "🎉");
        successIcon.put("size", "3xl");
        successIcon.put("align", "center");
        headerContents.add(successIcon);

        ObjectNode successText = objectMapper.createObjectNode();
        successText.put("type", "text");
        successText.put("text", "領取成功！");
        successText.put("size", "xl");
        successText.put("weight", "bold");
        successText.put("color", "#FFFFFF");
        successText.put("align", "center");
        successText.put("margin", "md");
        headerContents.add(successText);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body - 票券資訊
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 票券名稱
        ObjectNode nameLabel = objectMapper.createObjectNode();
        nameLabel.put("type", "text");
        nameLabel.put("text", "票券名稱");
        nameLabel.put("size", "xs");
        nameLabel.put("color", SECONDARY_COLOR);
        bodyContents.add(nameLabel);

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", couponName);
        nameText.put("size", "lg");
        nameText.put("weight", "bold");
        nameText.put("wrap", true);
        bodyContents.add(nameText);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 核銷代碼區塊
        ObjectNode codeLabel = objectMapper.createObjectNode();
        codeLabel.put("type", "text");
        codeLabel.put("text", "🎫 核銷代碼");
        codeLabel.put("size", "sm");
        codeLabel.put("color", SECONDARY_COLOR);
        codeLabel.put("margin", "lg");
        bodyContents.add(codeLabel);

        ObjectNode codeBox = objectMapper.createObjectNode();
        codeBox.put("type", "box");
        codeBox.put("layout", "vertical");
        codeBox.put("backgroundColor", "#FFF3E0");
        codeBox.put("cornerRadius", "10px");
        codeBox.put("paddingAll", "15px");
        codeBox.put("margin", "sm");

        ObjectNode codeText = objectMapper.createObjectNode();
        codeText.put("type", "text");
        codeText.put("text", couponCode);
        codeText.put("size", "xxl");
        codeText.put("weight", "bold");
        codeText.put("align", "center");
        codeText.put("color", "#E65100");

        codeBox.set("contents", objectMapper.createArrayNode().add(codeText));
        bodyContents.add(codeBox);

        // 有效期限
        if (expiresAt != null) {
            ObjectNode expiryText = objectMapper.createObjectNode();
            expiryText.put("type", "text");
            expiryText.put("text", "⏰ 有效至：" + expiresAt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
            expiryText.put("size", "sm");
            expiryText.put("color", "#FF5722");
            expiryText.put("align", "center");
            expiryText.put("margin", "md");
            bodyContents.add(expiryText);
        }

        // 使用說明
        ObjectNode tipBox = objectMapper.createObjectNode();
        tipBox.put("type", "box");
        tipBox.put("layout", "vertical");
        tipBox.put("backgroundColor", "#E3F2FD");
        tipBox.put("cornerRadius", "8px");
        tipBox.put("paddingAll", "12px");
        tipBox.put("margin", "lg");

        ArrayNode tipContents = objectMapper.createArrayNode();

        ObjectNode tipTitle = objectMapper.createObjectNode();
        tipTitle.put("type", "text");
        tipTitle.put("text", "💡 如何使用");
        tipTitle.put("size", "sm");
        tipTitle.put("weight", "bold");
        tipTitle.put("color", "#1565C0");
        tipContents.add(tipTitle);

        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "消費時出示上方代碼給店家，由店家輸入代碼完成核銷即可享有優惠！");
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("wrap", true);
        tipText.put("margin", "sm");
        tipContents.add(tipText);

        tipBox.set("contents", tipContents);
        bodyContents.add(tipBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();

        // 查看我的票券按鈕
        ObjectNode viewBtn = createButton("查看我的票券", "action=view_my_coupons", PRIMARY_COLOR);
        viewBtn.put("flex", 1);
        footerContents.add(viewBtn);

        // 繼續領取按鈕
        ObjectNode moreBtn = createButton("領取更多", "action=view_coupons", LINK_COLOR);
        moreBtn.put("flex", 1);
        footerContents.add(moreBtn);

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
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
     * @param tenantId            租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildMemberInfo(Customer customer, long bookingCount, String membershipLevelName, String tenantId) {
        // 讀取自訂配色與樣式
        String memberColor = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "color", PRIMARY_COLOR) : PRIMARY_COLOR;
        String memberIcon = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "icon", "👤") : "👤";
        String memberSubtitle = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "subtitle", "") : "";
        String heroImageUrl = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "imageUrl", "") : "";

        ObjectNode carousel = objectMapper.createObjectNode();
        carousel.put("type", "carousel");

        ArrayNode bubbles = objectMapper.createArrayNode();

        // ========================================
        // Bubble 1: 會員資訊
        // ========================================
        ObjectNode infoBubble = objectMapper.createObjectNode();
        infoBubble.put("type", "bubble");

        // Hero 圖片
        if (!heroImageUrl.isEmpty()) {
            if (heroImageUrl.startsWith("/api/public/")) {
                heroImageUrl = appBaseUrl + heroImageUrl;
            }
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", heroImageUrl);
            hero.put("size", "full");
            hero.put("aspectRatio", "20:8");
            hero.put("aspectMode", "cover");
            infoBubble.set("hero", hero);
        }

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", memberColor);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", memberIcon.isEmpty() ? "👤" : memberIcon);
        icon.put("size", "3xl");
        icon.put("align", "center");
        headerContents.add(icon);

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        // 防止 null 導致 Flex Message 無效
        String displayName = customer.getName() != null ? customer.getName() : "會員";
        nameText.put("text", displayName);
        nameText.put("size", "xl");
        nameText.put("weight", "bold");
        nameText.put("color", "#FFFFFF");
        nameText.put("align", "center");
        headerContents.add(nameText);

        // 自訂副標題
        if (!memberSubtitle.isEmpty()) {
            ObjectNode subText = objectMapper.createObjectNode();
            subText.put("type", "text");
            subText.put("text", memberSubtitle);
            subText.put("size", "xs");
            subText.put("color", "#FFFFFF");
            subText.put("align", "center");
            subText.put("margin", "sm");
            headerContents.add(subText);
        }

        // 會員等級（帶標籤樣式）
        if (membershipLevelName != null) {
            ObjectNode levelBox = objectMapper.createObjectNode();
            levelBox.put("type", "box");
            levelBox.put("layout", "vertical");
            levelBox.put("backgroundColor", "#FFFFFF");
            levelBox.put("cornerRadius", "20px");
            levelBox.put("paddingAll", "5px");
            levelBox.put("paddingStart", "15px");
            levelBox.put("paddingEnd", "15px");
            levelBox.put("margin", "md");

            ObjectNode levelText = objectMapper.createObjectNode();
            levelText.put("type", "text");
            levelText.put("text", "⭐ " + membershipLevelName);
            levelText.put("size", "sm");
            levelText.put("color", PRIMARY_COLOR);
            levelText.put("weight", "bold");
            levelText.put("align", "center");

            levelBox.set("contents", objectMapper.createArrayNode().add(levelText));
            headerContents.add(levelBox);
        }

        header.set("contents", headerContents);
        infoBubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 點數餘額（大字顯示）
        ObjectNode pointsBox = objectMapper.createObjectNode();
        pointsBox.put("type", "box");
        pointsBox.put("layout", "vertical");
        pointsBox.put("paddingAll", "15px");
        pointsBox.put("backgroundColor", "#FFF8E1");
        pointsBox.put("cornerRadius", "10px");

        ArrayNode pointsContents = objectMapper.createArrayNode();

        ObjectNode pointsLabel = objectMapper.createObjectNode();
        pointsLabel.put("type", "text");
        pointsLabel.put("text", "💰 點數餘額");
        pointsLabel.put("size", "sm");
        pointsLabel.put("color", SECONDARY_COLOR);
        pointsLabel.put("align", "center");
        pointsContents.add(pointsLabel);

        ObjectNode pointsValue = objectMapper.createObjectNode();
        pointsValue.put("type", "text");
        int points = customer.getPointBalance() != null ? customer.getPointBalance() : 0;
        pointsValue.put("text", String.format("%,d 點", points));
        pointsValue.put("size", "xxl");
        pointsValue.put("weight", "bold");
        pointsValue.put("color", "#FF9800");
        pointsValue.put("align", "center");
        pointsValue.put("margin", "sm");
        pointsContents.add(pointsValue);

        pointsBox.set("contents", pointsContents);
        bodyContents.add(pointsBox);

        // 統計資訊
        ObjectNode statsBox = objectMapper.createObjectNode();
        statsBox.put("type", "box");
        statsBox.put("layout", "horizontal");
        statsBox.put("spacing", "md");
        statsBox.put("margin", "lg");

        ArrayNode statsContents = objectMapper.createArrayNode();

        // 預約次數
        statsContents.add(createStatItem("📅", "累計預約", bookingCount + " 次"));

        // 累計消費（如果有的話）
        if (customer.getTotalSpent() != null && customer.getTotalSpent().compareTo(java.math.BigDecimal.ZERO) > 0) {
            statsContents.add(createStatItem("💳", "累計消費", "NT$ " + String.format("%,.0f", customer.getTotalSpent())));
        }

        statsBox.set("contents", statsContents);
        bodyContents.add(statsBox);

        // 電話
        if (customer.getPhone() != null) {
            bodyContents.add(createInfoRow("📱 聯絡電話", customer.getPhone()));
        }

        body.set("contents", bodyContents);
        infoBubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();

        ObjectNode bookingBtn = createButton("開始預約", "action=start_booking", PRIMARY_COLOR);
        bookingBtn.put("flex", 1);
        footerContents.add(bookingBtn);

        ObjectNode couponBtn = createButton("我的票券", "action=view_my_coupons", LINK_COLOR);
        couponBtn.put("flex", 1);
        footerContents.add(couponBtn);

        footer.set("contents", footerContents);
        infoBubble.set("footer", footer);

        bubbles.add(infoBubble);

        // ========================================
        // Bubble 2: 點數說明
        // ========================================
        ObjectNode pointsBubble = objectMapper.createObjectNode();
        pointsBubble.put("type", "bubble");
        pointsBubble.put("size", "kilo");

        // Header
        ObjectNode pointsHeader = objectMapper.createObjectNode();
        pointsHeader.put("type", "box");
        pointsHeader.put("layout", "vertical");
        pointsHeader.put("backgroundColor", "#FF9800");
        pointsHeader.put("paddingAll", "15px");

        ObjectNode pointsHeaderText = objectMapper.createObjectNode();
        pointsHeaderText.put("type", "text");
        pointsHeaderText.put("text", "💡 點數說明");
        pointsHeaderText.put("size", "lg");
        pointsHeaderText.put("weight", "bold");
        pointsHeaderText.put("color", "#FFFFFF");
        pointsHeaderText.put("align", "center");

        pointsHeader.set("contents", objectMapper.createArrayNode().add(pointsHeaderText));
        pointsBubble.set("header", pointsHeader);

        // Body
        ObjectNode pointsBody = objectMapper.createObjectNode();
        pointsBody.put("type", "box");
        pointsBody.put("layout", "vertical");
        pointsBody.put("spacing", "lg");
        pointsBody.put("paddingAll", "15px");

        ArrayNode pointsBodyContents = objectMapper.createArrayNode();

        // 如何獲得點數
        ObjectNode earnBox = objectMapper.createObjectNode();
        earnBox.put("type", "box");
        earnBox.put("layout", "vertical");
        earnBox.put("spacing", "sm");

        ArrayNode earnContents = objectMapper.createArrayNode();

        ObjectNode earnTitle = objectMapper.createObjectNode();
        earnTitle.put("type", "text");
        earnTitle.put("text", "📈 如何獲得點數");
        earnTitle.put("size", "sm");
        earnTitle.put("weight", "bold");
        earnTitle.put("color", "#333333");
        earnContents.add(earnTitle);

        earnContents.add(createPointTip("✓ 完成預約服務"));
        earnContents.add(createPointTip("✓ 消費累積回饋"));
        earnContents.add(createPointTip("✓ 參與店家活動"));
        earnContents.add(createPointTip("✓ 生日禮、節慶禮"));

        earnBox.set("contents", earnContents);
        pointsBodyContents.add(earnBox);

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        pointsBodyContents.add(separator);

        // 如何使用點數
        ObjectNode useBox = objectMapper.createObjectNode();
        useBox.put("type", "box");
        useBox.put("layout", "vertical");
        useBox.put("spacing", "sm");

        ArrayNode useContents = objectMapper.createArrayNode();

        ObjectNode useTitle = objectMapper.createObjectNode();
        useTitle.put("type", "text");
        useTitle.put("text", "🎁 如何使用點數");
        useTitle.put("size", "sm");
        useTitle.put("weight", "bold");
        useTitle.put("color", "#333333");
        useContents.add(useTitle);

        useContents.add(createPointTip("✓ 消費時折抵現金"));
        useContents.add(createPointTip("✓ 兌換店家商品"));
        useContents.add(createPointTip("✓ 兌換優惠票券"));

        useBox.set("contents", useContents);
        pointsBodyContents.add(useBox);

        // 提示
        ObjectNode tipBox = objectMapper.createObjectNode();
        tipBox.put("type", "box");
        tipBox.put("layout", "vertical");
        tipBox.put("backgroundColor", "#E3F2FD");
        tipBox.put("cornerRadius", "8px");
        tipBox.put("paddingAll", "10px");

        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "💬 使用點數時，請告知店家您要折抵的點數，由店家協助處理。");
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("wrap", true);

        tipBox.set("contents", objectMapper.createArrayNode().add(tipText));
        pointsBodyContents.add(tipBox);

        pointsBody.set("contents", pointsBodyContents);
        pointsBubble.set("body", pointsBody);

        bubbles.add(pointsBubble);

        carousel.set("contents", bubbles);
        return carousel;
    }

    /**
     * 建構簡化版會員資訊訊息（單一 Bubble）
     *
     * <p>使用單一 Bubble 結構，比 Carousel 更穩定
     *
     * @param customer            顧客
     * @param bookingCount        預約次數
     * @param membershipLevelName 會員等級名稱（可為 null）
     * @param tenantId            租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容（單一 Bubble）
     */
    public JsonNode buildSimpleMemberInfo(Customer customer, long bookingCount, String membershipLevelName, String tenantId) {
        // 讀取自訂配色和圖示
        String memberColor = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "color", PRIMARY_COLOR) : PRIMARY_COLOR;
        String memberIcon = tenantId != null ? getFunctionConfig(tenantId, "memberInfo", "icon", "👤") : "👤";

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Hero 圖片（如有自訂）
        if (tenantId != null) {
            String heroImageUrl = getFunctionConfig(tenantId, "memberInfo", "imageUrl", "");
            if (!heroImageUrl.isEmpty()) {
                if (heroImageUrl.startsWith("/api/public/")) {
                    heroImageUrl = appBaseUrl + heroImageUrl;
                }
                ObjectNode hero = objectMapper.createObjectNode();
                hero.put("type", "image");
                hero.put("url", heroImageUrl);
                hero.put("size", "full");
                hero.put("aspectRatio", "20:8");
                hero.put("aspectMode", "cover");
                bubble.set("hero", hero);
            }
        }

        // ========================================
        // Header - 會員頭像與名稱
        // ========================================
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", memberColor);
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        // 頭像 icon
        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", memberIcon);
        icon.put("size", "3xl");
        icon.put("align", "center");
        headerContents.add(icon);

        // 名稱
        String displayName = customer.getName() != null ? customer.getName() : "會員";
        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", displayName);
        nameText.put("size", "xl");
        nameText.put("weight", "bold");
        nameText.put("color", "#FFFFFF");
        nameText.put("align", "center");
        nameText.put("margin", "md");
        headerContents.add(nameText);

        // 會員等級標籤
        String levelDisplay = membershipLevelName != null ? membershipLevelName : "一般會員";
        ObjectNode levelBadge = objectMapper.createObjectNode();
        levelBadge.put("type", "box");
        levelBadge.put("layout", "vertical");
        levelBadge.put("backgroundColor", "#FFFFFF4D");
        levelBadge.put("cornerRadius", "15px");
        levelBadge.put("paddingAll", "5px");
        levelBadge.put("paddingStart", "15px");
        levelBadge.put("paddingEnd", "15px");
        levelBadge.put("margin", "md");

        ObjectNode levelText = objectMapper.createObjectNode();
        levelText.put("type", "text");
        levelText.put("text", "⭐ " + levelDisplay);
        levelText.put("size", "sm");
        levelText.put("color", "#FFFFFF");
        levelText.put("align", "center");
        levelBadge.set("contents", objectMapper.createArrayNode().add(levelText));
        headerContents.add(levelBadge);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // ========================================
        // Body - 會員資訊
        // ========================================
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "lg");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 點數餘額（醒目顯示）
        ObjectNode pointsBox = objectMapper.createObjectNode();
        pointsBox.put("type", "box");
        pointsBox.put("layout", "vertical");
        pointsBox.put("paddingAll", "15px");
        pointsBox.put("backgroundColor", "#FFF8E1");
        pointsBox.put("cornerRadius", "10px");

        ArrayNode pointsContents = objectMapper.createArrayNode();

        ObjectNode pointsLabel = objectMapper.createObjectNode();
        pointsLabel.put("type", "text");
        pointsLabel.put("text", "💰 點數餘額");
        pointsLabel.put("size", "sm");
        pointsLabel.put("color", SECONDARY_COLOR);
        pointsLabel.put("align", "center");
        pointsContents.add(pointsLabel);

        int points = customer.getPointBalance() != null ? customer.getPointBalance() : 0;
        ObjectNode pointsValue = objectMapper.createObjectNode();
        pointsValue.put("type", "text");
        pointsValue.put("text", String.format("%,d 點", points));
        pointsValue.put("size", "xxl");
        pointsValue.put("weight", "bold");
        pointsValue.put("color", "#FF9800");
        pointsValue.put("align", "center");
        pointsValue.put("margin", "sm");
        pointsContents.add(pointsValue);

        pointsBox.set("contents", pointsContents);
        bodyContents.add(pointsBox);

        // 統計資訊（橫向排列）
        ObjectNode statsRow = objectMapper.createObjectNode();
        statsRow.put("type", "box");
        statsRow.put("layout", "horizontal");
        statsRow.put("spacing", "md");
        statsRow.put("margin", "lg");

        ArrayNode statsContents = objectMapper.createArrayNode();
        statsContents.add(createStatItem("📅", "累計預約", bookingCount + " 次"));

        // 累計消費（如果有的話）
        if (customer.getTotalSpent() != null && customer.getTotalSpent().compareTo(java.math.BigDecimal.ZERO) > 0) {
            statsContents.add(createStatItem("💳", "累計消費", "NT$ " + String.format("%,.0f", customer.getTotalSpent())));
        } else {
            // 顯示加入日期
            if (customer.getCreatedAt() != null) {
                String joinDate = customer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM"));
                statsContents.add(createStatItem("📆", "加入時間", joinDate));
            }
        }

        statsRow.set("contents", statsContents);
        bodyContents.add(statsRow);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // ========================================
        // Footer - 操作按鈕（垂直排列）
        // ========================================
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();

        // 橫向按鈕組：開始預約 + 我的票券
        ObjectNode buttonRow = objectMapper.createObjectNode();
        buttonRow.put("type", "box");
        buttonRow.put("layout", "horizontal");
        buttonRow.put("spacing", "sm");

        ArrayNode buttonRowContents = objectMapper.createArrayNode();

        ObjectNode bookingBtn = createButton("開始預約", "action=start_booking", PRIMARY_COLOR);
        bookingBtn.put("flex", 1);
        buttonRowContents.add(bookingBtn);

        ObjectNode couponBtn = createButton("我的票券", "action=view_my_coupons", LINK_COLOR);
        couponBtn.put("flex", 1);
        buttonRowContents.add(couponBtn);

        buttonRow.set("contents", buttonRowContents);
        footerContents.add(buttonRow);

        // 返回主選單按鈕
        footerContents.add(createButton("返回主選單", "action=main_menu", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構統計項目
     */
    private ObjectNode createStatItem(String icon, String label, String value) {
        ObjectNode box = objectMapper.createObjectNode();
        box.put("type", "box");
        box.put("layout", "vertical");
        box.put("backgroundColor", "#F5F5F5");
        box.put("cornerRadius", "8px");
        box.put("paddingAll", "10px");
        box.put("flex", 1);

        ArrayNode contents = objectMapper.createArrayNode();

        ObjectNode iconText = objectMapper.createObjectNode();
        iconText.put("type", "text");
        iconText.put("text", icon + " " + label);
        iconText.put("size", "xs");
        iconText.put("color", SECONDARY_COLOR);
        iconText.put("align", "center");
        contents.add(iconText);

        ObjectNode valueText = objectMapper.createObjectNode();
        valueText.put("type", "text");
        valueText.put("text", value);
        valueText.put("size", "sm");
        valueText.put("weight", "bold");
        valueText.put("align", "center");
        valueText.put("margin", "sm");
        contents.add(valueText);

        box.set("contents", contents);
        return box;
    }

    /**
     * 建構點數說明項目
     */
    private ObjectNode createPointTip(String text) {
        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", text);
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        return tipText;
    }

    // ========================================
    // 13. 商品相關
    // ========================================

    /**
     * 建構商品選單（Carousel）
     *
     * @param products 商品列表
     * @param tenantId 租戶 ID（用於讀取自訂樣式）
     * @return Flex Message 內容
     */
    public JsonNode buildProductMenu(List<Product> products, String tenantId) {
        if (products == null || products.isEmpty()) {
            ObjectNode bubble = objectMapper.createObjectNode();
            bubble.put("type", "bubble");

            // 套用自訂 Header
            if (tenantId != null) {
                applyFunctionHeader(bubble, tenantId, "productMenu", "#FF9800", "🛍️", "瀏覽商品");
            }

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
            priceText.put("text", String.format("NT$ %d", product.getPrice() != null ? product.getPrice().intValue() : 0));
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
                    product.getPrice() != null ? product.getPrice().intValue() : 0
            );

            footer.set("contents", objectMapper.createArrayNode().add(
                    createButton("選購", postbackData, PRIMARY_COLOR)
            ));
            bubble.set("footer", footer);

            bubbles.add(bubble);
        }

        // 末端添加導航 Bubble
        bubbles.add(buildCarouselNavigationBubble());

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
        footerContents.add(createButton("返回主選單", "action=main_menu", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 額外輔助訊息
    // ========================================

    /**
     * 建構歡迎訊息（新用戶加入時）
     *
     * @param tenantId 租戶 ID
     * @param userName 用戶名稱
     * @return Flex Message 內容
     */
    public JsonNode buildWelcomeMessage(String tenantId, String userName) {
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        String shopName = tenantOpt.map(Tenant::getName).orElse("我們的店");

        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", PRIMARY_COLOR);
        header.put("paddingAll", "25px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode welcomeEmoji = objectMapper.createObjectNode();
        welcomeEmoji.put("type", "text");
        welcomeEmoji.put("text", "🎉");
        welcomeEmoji.put("size", "3xl");
        welcomeEmoji.put("align", "center");
        headerContents.add(welcomeEmoji);

        ObjectNode welcomeTitle = objectMapper.createObjectNode();
        welcomeTitle.put("type", "text");
        welcomeTitle.put("text", "歡迎加入！");
        welcomeTitle.put("size", "xl");
        welcomeTitle.put("weight", "bold");
        welcomeTitle.put("color", "#FFFFFF");
        welcomeTitle.put("align", "center");
        welcomeTitle.put("margin", "md");
        headerContents.add(welcomeTitle);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode greetingText = objectMapper.createObjectNode();
        greetingText.put("type", "text");
        greetingText.put("text", String.format("嗨 %s！感謝您加入 %s 的官方帳號 👋", userName != null ? userName : "朋友", shopName));
        greetingText.put("size", "sm");
        greetingText.put("wrap", true);
        greetingText.put("color", "#333333");
        bodyContents.add(greetingText);

        // 功能介紹
        ObjectNode featureBox = objectMapper.createObjectNode();
        featureBox.put("type", "box");
        featureBox.put("layout", "vertical");
        featureBox.put("backgroundColor", "#F8F9FA");
        featureBox.put("cornerRadius", "8px");
        featureBox.put("paddingAll", "15px");
        featureBox.put("margin", "lg");

        ArrayNode featureContents = objectMapper.createArrayNode();

        ObjectNode featureTitle = objectMapper.createObjectNode();
        featureTitle.put("type", "text");
        featureTitle.put("text", "✨ 您可以在這裡：");
        featureTitle.put("size", "sm");
        featureTitle.put("weight", "bold");
        featureTitle.put("color", "#333333");
        featureContents.add(featureTitle);

        String[] features = {
                "📅 線上預約服務",
                "📋 查看與管理預約",
                "🛍️ 購買優惠商品",
                "🎁 領取專屬優惠券",
                "👤 查看會員點數"
        };

        for (String feature : features) {
            ObjectNode featureItem = objectMapper.createObjectNode();
            featureItem.put("type", "text");
            featureItem.put("text", feature);
            featureItem.put("size", "xs");
            featureItem.put("color", SECONDARY_COLOR);
            featureItem.put("margin", "sm");
            featureContents.add(featureItem);
        }

        featureBox.set("contents", featureContents);
        bodyContents.add(featureBox);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("🚀 開始使用", "action=main_menu", PRIMARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構幫助訊息
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildHelpMessage(String tenantId) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#5C6BC0");
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode headerTitle = objectMapper.createObjectNode();
        headerTitle.put("type", "text");
        headerTitle.put("text", "❓ 使用說明");
        headerTitle.put("size", "lg");
        headerTitle.put("weight", "bold");
        headerTitle.put("color", "#FFFFFF");
        headerTitle.put("align", "center");
        headerContents.add(headerTitle);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "md");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        // 關鍵字說明
        ObjectNode keywordTitle = objectMapper.createObjectNode();
        keywordTitle.put("type", "text");
        keywordTitle.put("text", "📝 快速關鍵字：");
        keywordTitle.put("size", "sm");
        keywordTitle.put("weight", "bold");
        bodyContents.add(keywordTitle);

        String[][] keywords = {
                {"預約", "開始預約流程"},
                {"查詢", "查看我的預約"},
                {"取消", "取消預約"},
                {"商品", "瀏覽商品"},
                {"票券", "領取/查看票券"},
                {"會員", "查看會員資訊"}
        };

        for (String[] keyword : keywords) {
            ObjectNode keywordRow = objectMapper.createObjectNode();
            keywordRow.put("type", "box");
            keywordRow.put("layout", "horizontal");
            keywordRow.put("margin", "sm");

            ArrayNode rowContents = objectMapper.createArrayNode();

            ObjectNode keywordText = objectMapper.createObjectNode();
            keywordText.put("type", "text");
            keywordText.put("text", "「" + keyword[0] + "」");
            keywordText.put("size", "sm");
            keywordText.put("weight", "bold");
            keywordText.put("color", PRIMARY_COLOR);
            keywordText.put("flex", 2);
            rowContents.add(keywordText);

            ObjectNode descText = objectMapper.createObjectNode();
            descText.put("type", "text");
            descText.put("text", keyword[1]);
            descText.put("size", "sm");
            descText.put("color", SECONDARY_COLOR);
            descText.put("flex", 3);
            rowContents.add(descText);

            keywordRow.set("contents", rowContents);
            bodyContents.add(keywordRow);
        }

        // 分隔線
        ObjectNode separator = objectMapper.createObjectNode();
        separator.put("type", "separator");
        separator.put("margin", "lg");
        bodyContents.add(separator);

        // 提示
        ObjectNode tipText = objectMapper.createObjectNode();
        tipText.put("type", "text");
        tipText.put("text", "💡 隨時輸入任何文字，都會顯示主選單喔！");
        tipText.put("size", "xs");
        tipText.put("color", SECONDARY_COLOR);
        tipText.put("wrap", true);
        tipText.put("margin", "lg");
        bodyContents.add(tipText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("返回主選單", "action=main_menu", SECONDARY_COLOR)
        ));
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構錯誤訊息
     *
     * @param errorMessage 錯誤訊息
     * @return Flex Message 內容
     */
    public JsonNode buildErrorMessage(String errorMessage) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#E74C3C");
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode errorIcon = objectMapper.createObjectNode();
        errorIcon.put("type", "text");
        errorIcon.put("text", "⚠️");
        errorIcon.put("size", "3xl");
        errorIcon.put("align", "center");
        headerContents.add(errorIcon);

        ObjectNode errorTitle = objectMapper.createObjectNode();
        errorTitle.put("type", "text");
        errorTitle.put("text", "操作失敗");
        errorTitle.put("size", "lg");
        errorTitle.put("weight", "bold");
        errorTitle.put("color", "#FFFFFF");
        errorTitle.put("align", "center");
        errorTitle.put("margin", "md");
        headerContents.add(errorTitle);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode errorText = objectMapper.createObjectNode();
        errorText.put("type", "text");
        errorText.put("text", errorMessage != null ? errorMessage : "發生未預期的錯誤，請稍後再試");
        errorText.put("size", "sm");
        errorText.put("wrap", true);
        errorText.put("align", "center");
        errorText.put("color", SECONDARY_COLOR);
        bodyContents.add(errorText);

        ObjectNode helpText = objectMapper.createObjectNode();
        helpText.put("type", "text");
        helpText.put("text", "如問題持續發生，請聯繫店家");
        helpText.put("size", "xs");
        helpText.put("wrap", true);
        helpText.put("align", "center");
        helpText.put("color", SECONDARY_COLOR);
        helpText.put("margin", "lg");
        bodyContents.add(helpText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("重新開始", "action=main_menu", PRIMARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構取消流程確認訊息
     *
     * @return Flex Message 內容
     */
    public JsonNode buildCancelFlowConfirmation() {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", "🤔");
        icon.put("size", "3xl");
        icon.put("align", "center");
        bodyContents.add(icon);

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", "確定要取消嗎？");
        titleText.put("size", "lg");
        titleText.put("weight", "bold");
        titleText.put("align", "center");
        titleText.put("margin", "lg");
        bodyContents.add(titleText);

        ObjectNode descText = objectMapper.createObjectNode();
        descText.put("type", "text");
        descText.put("text", "目前填寫的資料將不會保存");
        descText.put("size", "sm");
        descText.put("color", SECONDARY_COLOR);
        descText.put("align", "center");
        descText.put("margin", "md");
        bodyContents.add(descText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "horizontal");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("繼續預約", "action=resume_booking", PRIMARY_COLOR));
        footerContents.add(createButton("確定取消", "action=confirm_cancel_flow", SECONDARY_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構無法理解的訊息回覆
     *
     * @param tenantId 租戶 ID
     * @return Flex Message 內容
     */
    public JsonNode buildUnknownCommandMessage(String tenantId) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode icon = objectMapper.createObjectNode();
        icon.put("type", "text");
        icon.put("text", "🤖");
        icon.put("size", "3xl");
        icon.put("align", "center");
        bodyContents.add(icon);

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", "需要什麼服務呢？");
        titleText.put("size", "lg");
        titleText.put("weight", "bold");
        titleText.put("align", "center");
        titleText.put("margin", "lg");
        bodyContents.add(titleText);

        ObjectNode descText = objectMapper.createObjectNode();
        descText.put("type", "text");
        descText.put("text", "請點擊下方按鈕選擇服務，\n或輸入「幫助」查看使用說明");
        descText.put("size", "sm");
        descText.put("color", SECONDARY_COLOR);
        descText.put("align", "center");
        descText.put("wrap", true);
        descText.put("margin", "md");
        bodyContents.add(descText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("paddingAll", "15px");

        ArrayNode footerContents = objectMapper.createArrayNode();
        footerContents.add(createButton("📅 開始預約", "action=start_booking", PRIMARY_COLOR));
        footerContents.add(createButton("📋 查看選單", "action=main_menu", LINK_COLOR));

        footer.set("contents", footerContents);
        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 生日祝福與顧客喚回
    // ========================================

    /**
     * 建構生日祝福訊息
     *
     * @param customerName 顧客名稱
     * @param message      祝福訊息
     * @return Flex Message 內容
     */
    public JsonNode buildBirthdayGreeting(String customerName, String message) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header - 生日主題
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#FF6B9D");
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode iconText = objectMapper.createObjectNode();
        iconText.put("type", "text");
        iconText.put("text", "🎂🎉🎁");
        iconText.put("size", "xxl");
        iconText.put("align", "center");
        headerContents.add(iconText);

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", "生日快樂！");
        titleText.put("size", "xl");
        titleText.put("weight", "bold");
        titleText.put("color", "#FFFFFF");
        titleText.put("align", "center");
        titleText.put("margin", "md");
        headerContents.add(titleText);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");
        body.put("spacing", "md");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", "親愛的 " + customerName);
        nameText.put("size", "md");
        nameText.put("weight", "bold");
        nameText.put("align", "center");
        bodyContents.add(nameText);

        ObjectNode messageText = objectMapper.createObjectNode();
        messageText.put("type", "text");
        messageText.put("text", message);
        messageText.put("size", "sm");
        messageText.put("color", SECONDARY_COLOR);
        messageText.put("align", "center");
        messageText.put("wrap", true);
        messageText.put("margin", "lg");
        bodyContents.add(messageText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 預約按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("🎁 立即預約享優惠", "action=start_booking", "#FF6B9D")
        ));

        bubble.set("footer", footer);

        return bubble;
    }

    /**
     * 建構顧客喚回通知
     *
     * @param customerName 顧客名稱
     * @param message      喚回訊息
     * @return Flex Message 內容
     */
    public JsonNode buildRecallNotification(String customerName, String message) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // Header
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", "#6C5CE7");
        header.put("paddingAll", "20px");

        ArrayNode headerContents = objectMapper.createArrayNode();

        ObjectNode iconText = objectMapper.createObjectNode();
        iconText.put("type", "text");
        iconText.put("text", "💕");
        iconText.put("size", "xxl");
        iconText.put("align", "center");
        headerContents.add(iconText);

        ObjectNode titleText = objectMapper.createObjectNode();
        titleText.put("type", "text");
        titleText.put("text", "好久不見！");
        titleText.put("size", "xl");
        titleText.put("weight", "bold");
        titleText.put("color", "#FFFFFF");
        titleText.put("align", "center");
        titleText.put("margin", "md");
        headerContents.add(titleText);

        header.set("contents", headerContents);
        bubble.set("header", header);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "20px");
        body.put("spacing", "md");

        ArrayNode bodyContents = objectMapper.createArrayNode();

        ObjectNode nameText = objectMapper.createObjectNode();
        nameText.put("type", "text");
        nameText.put("text", "親愛的 " + customerName);
        nameText.put("size", "md");
        nameText.put("weight", "bold");
        nameText.put("align", "center");
        bodyContents.add(nameText);

        ObjectNode messageText = objectMapper.createObjectNode();
        messageText.put("type", "text");
        messageText.put("text", message);
        messageText.put("size", "sm");
        messageText.put("color", SECONDARY_COLOR);
        messageText.put("align", "center");
        messageText.put("wrap", true);
        messageText.put("margin", "lg");
        bodyContents.add(messageText);

        body.set("contents", bodyContents);
        bubble.set("body", body);

        // Footer - 預約按鈕
        ObjectNode footer = objectMapper.createObjectNode();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("paddingAll", "15px");

        footer.set("contents", objectMapper.createArrayNode().add(
                createButton("📅 立即預約", "action=start_booking", "#6C5CE7")
        ));

        bubble.set("footer", footer);

        return bubble;
    }

    // ========================================
    // 進階自訂 Rich Menu - Flex 彈窗
    // ========================================

    /**
     * 建構自訂 Flex 彈窗（單一 Bubble 或 Carousel）
     *
     * <p>支援格式：
     * <ul>
     *   <li>單一 Bubble：heroImage + title + description + buttons</li>
     *   <li>Carousel：多張 Bubble 卡片可左右滑動</li>
     * </ul>
     *
     * @param flexPopupConfig 彈窗配置 JSON
     * @return Flex Message JSON（null 或無效配置時回傳 null，由呼叫端決定 fallback）
     */
    public JsonNode buildCustomFlexPopup(JsonNode flexPopupConfig) {
        if (flexPopupConfig == null) {
            return null;
        }

        String type = flexPopupConfig.path("type").asText("carousel");
        JsonNode bubblesConfig = flexPopupConfig.path("bubbles");

        if (!bubblesConfig.isArray() || bubblesConfig.isEmpty()) {
            // 如果沒有卡片，返回主選單
            return null;
        }

        if ("carousel".equals(type) && bubblesConfig.size() > 1) {
            // 多張卡片 → Carousel
            ObjectNode carousel = objectMapper.createObjectNode();
            carousel.put("type", "carousel");
            ArrayNode contents = objectMapper.createArrayNode();

            for (JsonNode bubbleConfig : bubblesConfig) {
                contents.add(buildCustomFlexBubble(bubbleConfig));
            }

            carousel.set("contents", contents);

            // 包裝為 Flex Message
            ObjectNode flexMessage = objectMapper.createObjectNode();
            flexMessage.put("type", "flex");
            flexMessage.put("altText", "自訂選單");
            flexMessage.set("contents", carousel);
            return flexMessage;
        } else if (bubblesConfig.size() > 0) {
            // 單張卡片 → Bubble
            JsonNode bubble = buildCustomFlexBubble(bubblesConfig.get(0));

            ObjectNode flexMessage = objectMapper.createObjectNode();
            flexMessage.put("type", "flex");
            flexMessage.put("altText", "自訂選單");
            flexMessage.set("contents", bubble);
            return flexMessage;
        }

        return null;
    }

    /**
     * 建構單張 Flex Bubble 卡片
     *
     * @param bubbleConfig 卡片配置
     * @return Bubble JSON
     */
    private JsonNode buildCustomFlexBubble(JsonNode bubbleConfig) {
        ObjectNode bubble = objectMapper.createObjectNode();
        bubble.put("type", "bubble");

        // ========================================
        // Hero（主圖）
        // ========================================
        String heroImageUrl = bubbleConfig.path("heroImageUrl").asText("");
        if (!heroImageUrl.isEmpty()) {
            ObjectNode hero = objectMapper.createObjectNode();
            hero.put("type", "image");
            hero.put("url", heroImageUrl);
            hero.put("size", "full");
            hero.put("aspectRatio", "20:13");
            hero.put("aspectMode", "cover");
            bubble.set("hero", hero);
        }

        // ========================================
        // Body（標題 + 說明）
        // ========================================
        String title = bubbleConfig.path("title").asText("");
        String description = bubbleConfig.path("description").asText("");

        if (!title.isEmpty() || !description.isEmpty()) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "15px");
            ArrayNode bodyContents = objectMapper.createArrayNode();

            if (!title.isEmpty()) {
                ObjectNode titleText = objectMapper.createObjectNode();
                titleText.put("type", "text");
                titleText.put("text", title);
                titleText.put("weight", "bold");
                titleText.put("size", "lg");
                titleText.put("wrap", true);
                bodyContents.add(titleText);
            }

            if (!description.isEmpty()) {
                ObjectNode descText = objectMapper.createObjectNode();
                descText.put("type", "text");
                descText.put("text", description);
                descText.put("size", "sm");
                descText.put("color", "#888888");
                descText.put("wrap", true);
                if (!title.isEmpty()) {
                    descText.put("margin", "md");
                }
                bodyContents.add(descText);
            }

            body.set("contents", bodyContents);
            bubble.set("body", body);
        }

        // ========================================
        // Footer（按鈕）
        // ========================================
        JsonNode buttonsConfig = bubbleConfig.path("buttons");
        if (buttonsConfig.isArray() && !buttonsConfig.isEmpty()) {
            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("spacing", "sm");
            footer.put("paddingAll", "15px");

            ArrayNode footerContents = objectMapper.createArrayNode();

            for (JsonNode btnConfig : buttonsConfig) {
                String btnLabel = btnConfig.path("label").asText("按鈕");
                JsonNode btnAction = btnConfig.path("action");

                ObjectNode button = objectMapper.createObjectNode();
                button.put("type", "button");
                button.put("style", "primary");
                button.put("height", "sm");

                // 按鈕顏色
                String btnColor = btnConfig.path("color").asText("#1DB446");
                button.put("color", btnColor);

                ObjectNode action = objectMapper.createObjectNode();
                String actionType = btnAction.path("type").asText("postback");

                switch (actionType) {
                    case "uri" -> {
                        action.put("type", "uri");
                        action.put("label", btnLabel);
                        action.put("uri", btnAction.path("uri").asText("https://example.com"));
                    }
                    case "postback" -> {
                        action.put("type", "postback");
                        action.put("label", btnLabel);
                        action.put("data", btnAction.path("data").asText("action=main_menu"));
                        action.put("displayText", btnLabel);
                    }
                    default -> {
                        action.put("type", "postback");
                        action.put("label", btnLabel);
                        action.put("data", "action=main_menu");
                        action.put("displayText", btnLabel);
                    }
                }

                button.set("action", action);
                footerContents.add(button);
            }

            // 加入「返回主選單」按鈕
            footerContents.add(createButton("↩ 返回主選單", "action=main_menu", "#AAAAAA"));

            footer.set("contents", footerContents);
            bubble.set("footer", footer);
        }

        return bubble;
    }
}
