package com.booking.platform.service.line;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.common.EncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * LINE 訊息服務
 *
 * <p>處理 LINE 訊息的發送（Reply 和 Push）
 *
 * @author Developer
 * @since 1.0.0
 * @see <a href="https://developers.line.biz/en/reference/messaging-api/">LINE Messaging API</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineMessageService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantLineConfigRepository lineConfigRepository;
    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // ========================================
    // 配置
    // ========================================

    @Value("${line.bot.api-endpoint:https://api.line.me}")
    private String apiEndpoint;

    // ========================================
    // API 端點
    // ========================================

    private static final String REPLY_API = "/v2/bot/message/reply";
    private static final String PUSH_API = "/v2/bot/message/push";
    private static final String MULTICAST_API = "/v2/bot/message/multicast";
    private static final String PROFILE_API = "/v2/bot/profile/";

    // ========================================
    // 回覆訊息
    // ========================================

    /**
     * 回覆訊息（Reply）
     *
     * <p>使用 replyToken 回覆用戶訊息，不計入推送額度
     * <p>注意：此方法不使用 @Async，因為 replyToken 只有約 30 秒有效期
     * <p>呼叫此方法的 processWebhook 已經是 @Async，所以這裡同步執行即可
     *
     * @param tenantId   租戶 ID
     * @param replyToken 回覆 Token
     * @param messages   訊息列表
     */
    public void reply(String tenantId, String replyToken, List<Map<String, Object>> messages) {
        try {
            String accessToken = getAccessToken(tenantId);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("replyToken", replyToken);
            requestBody.set("messages", objectMapper.valueToTree(messages));

            sendRequest(REPLY_API, accessToken, requestBody);

        } catch (Exception e) {
            log.error("回覆訊息失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
        }
    }

    /**
     * 回覆文字訊息
     *
     * @param tenantId   租戶 ID
     * @param replyToken 回覆 Token
     * @param text       文字內容
     */
    public void replyText(String tenantId, String replyToken, String text) {
        Map<String, Object> message = Map.of(
                "type", "text",
                "text", text
        );
        reply(tenantId, replyToken, List.of(message));
    }

    /**
     * 回覆 Flex Message
     *
     * @param tenantId   租戶 ID
     * @param replyToken 回覆 Token
     * @param altText    替代文字（不支援 Flex Message 的客戶端會看到）
     * @param contents   Flex Message 內容
     */
    public void replyFlex(String tenantId, String replyToken, String altText, JsonNode contents) {
        Map<String, Object> message = Map.of(
                "type", "flex",
                "altText", altText,
                "contents", contents
        );
        reply(tenantId, replyToken, List.of(message));
    }

    /**
     * 回覆文字訊息 + Flex Message
     *
     * @param tenantId   租戶 ID
     * @param replyToken 回覆 Token
     * @param text       文字訊息
     * @param altText    Flex Message 替代文字
     * @param contents   Flex Message 內容
     */
    public void replyTextAndFlex(String tenantId, String replyToken, String text, String altText, JsonNode contents) {
        Map<String, Object> textMessage = Map.of(
                "type", "text",
                "text", text
        );
        Map<String, Object> flexMessage = Map.of(
                "type", "flex",
                "altText", altText,
                "contents", contents
        );
        reply(tenantId, replyToken, List.of(textMessage, flexMessage));
    }

    // ========================================
    // 推送訊息
    // ========================================

    /**
     * 推送訊息給單一用戶（Push）
     *
     * <p>主動推送訊息，會計入推送額度
     *
     * @param tenantId 租戶 ID
     * @param userId   LINE User ID
     * @param messages 訊息列表
     */
    @Async
    @Transactional
    public void push(String tenantId, String userId, List<Map<String, Object>> messages) {
        log.debug("推送訊息，租戶：{}，用戶：{}", tenantId, userId);

        try {
            // ========================================
            // 1. 檢查推送額度
            // ========================================

            checkAndUsePushQuota(tenantId, 1);

            // ========================================
            // 2. 取得 Access Token
            // ========================================

            String accessToken = getAccessToken(tenantId);

            // ========================================
            // 3. 建立請求
            // ========================================

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("to", userId);
            requestBody.set("messages", objectMapper.valueToTree(messages));

            // ========================================
            // 4. 發送請求
            // ========================================

            sendRequest(PUSH_API, accessToken, requestBody);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("推送訊息失敗，租戶：{}，用戶：{}，錯誤：{}", tenantId, userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_SEND_FAILED, "LINE 訊息推送失敗");
        }
    }

    /**
     * 推送文字訊息
     *
     * @param tenantId 租戶 ID
     * @param userId   LINE User ID
     * @param text     文字內容
     */
    public void pushText(String tenantId, String userId, String text) {
        Map<String, Object> message = Map.of(
                "type", "text",
                "text", text
        );
        push(tenantId, userId, List.of(message));
    }

    /**
     * 推送 Flex Message
     *
     * @param tenantId 租戶 ID
     * @param userId   LINE User ID
     * @param altText  替代文字
     * @param contents Flex Message 內容
     */
    public void pushFlex(String tenantId, String userId, String altText, JsonNode contents) {
        Map<String, Object> message = Map.of(
                "type", "flex",
                "altText", altText,
                "contents", contents
        );
        push(tenantId, userId, List.of(message));
    }

    /**
     * 群發訊息（Multicast）
     *
     * <p>推送訊息給多位用戶，每位用戶計 1 次推送額度
     *
     * @param tenantId 租戶 ID
     * @param userIds  LINE User ID 列表（最多 500 個）
     * @param messages 訊息列表
     */
    @Async
    @Transactional
    public void multicast(String tenantId, List<String> userIds, List<Map<String, Object>> messages) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        log.debug("群發訊息，租戶：{}，用戶數：{}", tenantId, userIds.size());

        try {
            // ========================================
            // 1. 檢查推送額度
            // ========================================

            checkAndUsePushQuota(tenantId, userIds.size());

            // ========================================
            // 2. 取得 Access Token
            // ========================================

            String accessToken = getAccessToken(tenantId);

            // ========================================
            // 3. 建立請求
            // ========================================

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode toArray = objectMapper.createArrayNode();
            userIds.forEach(toArray::add);
            requestBody.set("to", toArray);
            requestBody.set("messages", objectMapper.valueToTree(messages));

            // ========================================
            // 4. 發送請求
            // ========================================

            sendRequest(MULTICAST_API, accessToken, requestBody);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("群發訊息失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_SEND_FAILED, "LINE 訊息群發失敗");
        }
    }

    // ========================================
    // 用戶資訊
    // ========================================

    /**
     * 取得用戶個人資料
     *
     * @param tenantId 租戶 ID
     * @param userId   LINE User ID
     * @return 用戶資料（包含 displayName, pictureUrl, statusMessage）
     */
    public JsonNode getProfile(String tenantId, String userId) {
        try {
            String accessToken = getAccessToken(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    apiEndpoint + PROFILE_API + userId,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("取得用戶資料失敗，租戶：{}，用戶：{}，錯誤：{}",
                    tenantId, userId, e.getMessage());
            return null;
        }
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 取得解密後的 Access Token
     */
    private String getAccessToken(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "LINE 設定不存在"
                ));

        if (config.getChannelAccessTokenEncrypted() == null) {
            throw new BusinessException(
                    ErrorCode.LINE_CONFIG_INVALID, "LINE Access Token 未設定"
            );
        }

        return encryptionService.decrypt(config.getChannelAccessTokenEncrypted());
    }

    /**
     * 檢查並使用推送額度
     */
    private void checkAndUsePushQuota(String tenantId, int count) {
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TENANT_NOT_FOUND, "租戶不存在"
                ));

        if (!tenant.hasPushQuota(count)) {
            throw new BusinessException(
                    ErrorCode.LINE_PUSH_QUOTA_EXCEEDED,
                    String.format("推送額度不足，剩餘：%d，需要：%d",
                            tenant.getMonthlyPushQuota() - tenant.getMonthlyPushUsed(),
                            count)
            );
        }

        tenant.usePushQuota(count);
        tenantRepository.save(tenant);

        // 同時更新 LINE 設定的計數
        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            for (int i = 0; i < count; i++) {
                config.incrementPushCount();
            }
            lineConfigRepository.save(config);
        });
    }

    /**
     * 發送 HTTP 請求到 LINE API
     */
    private void sendRequest(String endpoint, String accessToken, ObjectNode requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiEndpoint + endpoint,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("LINE API 請求失敗，端點：{}，狀態碼：{}，回應：{}",
                        endpoint, response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("LINE API 請求錯誤，端點：{}，錯誤：{}", endpoint, e.getMessage());
            throw e;
        }
    }
}
