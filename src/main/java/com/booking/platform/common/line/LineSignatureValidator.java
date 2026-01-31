package com.booking.platform.common.line;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * LINE 簽名驗證器
 *
 * <p>驗證 LINE Webhook 請求的 X-Line-Signature 標頭
 *
 * <p>驗證流程：
 * <ol>
 *   <li>取得請求 body 和 X-Line-Signature 標頭</li>
 *   <li>使用 Channel Secret 對 body 進行 HMAC-SHA256 簽名</li>
 *   <li>將簽名結果 Base64 編碼</li>
 *   <li>比較計算結果與標頭值</li>
 * </ol>
 *
 * @author Developer
 * @since 1.0.0
 * @see <a href="https://developers.line.biz/en/reference/messaging-api/#signature-validation">LINE Signature Validation</a>
 */
@Component
@Slf4j
public class LineSignatureValidator {

    /**
     * 簽名演算法
     */
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 驗證簽名
     *
     * @param body          請求 body（原始字串）
     * @param signature     X-Line-Signature 標頭值
     * @param channelSecret Channel Secret
     * @return true 表示驗證通過
     */
    public boolean validate(String body, String signature, String channelSecret) {
        if (body == null || signature == null || channelSecret == null) {
            log.warn("簽名驗證失敗：參數不完整");
            return false;
        }

        try {
            // ========================================
            // 1. 計算 HMAC-SHA256 簽名
            // ========================================

            String calculatedSignature = calculateSignature(body, channelSecret);

            // ========================================
            // 2. 比較簽名
            // ========================================

            boolean isValid = constantTimeEquals(signature, calculatedSignature);

            if (!isValid) {
                log.warn("簽名驗證失敗：簽名不符");
            }

            return isValid;

        } catch (Exception e) {
            log.error("簽名驗證發生錯誤", e);
            return false;
        }
    }

    /**
     * 計算簽名
     *
     * @param body          請求 body
     * @param channelSecret Channel Secret
     * @return Base64 編碼的簽名
     * @throws NoSuchAlgorithmException 演算法不存在
     * @throws InvalidKeyException      無效金鑰
     */
    public String calculateSignature(String body, String channelSecret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        // ========================================
        // 1. 建立 HMAC-SHA256 實例
        // ========================================

        SecretKeySpec secretKey = new SecretKeySpec(
                channelSecret.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
        );

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(secretKey);

        // ========================================
        // 2. 計算簽名
        // ========================================

        byte[] signatureBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));

        // ========================================
        // 3. Base64 編碼
        // ========================================

        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * 常數時間比較（防止時序攻擊）
     *
     * @param a 字串 a
     * @param b 字串 b
     * @return true 表示相等
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}
