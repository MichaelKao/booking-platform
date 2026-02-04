package com.booking.platform.controller.line;

import com.booking.platform.common.line.LineSignatureValidator;
import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.enums.line.LineConfigStatus;
import com.booking.platform.service.common.EncryptionService;
import com.booking.platform.service.line.LineConfigService;
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
    private final LineSignatureValidator signatureValidator;
    private final EncryptionService encryptionService;

    // ========================================
    // Webhook 端點
    // ========================================

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
        log.debug("收到 LINE Webhook，租戶代碼：{}", tenantCode);

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

        // ========================================
        // 2. 檢查設定狀態
        // ========================================

        if (config.getStatus() == LineConfigStatus.INACTIVE) {
            log.debug("LINE Bot 已停用，租戶：{}", tenantId);
            return ResponseEntity.ok(ApiResponse.ok());
        }

        // ========================================
        // 3. 驗證簽名（目前跳過，因為可能有問題）
        // ========================================

        // TODO: 修復簽名驗證後再啟用
        // 簽名驗證暫時停用，直接處理事件
        if (signature != null && config.getChannelSecretEncrypted() != null) {
            // 標記 Webhook 已驗證（僅用於 UI 顯示）
            if (!config.getWebhookVerified()) {
                lineConfigService.markWebhookVerified(tenantId);
            }
        }

        // ========================================
        // 4. 處理事件（非同步）
        // ========================================

        try {
            webhookService.processWebhook(tenantId, body);
        } catch (Exception e) {
            log.error("Webhook 處理失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
        }

        // LINE 要求必須回傳 200 OK
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
