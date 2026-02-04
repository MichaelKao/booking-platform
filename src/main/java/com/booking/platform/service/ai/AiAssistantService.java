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
        你是「%s」的 LINE 智慧客服助手。請用繁體中文、親切友善的語氣回答顧客問題。

        【店家資訊】
        %s

        【服務項目】
        %s

        【回答規則】
        1. 簡潔回答，不超過 100 字
        2. 如果問題與預約相關，引導顧客點選「開始預約」按鈕
        3. 如果問題與價格相關，提供具體價格資訊
        4. 如果無法回答，請說「抱歉，這個問題我無法回答，請直接聯繫店家喔！」
        5. 不要編造不存在的服務或價格
        6. 語氣親切，可適當使用 1-2 個表情符號

        【顧客資訊】
        %s
        """;

    /**
     * 處理顧客訊息並回覆
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID（可為 null）
     * @param userMessage 用戶訊息
     * @return AI 回覆內容
     */
    public String chat(String tenantId, String customerId, String userMessage) {
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
            String response = callGroqApi(systemPrompt, userMessage);

            log.info("AI 回覆成功，租戶：{}，訊息長度：{}", tenantId, response.length());
            return response;

        } catch (Exception e) {
            log.error("AI 回覆失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
            return null;
        }
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
