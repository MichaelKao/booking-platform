package com.booking.platform.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * 郵件發送服務
 *
 * <p>使用 Spring Mail 發送郵件，支援 HTML 模板
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

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // ========================================
    // 設定值
    // ========================================

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.base-url}")
    private String baseUrl;

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

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 發送 HTML 郵件
     *
     * @param to 收件人
     * @param subject 主旨
     * @param templateName 模板名稱（不含 .html）
     * @param variables 模板變數
     */
    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 處理模板
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            // 建立郵件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // 發送
            mailSender.send(message);
            log.info("郵件發送成功，收件人：{}，主旨：{}", to, subject);

        } catch (MessagingException e) {
            log.error("郵件發送失敗，收件人：{}，錯誤：{}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("郵件發送異常，收件人：{}，錯誤：{}", to, e.getMessage(), e);
        }
    }

    /**
     * 測試郵件發送（同步，回傳結果）
     */
    public String testSendEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("【預約平台】郵件測試");
            helper.setText("<h2>郵件測試成功！</h2><p>如果您看到這封信，表示郵件功能正常運作。</p>", true);
            mailSender.send(message);
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(message);
            log.info("純文字郵件發送成功，收件人：{}，主旨：{}", to, subject);

        } catch (Exception e) {
            log.error("純文字郵件發送失敗，收件人：{}，錯誤：{}", to, e.getMessage(), e);
        }
    }
}
