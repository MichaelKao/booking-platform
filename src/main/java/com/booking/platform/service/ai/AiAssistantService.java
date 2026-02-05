package com.booking.platform.service.ai;

import com.booking.platform.common.config.GroqConfig;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.enums.ServiceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI 智慧客服服務
 *
 * <p>使用 Groq API (Llama 3.1) 提供智慧問答功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private final GroqConfig groqConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    /**
     * 系統提示詞模板
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是「%s」的專屬 LINE 智慧客服小幫手。你的名字是「小助手」。

        【你的個性】
        - 親切熱情、像朋友一樣聊天
        - 專業但不死板，會用輕鬆的方式解釋
        - 主動提供有用的建議
        - 適當使用表情符號讓對話更生動 😊

        【店家資訊】
        %s

        【我們的服務項目與價格】
        %s

        【這位顧客的資料】
        %s

        【回答指南】
        1. 用繁體中文回答，語氣親切自然
        2. 回答要具體有用，不要敷衍
        3. 如果顧客問價格，直接告訴他確切金額
        4. 如果顧客想預約，告訴他「點選下方的『開始預約』就可以囉！」
        5. 如果顧客問營業時間或地址，直接提供資訊
        6. 如果是老顧客（有預約記錄），可以說「歡迎回來」
        7. 如果問題超出你的知識範圍，說「這個問題我不太確定，建議您直接打電話詢問店家會更準確喔！」
        8. 回答控制在 50-150 字左右，不要太長

        【重要！選單顯示規則】
        在你的回覆最後，必須加上以下其中一個標記（標記不會顯示給顧客看）：

        加上 [SHOW_MENU] 的情況（顯示服務選單）- 只有以下情況才加：
        - 顧客明確說「我要預約」、「幫我預約」、「想預約」、「預約一下」
        - 顧客明確說「我要買」、「我想買」、「購買」
        - 顧客明確說「我要領票券」、「領優惠券」
        - 顧客明確說「查詢我的預約」、「我的預約」

        加上 [NO_MENU] 的情況（不顯示選單）- 大部分情況都用這個：
        - 顧客打招呼（你好、嗨、哈囉）
        - 顧客問價格、問服務內容
        - 顧客問營業時間、地址、電話
        - 顧客問「有什麼服務」、「可以做什麼」（只是詢問，還沒決定）
        - 顧客說「謝謝」、「好的」、「了解」
        - 顧客在考慮、還沒明確說要預約
        - 任何不確定的情況都用 [NO_MENU]

        【範例對話】
        顧客：「你好」
        回答：「嗨～歡迎光臨！😊 我是小助手，有什麼我可以幫您的嗎？您可以問我服務項目、價格，或是跟我說『我要預約』就可以開始預約囉！[NO_MENU]」

        顧客：「請問剪髮多少錢？」
        回答：「我們的剪髮服務是 NT$500，大約需要 60 分鐘，包含洗髮和造型喔！✂️ 想預約的話跟我說一聲～[NO_MENU]」

        顧客：「我要預約剪髮」
        回答：「太棒了！點選下方的『開始預約』就可以選擇喜歡的時間和設計師囉！期待為您服務～ 😊[SHOW_MENU]」

        顧客：「幫我預約」
        回答：「沒問題！請點選下方的『開始預約』，選擇您想要的服務和時間吧！😊[SHOW_MENU]」

        顧客：「你們在哪裡？」
        回答：「我們的地址是 [地址]，搭捷運到 XX 站走路約 5 分鐘就到了！📍 如果找不到的話，可以打電話給我們，我們幫您指路～[NO_MENU]」

        顧客：「有什麼服務？」
        回答：「我們提供剪髮 NT$500、染髮 NT$1,500、護髮 NT$800 等服務喔！想預約哪個服務呢？跟我說『我要預約』就可以開始～[NO_MENU]」

        顧客：「謝謝」
        回答：「不客氣！有任何問題隨時問我喔～ 😊[NO_MENU]」
        """;

    /**
     * AI 回覆結果，包含文字內容和是否顯示選單
     */
    public record AiResponse(String text, boolean showMenu) {}

    /**
     * 處理顧客訊息並回覆
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID（可為 null）
     * @param userMessage 用戶訊息
     * @return AI 回覆內容
     */
    public String chat(String tenantId, String customerId, String userMessage) {
        AiResponse response = chatWithMenuFlag(tenantId, customerId, userMessage);
        return response != null ? response.text() : null;
    }

    /**
     * 處理顧客訊息並回覆（包含選單顯示標記）
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID（可為 null）
     * @param userMessage 用戶訊息
     * @return AI 回覆結果（包含文字和是否顯示選單）
     */
    public AiResponse chatWithMenuFlag(String tenantId, String customerId, String userMessage) {
        // 檢查是否啟用 AI
        if (!groqConfig.isEnabled()) {
            log.debug("AI 助手未啟用");
            return null;
        }

        // 檢查 API Key
        if (groqConfig.getApiKey() == null || groqConfig.getApiKey().isEmpty()) {
            log.warn("Groq API Key 未設定");
            return null;
        }

        try {
            // 建立系統提示詞
            String systemPrompt = buildSystemPrompt(tenantId, customerId);

            // 呼叫 Groq API
            String rawResponse = callGroqApi(systemPrompt, userMessage);

            // 解析回覆，提取選單標記
            AiResponse response = parseResponse(rawResponse);

            log.info("AI 回覆成功，租戶：{}，顯示選單：{}，訊息長度：{}",
                    tenantId, response.showMenu(), response.text().length());
            return response;

        } catch (Exception e) {
            log.error("AI 回覆失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 AI 回覆，提取選單顯示標記
     *
     * @param rawResponse AI 原始回覆
     * @return 解析後的回覆（移除標記）
     */
    private AiResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return new AiResponse("", false);
        }

        // 檢查是否包含 [SHOW_MENU] 標記
        boolean showMenu = rawResponse.contains("[SHOW_MENU]");

        // 移除標記
        String cleanedText = rawResponse
                .replace("[SHOW_MENU]", "")
                .replace("[NO_MENU]", "")
                .trim();

        return new AiResponse(cleanedText, showMenu);
    }

    /**
     * 檢查是否應該使用 AI 回覆
     *
     * @param userMessage 用戶訊息
     * @return 是否應該使用 AI
     */
    public boolean shouldUseAi(String userMessage) {
        try {
            // 檢查設定是否存在
            if (groqConfig == null) {
                return false;
            }

            // 檢查是否啟用
            if (!groqConfig.isEnabled()) {
                return false;
            }

            // API Key 檢查
            String apiKey = groqConfig.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return false;
            }

            // 訊息檢查
            if (userMessage == null || userMessage.length() < 2 || userMessage.length() > 500) {
                return false;
            }

            // 排除純表情或特殊字元
            String cleaned = userMessage.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
            if (cleaned.length() < 2) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("shouldUseAi 檢查失敗：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 建立系統提示詞
     */
    private String buildSystemPrompt(String tenantId, String customerId) {
        // 取得店家資訊
        String shopInfo = getShopInfo(tenantId);

        // 取得服務項目
        String serviceInfo = getServiceInfo(tenantId);

        // 取得顧客資訊
        String customerInfo = getCustomerInfo(tenantId, customerId);

        // 取得店家名稱
        String shopName = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .map(Tenant::getName)
                .orElse("店家");

        return String.format(SYSTEM_PROMPT_TEMPLATE, shopName, shopInfo, serviceInfo, customerInfo);
    }

    /**
     * 取得店家資訊
     */
    private String getShopInfo(String tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
        if (tenantOpt.isEmpty()) {
            return "店家資訊不可用";
        }

        Tenant tenant = tenantOpt.get();
        StringBuilder sb = new StringBuilder();

        sb.append("- 店名：").append(tenant.getName()).append("\n");

        if (tenant.getPhone() != null) {
            sb.append("- 電話：").append(tenant.getPhone()).append("\n");
        }

        if (tenant.getAddress() != null) {
            sb.append("- 地址：").append(tenant.getAddress()).append("\n");
        }

        // 營業時間（從設定取得，這裡用預設值）
        sb.append("- 營業時間：週一至週日 10:00-20:00（請以實際公告為準）\n");

        return sb.toString();
    }

    /**
     * 取得服務項目資訊
     */
    private String getServiceInfo(String tenantId) {
        List<ServiceItem> services = serviceItemRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, ServiceStatus.ACTIVE);

        if (services.isEmpty()) {
            return "目前沒有可用的服務項目";
        }

        return services.stream()
                .map(s -> String.format("- %s：NT$%d（約 %d 分鐘）%s",
                        s.getName(),
                        s.getPrice().intValue(),
                        s.getDuration(),
                        s.getDescription() != null ? " - " + s.getDescription() : ""))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 取得顧客資訊
     */
    private String getCustomerInfo(String tenantId, String customerId) {
        if (customerId == null) {
            return "新顧客（尚未建立會員資料）";
        }

        Optional<Customer> customerOpt = customerRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId);

        if (customerOpt.isEmpty()) {
            return "新顧客";
        }

        Customer customer = customerOpt.get();
        StringBuilder sb = new StringBuilder();

        if (customer.getName() != null) {
            sb.append("- 姓名：").append(customer.getName()).append("\n");
        }

        sb.append("- 點數餘額：").append(customer.getPointBalance() != null ? customer.getPointBalance() : 0).append(" 點\n");

        // 查詢預約次數
        long bookingCount = bookingRepository.countByCustomerIdAndTenantId(customerId, tenantId);
        sb.append("- 累計預約：").append(bookingCount).append(" 次\n");

        return sb.toString();
    }

    /**
     * 呼叫 Groq API
     */
    private String callGroqApi(String systemPrompt, String userMessage) {
        String url = groqConfig.getApiUrl() + "/chat/completions";

        // 建立請求標頭
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqConfig.getApiKey());

        // 建立請求體
        Map<String, Object> requestBody = Map.of(
                "model", groqConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", groqConfig.getMaxTokens(),
                "temperature", groqConfig.getTemperature()
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 解析回應
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("呼叫 Groq API 失敗：{}", e.getMessage());
            throw new RuntimeException("AI 回應失敗", e);
        }
    }
}
