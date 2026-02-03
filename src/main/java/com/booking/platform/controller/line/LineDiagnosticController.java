package com.booking.platform.controller.line;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.service.common.EncryptionService;
import com.booking.platform.service.line.LineConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LINE Bot 診斷控制器
 */
@RestController
@RequestMapping("/api/line/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class LineDiagnosticController {

    private final LineConfigService lineConfigService;
    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;

    @GetMapping("/{tenantCode}")
    public ApiResponse<Map<String, Object>> diagnose(@PathVariable String tenantCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 檢查設定是否存在（透過 Service 查詢，自動處理 tenantCode -> tenantId 轉換）
            var configOpt = lineConfigService.getConfigByTenantCode(tenantCode);
            if (configOpt.isEmpty()) {
                result.put("configExists", false);
                result.put("error", "找不到 LINE 設定");
                return ApiResponse.ok(result);
            }
            
            TenantLineConfig config = configOpt.get();
            result.put("configExists", true);
            result.put("status", config.getStatus().name());
            result.put("webhookVerified", config.getWebhookVerified());
            result.put("hasChannelId", config.getChannelId() != null);
            result.put("hasChannelSecret", config.getChannelSecretEncrypted() != null);
            result.put("hasAccessToken", config.getChannelAccessTokenEncrypted() != null);
            
            // 2. 測試 Access Token
            if (config.getChannelAccessTokenEncrypted() != null) {
                try {
                    String token = encryptionService.decrypt(config.getChannelAccessTokenEncrypted());
                    result.put("tokenDecrypted", true);
                    result.put("tokenLength", token.length());
                    
                    // 嘗試呼叫 LINE API 驗證 Token
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);
                    HttpEntity<Void> request = new HttpEntity<>(headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(
                        "https://api.line.me/v2/bot/info",
                        HttpMethod.GET,
                        request,
                        String.class
                    );
                    
                    result.put("tokenValid", response.getStatusCode().is2xxSuccessful());
                    result.put("botInfo", response.getBody());
                    
                } catch (Exception e) {
                    result.put("tokenValid", false);
                    result.put("tokenError", e.getMessage());
                }
            }
            
            return ApiResponse.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.ok(result);
        }
    }

    /**
     * 啟用 LINE Bot（除錯用）
     */
    @PostMapping("/{tenantCode}/activate")
    public ApiResponse<Map<String, Object>> activate(@PathVariable String tenantCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 使用 Service 方法來啟用（包含事務處理）
            String previousStatus = lineConfigService.activateByTenantCode(tenantCode);

            result.put("success", true);
            result.put("previousStatus", previousStatus);
            result.put("currentStatus", "ACTIVE");
            result.put("message", "LINE Bot 已啟用");

            return ApiResponse.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return ApiResponse.ok(result);
        }
    }
}
