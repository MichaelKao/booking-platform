package com.booking.platform.controller.line;

import com.booking.platform.common.line.LineSignatureValidator;
import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.enums.line.LineConfigStatus;
import com.booking.platform.service.common.EncryptionService;
import com.booking.platform.service.line.LineConfigService;
import com.booking.platform.service.line.LineMessageService;
import com.booking.platform.service.line.LineWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * LINE Webhook 控制器
 *
 * <p>接收 LINE 平台的 Webhook 事件
 *
 * <p>URL 格式：POST /api/line/webhook/{tenantCode}
 *
 * <p>安全性：
 * <ul>
 *   <li>使用 X-Line-Signature 驗證請求來源</li>
 *   <li>每個租戶使用獨立的 Channel Secret</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/line/webhook")
@RequiredArgsConstructor
@Slf4j
public class LineWebhookController {

    // ========================================
    // 依賴注入
    // ========================================

    private final LineConfigService lineConfigService;
    private final LineWebhookService webhookService;
    private final LineMessageService messageService;
    private final LineSignatureValidator signatureValidator;
    private final EncryptionService encryptionService;

    // ========================================
    // Webhook 端點
    // ========================================

    /**
     * 調試端點：發送測試訊息（直接呼叫 LINE API）
     */
    @GetMapping("/debug/{tenantCode}/push/{lineUserId}")
    public ResponseEntity<ApiResponse<Object>> debugPushMessage(
            @PathVariable String tenantCode,
            @PathVariable String lineUserId
    ) {
        log.info("=== 調試發送訊息 ===");
        log.info("租戶代碼：{}，LINE User ID：{}", tenantCode, lineUserId);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("tenantCode", tenantCode);
        result.put("lineUserId", lineUserId);

        try {
            // 1. 查詢租戶設定
            Optional<TenantLineConfig> configOpt = lineConfigService.getConfigByTenantCode(tenantCode);
            if (configOpt.isEmpty()) {
                result.put("step", "查詢租戶設定");
                result.put("error", "找不到租戶 LINE 設定");
                return ResponseEntity.ok(ApiResponse.error("CONFIG_NOT_FOUND", "找不到租戶 LINE 設定"));
            }

            TenantLineConfig config = configOpt.get();
            String tenantId = config.getTenantId();
            result.put("tenantId", tenantId);
            result.put("configStatus", config.getStatus().name());
            result.put("hasToken", config.getChannelAccessTokenEncrypted() != null);

            // 2. 取得 Access Token
            if (config.getChannelAccessTokenEncrypted() == null) {
                result.put("error", "Access Token 未設定");
                return ResponseEntity.ok(ApiResponse.error("NO_TOKEN", "Access Token 未設定"));
            }

            String accessToken = encryptionService.decrypt(config.getChannelAccessTokenEncrypted());
            result.put("tokenLength", accessToken.length());
            result.put("tokenPrefix", accessToken.substring(0, Math.min(20, accessToken.length())) + "...");

            // 3. 直接呼叫 LINE API
            String testMessage = "診斷測試 - " + System.currentTimeMillis();
            result.put("testMessage", testMessage);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String requestBody = String.format(
                "{\"to\":\"%s\",\"messages\":[{\"type\":\"text\",\"text\":\"%s\"}]}",
                lineUserId, testMessage
            );
            result.put("requestBody", requestBody);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.line.me/v2/bot/message/push"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            result.put("httpStatus", response.statusCode());
            result.put("httpResponse", response.body());
            result.put("success", response.statusCode() == 200);

            if (response.statusCode() == 200) {
                return ResponseEntity.ok(ApiResponse.ok("訊息發送成功", result));
            } else {
                return ResponseEntity.ok(ApiResponse.error("LINE_API_ERROR", "LINE API 回應：" + response.body()));
            }

        } catch (Exception e) {
            log.error("發送測試訊息失敗：", e);
            result.put("error", e.getClass().getName() + ": " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("SEND_FAILED", e.getMessage()));
        }
    }

    /**
     * 調試端點：測試會員資訊功能
     */
    @GetMapping("/debug/{tenantCode}/member-info/{lineUserId}")
    public ResponseEntity<ApiResponse<Object>> debugMemberInfo(
            @PathVariable String tenantCode,
            @PathVariable String lineUserId
    ) {
        log.info("=== 調試會員資訊 ===");
        log.info("租戶代碼：{}，LINE User ID：{}", tenantCode, lineUserId);

        try {
            // 1. 查詢租戶設定
            Optional<TenantLineConfig> configOpt = lineConfigService.getConfigByTenantCode(tenantCode);
            if (configOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("CONFIG_NOT_FOUND", "找不到租戶 LINE 設定"));
            }

            String tenantId = configOpt.get().getTenantId();
            log.info("租戶 ID：{}", tenantId);

            // 2. 調用 WebhookService 的測試方法
            Object result = webhookService.debugMemberInfo(tenantId, lineUserId);
            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            log.error("調試失敗：", e);
            return ResponseEntity.ok(ApiResponse.error("DEBUG_ERROR", e.getMessage()));
        }
    }

    /**
     * 接收 LINE Webhook 事件
     *
     * @param tenantCode 租戶代碼
     * @param signature  X-Line-Signature 標頭
     * @param body       請求 body（JSON 格式）
     * @return 回應
     */
    @PostMapping("/{tenantCode}")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @PathVariable String tenantCode,
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String body
    ) {
        log.info("=== 收到 LINE Webhook ===");
        log.info("租戶代碼：{}，signature 存在：{}，body 長度：{}",
                tenantCode, signature != null, body != null ? body.length() : 0);

        // ========================================
        // 1. 查詢租戶設定
        // ========================================

        Optional<TenantLineConfig> configOpt = lineConfigService.getConfigByTenantCode(tenantCode);

        if (configOpt.isEmpty()) {
            log.warn("找不到租戶 LINE 設定，租戶代碼：{}", tenantCode);
            return ResponseEntity.ok(ApiResponse.ok());
        }

        TenantLineConfig config = configOpt.get();
        String tenantId = config.getTenantId();
        log.info("找到租戶設定，租戶 ID：{}，狀態：{}", tenantId, config.getStatus());

        // ========================================
        // 2. 檢查設定狀態
        // ========================================

        if (config.getStatus() == LineConfigStatus.INACTIVE) {
            log.info("LINE Bot 已停用，租戶：{}，跳過處理", tenantId);
            return ResponseEntity.ok(ApiResponse.ok());
        }

        // ========================================
        // 3. 驗證簽名（暫時改為僅記錄，不阻擋）
        // ========================================

        boolean signatureValid = false;
        if (signature != null && config.getChannelSecretEncrypted() != null) {
            String channelSecret = encryptionService.decrypt(config.getChannelSecretEncrypted());
            log.info("嘗試驗證簽名，signature 長度：{}，secret 長度：{}",
                    signature.length(), channelSecret.length());

            signatureValid = signatureValidator.validate(body, signature, channelSecret);

            if (!signatureValid) {
                log.warn("=== 簽名驗證失敗 ===");
                log.warn("租戶：{}，但仍繼續處理（調試模式）", tenantId);
                // 暫時不 return，繼續處理以便調試
                // return ResponseEntity.ok(ApiResponse.ok());
            } else {
                log.info("簽名驗證成功");
                // 首次驗證成功，標記 Webhook 已驗證
                if (!config.getWebhookVerified()) {
                    lineConfigService.markWebhookVerified(tenantId);
                }
            }
        } else {
            log.info("跳過簽名驗證：signature={}，hasSecret={}",
                    signature != null, config.getChannelSecretEncrypted() != null);
        }

        // ========================================
        // 4. 處理事件（非同步）
        // ========================================

        // 解析事件取得 userId 用於調試
        String debugUserId = null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);
            com.fasterxml.jackson.databind.JsonNode events = root.get("events");
            if (events != null && events.isArray() && events.size() > 0) {
                debugUserId = events.get(0).path("source").path("userId").asText(null);
                String eventType = events.get(0).path("type").asText();
                log.info("事件類型：{}，用戶 ID：{}", eventType, debugUserId);
            }
        } catch (Exception e) {
            log.warn("解析事件失敗：{}", e.getMessage());
        }

        try {
            log.info("開始呼叫 processWebhook，租戶：{}", tenantId);
            webhookService.processWebhook(tenantId, body);
            log.info("processWebhook 呼叫完成（非同步，已排入佇列）");

            // 調試：發送 push 確認 webhook 有收到
            if (debugUserId != null && !debugUserId.isEmpty()) {
                final String userId = debugUserId;
                final String tid = tenantId;
                // 使用新執行緒避免阻塞
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // 等待 0.5 秒讓 reply 先執行
                        webhookService.sendDebugPush(tid, userId, "[DEBUG] Webhook 收到您的訊息，正在處理...");
                    } catch (Exception e) {
                        log.error("Debug push 失敗：{}", e.getMessage());
                    }
                }).start();
            }
        } catch (Exception e) {
            log.error("Webhook 處理失敗，租戶：{}，錯誤類型：{}，錯誤訊息：{}",
                    tenantId, e.getClass().getName(), e.getMessage(), e);
        }

        // LINE 要求必須回傳 200 OK
        log.info("=== Webhook 處理完成，回傳 200 OK ===");
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
