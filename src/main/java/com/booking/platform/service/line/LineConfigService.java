package com.booking.platform.service.line;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.line.LineConfigResponse;
import com.booking.platform.dto.line.SaveLineConfigRequest;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.line.LineConfigStatus;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.common.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * LINE 設定服務
 *
 * <p>管理店家的 LINE Bot 設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LineConfigService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantLineConfigRepository lineConfigRepository;
    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;

    @Value("${server.port:8080}")
    private int serverPort;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得當前租戶的 LINE 設定
     *
     * @return LINE 設定回應
     */
    public LineConfigResponse getConfig() {
        String tenantId = TenantContext.getTenantId();
        return getConfigByTenantId(tenantId);
    }

    /**
     * 根據租戶 ID 取得 LINE 設定
     *
     * @param tenantId 租戶 ID
     * @return LINE 設定回應
     */
    public LineConfigResponse getConfigByTenantId(String tenantId) {
        // ========================================
        // 1. 查詢設定
        // ========================================

        Optional<TenantLineConfig> configOpt = lineConfigRepository.findByTenantId(tenantId);

        // ========================================
        // 2. 如果不存在，返回空設定
        // ========================================

        if (configOpt.isEmpty()) {
            return buildEmptyResponse(tenantId);
        }

        // ========================================
        // 3. 轉換為回應
        // ========================================

        TenantLineConfig config = configOpt.get();
        return buildResponse(config, tenantId);
    }

    /**
     * 根據租戶代碼取得 LINE 設定（用於 Webhook）
     *
     * @param tenantCode 租戶代碼
     * @return LINE 設定（可能為空）
     */
    public Optional<TenantLineConfig> getConfigByTenantCode(String tenantCode) {
        // ========================================
        // 1. 查詢租戶
        // ========================================

        Optional<Tenant> tenantOpt = tenantRepository.findByCodeAndDeletedAtIsNull(tenantCode);
        if (tenantOpt.isEmpty()) {
            return Optional.empty();
        }

        // ========================================
        // 2. 查詢設定
        // ========================================

        return lineConfigRepository.findByTenantId(tenantOpt.get().getId());
    }

    /**
     * 取得解密後的 Channel Secret
     *
     * @param tenantId 租戶 ID
     * @return Channel Secret
     */
    public String getDecryptedChannelSecret(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "LINE 設定不存在"
                ));

        if (config.getChannelSecretEncrypted() == null) {
            return null;
        }

        return encryptionService.decrypt(config.getChannelSecretEncrypted());
    }

    /**
     * 取得解密後的 Access Token
     *
     * @param tenantId 租戶 ID
     * @return Access Token
     */
    public String getDecryptedAccessToken(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "LINE 設定不存在"
                ));

        if (config.getChannelAccessTokenEncrypted() == null) {
            return null;
        }

        return encryptionService.decrypt(config.getChannelAccessTokenEncrypted());
    }

    // ========================================
    // 寫入方法
    // ========================================

    /**
     * 儲存 LINE 設定
     *
     * @param request 儲存請求
     * @return LINE 設定回應
     */
    @Transactional
    public LineConfigResponse saveConfig(SaveLineConfigRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("儲存 LINE 設定，租戶：{}", tenantId);

        // ========================================
        // 1. 驗證 Channel ID 是否重複
        // ========================================

        if (request.getChannelId() != null && !request.getChannelId().isEmpty()) {
            if (lineConfigRepository.existsByChannelIdAndTenantIdNot(request.getChannelId(), tenantId)) {
                throw new BusinessException(
                        ErrorCode.LINE_CHANNEL_ID_DUPLICATE,
                        "此 Channel ID 已被其他店家使用"
                );
            }
        }

        // ========================================
        // 2. 查詢或建立設定
        // ========================================

        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantLineConfig newConfig = TenantLineConfig.builder()
                            .tenantId(tenantId)
                            .status(LineConfigStatus.PENDING)
                            .build();
                    return newConfig;
                });

        // ========================================
        // 3. 更新基本資訊
        // ========================================

        if (request.getChannelId() != null) {
            config.setChannelId(request.getChannelId());
        }

        // ========================================
        // 4. 加密並更新敏感資訊
        // ========================================

        if (request.hasChannelSecretUpdate()) {
            String encrypted = encryptionService.encrypt(request.getChannelSecret());
            config.setChannelSecretEncrypted(encrypted);
        }

        if (request.hasAccessTokenUpdate()) {
            String encrypted = encryptionService.encrypt(request.getChannelAccessToken());
            config.setChannelAccessTokenEncrypted(encrypted);
        }

        // ========================================
        // 5. 更新訊息設定
        // ========================================

        if (request.getWelcomeMessage() != null) {
            config.setWelcomeMessage(request.getWelcomeMessage());
        }

        if (request.getDefaultReply() != null) {
            config.setDefaultReply(request.getDefaultReply());
        }

        if (request.getAutoReplyEnabled() != null) {
            config.setAutoReplyEnabled(request.getAutoReplyEnabled());
        }

        if (request.getBookingEnabled() != null) {
            config.setBookingEnabled(request.getBookingEnabled());
        }

        // ========================================
        // 6. 更新狀態和 Webhook URL
        // ========================================

        updateWebhookUrl(config, tenantId);
        updateConfigStatus(config);

        // ========================================
        // 7. 儲存
        // ========================================

        config = lineConfigRepository.save(config);

        log.info("LINE 設定儲存成功，租戶：{}，狀態：{}", tenantId, config.getStatus());

        return buildResponse(config, tenantId);
    }

    /**
     * 啟用 LINE Bot
     *
     * @return LINE 設定回應
     */
    @Transactional
    public LineConfigResponse activate() {
        String tenantId = TenantContext.getTenantId();

        log.info("啟用 LINE Bot，租戶：{}", tenantId);

        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "請先完成 LINE 設定"
                ));

        // 檢查設定是否完整
        if (!config.isConfigured() && config.getChannelSecretEncrypted() == null) {
            throw new BusinessException(
                    ErrorCode.LINE_CONFIG_INVALID,
                    "請先完成 LINE Channel 設定"
            );
        }

        config.activate();
        config = lineConfigRepository.save(config);

        log.info("LINE Bot 啟用成功，租戶：{}", tenantId);

        return buildResponse(config, tenantId);
    }

    /**
     * 停用 LINE Bot
     *
     * @return LINE 設定回應
     */
    @Transactional
    public LineConfigResponse deactivate() {
        String tenantId = TenantContext.getTenantId();

        log.info("停用 LINE Bot，租戶：{}", tenantId);

        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "LINE 設定不存在"
                ));

        config.deactivate();
        config = lineConfigRepository.save(config);

        log.info("LINE Bot 停用成功，租戶：{}", tenantId);

        return buildResponse(config, tenantId);
    }

    /**
     * 標記 Webhook 驗證成功
     *
     * @param tenantId 租戶 ID
     */
    @Transactional
    public void markWebhookVerified(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElse(null);

        if (config != null) {
            config.markAsVerified();
            lineConfigRepository.save(config);
            log.info("Webhook 驗證成功，租戶：{}", tenantId);
        }
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 更新 Webhook URL
     */
    private void updateWebhookUrl(TenantLineConfig config, String tenantId) {
        // 查詢租戶代碼
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isPresent()) {
            String tenantCode = tenantOpt.get().getCode();
            // TODO: 正式環境使用 HTTPS 和正確的域名
            String webhookUrl = String.format("https://your-domain.com/api/line/webhook/%s", tenantCode);
            config.setWebhookUrl(webhookUrl);
        }
    }

    /**
     * 更新設定狀態
     */
    private void updateConfigStatus(TenantLineConfig config) {
        boolean hasChannel = config.getChannelId() != null && !config.getChannelId().isEmpty();
        boolean hasSecret = config.getChannelSecretEncrypted() != null;
        boolean hasToken = config.getChannelAccessTokenEncrypted() != null;

        if (hasChannel && hasSecret && hasToken) {
            // 設定完整，設為驗證中（等待 Webhook 驗證）
            if (config.getStatus() == LineConfigStatus.PENDING) {
                config.setStatus(LineConfigStatus.VERIFYING);
            }
        } else {
            // 設定不完整
            config.setStatus(LineConfigStatus.PENDING);
        }
    }

    /**
     * 建立空回應
     */
    private LineConfigResponse buildEmptyResponse(String tenantId) {
        // 查詢租戶取得推送額度
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        Integer monthlyQuota = tenant != null ? tenant.getMonthlyPushQuota() : 100;

        return LineConfigResponse.builder()
                .tenantId(tenantId)
                .hasChannelSecret(false)
                .hasAccessToken(false)
                .webhookVerified(false)
                .status(LineConfigStatus.PENDING)
                .statusDescription(LineConfigResponse.getStatusDescription(LineConfigStatus.PENDING))
                .autoReplyEnabled(true)
                .bookingEnabled(true)
                .monthlyPushCount(0)
                .monthlyPushQuota(monthlyQuota)
                .remainingPushQuota(monthlyQuota)
                .build();
    }

    /**
     * 建立回應
     */
    private LineConfigResponse buildResponse(TenantLineConfig config, String tenantId) {
        // 查詢租戶取得推送額度
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        Integer monthlyQuota = tenant != null ? tenant.getMonthlyPushQuota() : 100;
        Integer remaining = monthlyQuota - (config.getMonthlyPushCount() != null ? config.getMonthlyPushCount() : 0);

        return LineConfigResponse.builder()
                .tenantId(config.getTenantId())
                .channelId(config.getChannelId())
                .hasChannelSecret(config.getChannelSecretEncrypted() != null)
                .hasAccessToken(config.getChannelAccessTokenEncrypted() != null)
                .webhookUrl(config.getWebhookUrl())
                .webhookVerified(config.getWebhookVerified())
                .lastVerifiedAt(config.getLastVerifiedAt())
                .status(config.getStatus())
                .statusDescription(LineConfigResponse.getStatusDescription(config.getStatus()))
                .welcomeMessage(config.getWelcomeMessage())
                .defaultReply(config.getDefaultReply())
                .autoReplyEnabled(config.getAutoReplyEnabled())
                .bookingEnabled(config.getBookingEnabled())
                .monthlyPushCount(config.getMonthlyPushCount())
                .monthlyPushQuota(monthlyQuota)
                .remainingPushQuota(remaining)
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
