package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateMarketingPushRequest;
import com.booking.platform.dto.response.MarketingPushResponse;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.entity.marketing.MarketingPush;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.MarketingPushStatus;
import com.booking.platform.enums.MarketingPushTargetType;
import com.booking.platform.repository.MarketingPushRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.LineUserRepository;
import com.booking.platform.service.line.LineMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行銷推播服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MarketingService {

    private final MarketingPushRepository marketingPushRepository;
    private final TenantRepository tenantRepository;
    private final LineUserRepository lineUserRepository;
    private final LineMessageService lineMessageService;
    private final ObjectMapper objectMapper;

    // ========================================
    // 推播管理
    // ========================================

    /**
     * 建立推播
     */
    @Transactional
    public MarketingPushResponse createPush(CreateMarketingPushRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.debug("建立行銷推播，租戶 ID：{}", tenantId);

        // 估算發送人數
        int estimatedCount = estimateTargetCount(tenantId, request.getTargetType(), request.getTargetValue());

        // 建立實體
        MarketingPush push = MarketingPush.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .targetType(request.getTargetType())
                .targetValue(request.getTargetValue())
                .estimatedCount(estimatedCount)
                .note(request.getNote())
                .build();

        push.setTenantId(tenantId);

        // 處理自訂名單
        if (request.getCustomTargets() != null && !request.getCustomTargets().isEmpty()) {
            try {
                push.setCustomTargets(objectMapper.writeValueAsString(request.getCustomTargets()));
            } catch (JsonProcessingException e) {
                log.error("序列化自訂名單失敗", e);
            }
        }

        // 設定排程
        if (request.getScheduledAt() != null) {
            push.schedule(request.getScheduledAt());
        }

        marketingPushRepository.save(push);
        log.info("建立行銷推播成功，ID：{}，標題：{}", push.getId(), push.getTitle());

        return toResponse(push);
    }

    /**
     * 取得推播列表
     */
    public PageResponse<MarketingPushResponse> getPushList(MarketingPushStatus status, int page, int size) {
        String tenantId = TenantContext.getTenantId();

        Page<MarketingPush> pushPage = marketingPushRepository.findByTenantIdAndFilters(
                tenantId,
                status,
                PageRequest.of(page, size)
        );

        List<MarketingPushResponse> responses = pushPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<MarketingPushResponse>builder()
                .content(responses)
                .page(pushPage.getNumber())
                .size(pushPage.getSize())
                .totalElements(pushPage.getTotalElements())
                .totalPages(pushPage.getTotalPages())
                .first(pushPage.isFirst())
                .last(pushPage.isLast())
                .build();
    }

    /**
     * 取得推播詳情
     */
    public MarketingPushResponse getPush(String id) {
        String tenantId = TenantContext.getTenantId();

        MarketingPush push = marketingPushRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到推播"));

        return toResponse(push);
    }

    /**
     * 立即發送推播
     */
    @Transactional
    public MarketingPushResponse sendPush(String id) {
        String tenantId = TenantContext.getTenantId();

        MarketingPush push = marketingPushRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到推播"));

        if (!push.isSendable()) {
            throw new BusinessException(ErrorCode.SYS_INVALID_OPERATION, "此推播無法發送");
        }

        // 檢查推送額度
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TENANT_NOT_FOUND, "找不到店家"));

        int estimatedCount = push.getEstimatedCount() != null ? push.getEstimatedCount() : 0;
        if (!tenant.hasPushQuota(estimatedCount)) {
            throw new BusinessException(ErrorCode.POINT_INSUFFICIENT, "推送額度不足");
        }

        // 開始發送
        push.startSending();
        marketingPushRepository.save(push);

        // 非同步發送
        executePushAsync(push, tenant);

        return toResponse(push);
    }

    /**
     * 取消推播
     */
    @Transactional
    public MarketingPushResponse cancelPush(String id) {
        String tenantId = TenantContext.getTenantId();

        MarketingPush push = marketingPushRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到推播"));

        if (!push.isCancellable()) {
            throw new BusinessException(ErrorCode.SYS_INVALID_OPERATION, "此推播無法取消");
        }

        push.cancel();
        marketingPushRepository.save(push);

        log.info("取消行銷推播，ID：{}", id);

        return toResponse(push);
    }

    /**
     * 刪除推播
     */
    @Transactional
    public void deletePush(String id) {
        String tenantId = TenantContext.getTenantId();

        MarketingPush push = marketingPushRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到推播"));

        if (!push.isCancellable()) {
            throw new BusinessException(ErrorCode.SYS_INVALID_OPERATION, "此推播無法刪除");
        }

        push.softDelete();
        marketingPushRepository.save(push);

        log.info("刪除行銷推播，ID：{}", id);
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 估算目標人數
     */
    private int estimateTargetCount(String tenantId, MarketingPushTargetType targetType, String targetValue) {
        return switch (targetType) {
            case ALL -> (int) lineUserRepository.countByTenantIdAndIsFollowedAndDeletedAtIsNull(tenantId, true);
            case MEMBERSHIP_LEVEL -> (int) lineUserRepository.countByTenantIdAndMembershipLevelAndIsFollowedAndDeletedAtIsNull(
                    tenantId, targetValue, true);
            case TAG -> (int) lineUserRepository.countByTenantIdAndTagAndIsFollowedAndDeletedAtIsNull(
                    tenantId, targetValue);
            case CUSTOM -> 0; // 自訂名單需要實際解析
        };
    }

    /**
     * 非同步執行推播
     */
    @Async("notificationExecutor")
    public void executePushAsync(MarketingPush push, Tenant tenant) {
        try {
            String tenantId = push.getTenantId();

            // 取得目標用戶
            List<LineUser> targetUsers = getTargetUsers(push);

            log.info("開始發送行銷推播，ID：{}，目標人數：{}", push.getId(), targetUsers.size());

            int successCount = 0;
            int failCount = 0;

            for (LineUser user : targetUsers) {
                try {
                    // 發送文字訊息
                    lineMessageService.pushText(tenantId, user.getLineUserId(), push.getContent());
                    successCount++;
                } catch (Exception e) {
                    log.warn("發送推播失敗，LINE User ID：{}，錯誤：{}", user.getLineUserId(), e.getMessage());
                    failCount++;
                }
            }

            // 更新推播狀態
            push.complete(successCount, failCount);
            marketingPushRepository.save(push);

            // 扣除推送額度
            tenant.usePushQuota(successCount);
            tenantRepository.save(tenant);

            log.info("行銷推播發送完成，ID：{}，成功：{}，失敗：{}", push.getId(), successCount, failCount);

        } catch (Exception e) {
            log.error("行銷推播執行失敗，ID：{}，錯誤：{}", push.getId(), e.getMessage(), e);
            push.markFailed(e.getMessage());
            marketingPushRepository.save(push);
        }
    }

    /**
     * 取得目標用戶
     */
    private List<LineUser> getTargetUsers(MarketingPush push) {
        String tenantId = push.getTenantId();

        return switch (push.getTargetType()) {
            case ALL -> lineUserRepository.findByTenantIdAndIsFollowedAndDeletedAtIsNull(tenantId, true);
            case MEMBERSHIP_LEVEL -> lineUserRepository.findByTenantIdAndMembershipLevelAndIsFollowedAndDeletedAtIsNull(
                    tenantId, push.getTargetValue(), true);
            case CUSTOM -> {
                if (push.getCustomTargets() != null) {
                    try {
                        List<String> lineUserIds = objectMapper.readValue(
                                push.getCustomTargets(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                        );
                        yield lineUserRepository.findByTenantIdAndLineUserIdInAndDeletedAtIsNull(tenantId, lineUserIds);
                    } catch (JsonProcessingException e) {
                        log.error("解析自訂名單失敗", e);
                        yield List.of();
                    }
                }
                yield List.of();
            }
            case TAG -> lineUserRepository.findByTenantIdAndTagAndIsFollowedAndDeletedAtIsNull(
                    tenantId, push.getTargetValue());
        };
    }

    /**
     * 轉換為回應 DTO
     */
    private MarketingPushResponse toResponse(MarketingPush push) {
        return MarketingPushResponse.builder()
                .id(push.getId())
                .title(push.getTitle())
                .content(push.getContent())
                .imageUrl(push.getImageUrl())
                .targetType(push.getTargetType())
                .targetValue(push.getTargetValue())
                .estimatedCount(push.getEstimatedCount())
                .status(push.getStatus())
                .scheduledAt(push.getScheduledAt())
                .sentAt(push.getSentAt())
                .completedAt(push.getCompletedAt())
                .sentCount(push.getSentCount())
                .failedCount(push.getFailedCount())
                .errorMessage(push.getErrorMessage())
                .note(push.getNote())
                .createdAt(push.getCreatedAt())
                .updatedAt(push.getUpdatedAt())
                .build();
    }
}
