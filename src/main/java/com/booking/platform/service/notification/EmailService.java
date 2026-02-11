package com.booking.platform.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * 郵件發送服務
 *
 * <p>使用 Resend HTTP API 發送郵件，支援 HTML 模板
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    // ========================================
    // 設定值
    // ========================================

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.email.resend-api-key:}")
    private String resendApiKey;

    // ========================================
    // 公開方法
    // ========================================

    /**
     * 發送密碼重設郵件
     *
     * @param toEmail 收件人信箱
     * @param tenantName 店家名稱
     * @param resetToken 重設 Token
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String tenantName, String resetToken) {
        log.info("發送密碼重設郵件，收件人：{}，店家：{}", toEmail, tenantName);

        String resetUrl = baseUrl + "/tenant/reset-password?token=" + resetToken;

        // 建立模板變數
        Map<String, Object> variables = Map.of(
                "tenantName", tenantName,
                "resetUrl", resetUrl,
                "expiryHours", 1
        );

        // 發送郵件
        sendHtmlEmail(
                toEmail,
                "【預約平台】密碼重設請求",
                "email/password-reset",
                variables
        );
    }

    /**
     * 發送歡迎郵件（註冊成功）
     *
     * @param toEmail 收件人信箱
     * @param tenantName 店家名稱
     * @param tenantCode 店家代碼
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String tenantName, String tenantCode) {
        log.info("發送歡迎郵件，收件人：{}，店家：{}", toEmail, tenantName);

        String loginUrl = baseUrl + "/tenant/login";

        // 建立模板變數
        Map<String, Object> variables = Map.of(
                "tenantName", tenantName,
                "tenantCode", tenantCode,
                "loginUrl", loginUrl
        );

        // 發送郵件
        sendHtmlEmail(
                toEmail,
                "【預約平台】歡迎加入！您的店家帳號已建立",
                "email/welcome",
                variables
        );
    }

    /**
     * 發送密碼變更通知
     *
     * @param toEmail 收件人信箱
     * @param tenantName 店家名稱
     */
    @Async
    public void sendPasswordChangedEmail(String toEmail, String tenantName) {
        log.info("發送密碼變更通知，收件人：{}，店家：{}", toEmail, tenantName);

        // 建立模板變數
        Map<String, Object> variables = Map.of(
                "tenantName", tenantName,
                "loginUrl", baseUrl + "/tenant/login"
        );

        // 發送郵件
        sendHtmlEmail(
                toEmail,
                "【預約平台】密碼已變更",
                "email/password-changed",
                variables
        );
    }

    /**
     * 測試郵件發送（同步，回傳結果）
     */
    public String testSendEmail(String toEmail) {
        try {
            String html = "<h2>郵件測試成功！</h2><p>如果您看到這封信，表示郵件功能正常運作。</p>";
            sendViaResend(toEmail, "【預約平台】郵件測試", html);
            return "SUCCESS: 郵件已發送到 " + toEmail;
        } catch (Exception e) {
            log.error("測試郵件發送失敗：{}", e.getMessage(), e);
            return "FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    /**
     * 發送純文字郵件
     *
     * @param to 收件人
     * @param subject 主旨
     * @param text 內容
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            sendViaResend(to, subject, text);
            log.info("純文字郵件發送成功，收件人：{}，主旨：{}", to, subject);
        } catch (Exception e) {
            log.error("純文字郵件發送失敗，收件人：{}，錯誤：{}", to, e.getMessage(), e);
        }
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 發送 HTML 郵件
     */
    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 處理模板
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            // 透過 Resend API 發送
            sendViaResend(to, subject, htmlContent);
            log.info("郵件發送成功，收件人：{}，主旨：{}", to, subject);

        } catch (Exception e) {
            log.error("郵件發送失敗，收件人：{}，錯誤：{}", to, e.getMessage(), e);
        }
    }

    /**
     * 透過 Resend HTTP API 發送郵件
     */
    private void sendViaResend(String to, String subject, String htmlContent) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend API Key 未設定，跳過郵件發送");
            return;
        }

        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        // 組合寄件人
        String from = fromName + " <" + fromEmail + ">";

        // 建立請求 body
        String body = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":%s}",
                escapeJson(from),
                escapeJson(to),
                escapeJson(subject),
                toJsonString(htmlContent)
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend API 回傳錯誤: " + response.getStatusCode() + " - " + response.getBody());
        }

        log.debug("Resend API 回傳：{}", response.getBody());
    }

    /**
     * JSON 字串跳脫
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 將字串轉為 JSON 字串格式（含雙引號和跳脫）
     */
    private String toJsonString(String value) {
        if (value == null) return "\"\"";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
