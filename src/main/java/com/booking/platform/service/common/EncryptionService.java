package com.booking.platform.service.common;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密服務
 *
 * <p>使用 AES-256-GCM 加密演算法保護敏感資料
 *
 * <p>特性：
 * <ul>
 *   <li>AES-256-GCM 提供加密和認證</li>
 *   <li>每次加密使用隨機 IV（Initialization Vector）</li>
 *   <li>IV 會附加在加密資料前面</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@Slf4j
public class EncryptionService {

    // ========================================
    // 常數
    // ========================================

    /**
     * 加密演算法
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * 金鑰演算法
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * GCM 認證標籤長度（bits）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * IV 長度（bytes）
     */
    private static final int IV_LENGTH = 12;

    // ========================================
    // 依賴
    // ========================================

    /**
     * 加密金鑰（Base64 編碼）
     */
    @Value("${encryption.secret-key}")
    private String secretKeyBase64;

    /**
     * 解碼後的金鑰
     */
    private SecretKey secretKey;

    /**
     * 安全亂數產生器
     */
    private final SecureRandom secureRandom = new SecureRandom();

    // ========================================
    // 初始化
    // ========================================

    /**
     * 初始化加密服務
     */
    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);

            // 驗證金鑰長度
            if (keyBytes.length != 32) {
                log.warn("加密金鑰長度不是 32 bytes，將使用前 32 bytes 或補齊");
                byte[] adjustedKey = new byte[32];
                System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = adjustedKey;
            }

            this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            log.info("加密服務初始化成功");
        } catch (Exception e) {
            log.error("加密服務初始化失敗", e);
            throw new RuntimeException("加密服務初始化失敗", e);
        }
    }

    // ========================================
    // 公開方法
    // ========================================

    /**
     * 加密字串
     *
     * @param plainText 明文
     * @return 加密後的 Base64 字串（包含 IV）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // ========================================
            // 1. 產生隨機 IV
            // ========================================

            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // ========================================
            // 2. 初始化加密器
            // ========================================

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // ========================================
            // 3. 加密
            // ========================================

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            // ========================================
            // 4. 組合 IV + 密文
            // ========================================

            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encryptedBytes.length);
            buffer.put(iv);
            buffer.put(encryptedBytes);

            // ========================================
            // 5. Base64 編碼
            // ========================================

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            log.error("加密失敗", e);
            throw new BusinessException(ErrorCode.LINE_ENCRYPTION_ERROR, "加密處理失敗");
        }
    }

    /**
     * 解密字串
     *
     * @param encryptedText 加密後的 Base64 字串（包含 IV）
     * @return 解密後的明文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            // ========================================
            // 1. Base64 解碼
            // ========================================

            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            // ========================================
            // 2. 分離 IV 和密文
            // ========================================

            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            // ========================================
            // 3. 初始化解密器
            // ========================================

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // ========================================
            // 4. 解密
            // ========================================

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
            log.error("解密失敗", e);
            throw new BusinessException(ErrorCode.LINE_ENCRYPTION_ERROR, "解密處理失敗");
        }
    }

    /**
     * 驗證加密是否正常運作
     *
     * @return true 表示正常
     */
    public boolean verify() {
        try {
            String testText = "encryption-test-" + System.currentTimeMillis();
            String encrypted = encrypt(testText);
            String decrypted = decrypt(encrypted);
            return testText.equals(decrypted);
        } catch (Exception e) {
            log.error("加密驗證失敗", e);
            return false;
        }
    }
}
