package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateCampaignRequest;
import com.booking.platform.dto.response.CampaignResponse;
import com.booking.platform.entity.marketing.Campaign;
import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
import com.booking.platform.mapper.CampaignMapper;
import com.booking.platform.repository.CampaignRepository;
import com.booking.platform.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行銷活動服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CouponRepository couponRepository;
    private final CampaignMapper campaignMapper;
    private final CampaignPushService campaignPushService;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<CampaignResponse> getList(
            CampaignStatus status,
            CampaignType type,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<Campaign> page = campaignRepository.findByTenantIdAndFilters(
                tenantId, status, type, pageable
        );

        List<CampaignResponse> content = page.getContent().stream()
                .map(c -> {
                    String couponName = null;
                    if (c.getCouponId() != null) {
                        couponName = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(c.getCouponId(), tenantId)
                                .map(Coupon::getName)
                                .orElse(null);
                    }
                    return campaignMapper.toResponse(c, couponName);
                })
                .collect(Collectors.toList());

        return PageResponse.<CampaignResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public CampaignResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        String couponName = null;
        if (entity.getCouponId() != null) {
            couponName = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(entity.getCouponId(), tenantId)
                    .map(Coupon::getName)
                    .orElse(null);
        }

        return campaignMapper.toResponse(entity, couponName);
    }

    public List<CampaignResponse> getActiveCampaigns() {
        String tenantId = TenantContext.getTenantId();

        return campaignRepository.findActiveByTenantId(tenantId, LocalDateTime.now())
                .stream()
                .map(campaignMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立行銷活動，租戶：{}，名稱：{}", tenantId, request.getName());

        // 檢查名稱是否重複
        if (campaignRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NAME_DUPLICATE, "活動名稱已存在");
        }

        // 驗證活動期間：開始時間不能晚於結束時間
        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "活動開始時間必須早於結束時間");
        }

        // 驗證關聯票券
        if (request.getCouponId() != null) {
            if (!couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getCouponId(), tenantId).isPresent()) {
                throw new ResourceNotFoundException(ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券");
            }
        }

        Campaign entity = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .status(CampaignStatus.DRAFT)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .imageUrl(request.getImageUrl())
                .thresholdAmount(request.getThresholdAmount())
                .recallDays(request.getRecallDays())
                .couponId(request.getCouponId())
                .bonusPoints(request.getBonusPoints())
                .pushMessage(request.getPushMessage())
                .isAutoTrigger(request.getIsAutoTrigger())
                .note(request.getNote())
                .build();

        entity.setTenantId(tenantId);
        entity = campaignRepository.save(entity);

        log.info("行銷活動建立成功，ID：{}", entity.getId());

        return campaignMapper.toResponse(entity);
    }

    @Transactional
    public CampaignResponse update(String id, CreateCampaignRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新行銷活動，ID：{}", id);

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        // 檢查名稱是否重複
        if (campaignRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                tenantId, request.getName(), id)) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NAME_DUPLICATE, "活動名稱已存在");
        }

        // 驗證活動期間：開始時間不能晚於結束時間
        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "活動開始時間必須早於結束時間");
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setImageUrl(request.getImageUrl());
        entity.setThresholdAmount(request.getThresholdAmount());
        entity.setRecallDays(request.getRecallDays());
        entity.setCouponId(request.getCouponId());
        entity.setBonusPoints(request.getBonusPoints());
        entity.setPushMessage(request.getPushMessage());
        entity.setIsAutoTrigger(request.getIsAutoTrigger());
        entity.setNote(request.getNote());

        entity = campaignRepository.save(entity);

        log.info("行銷活動更新成功，ID：{}", entity.getId());

        return campaignMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除行銷活動，ID：{}", id);

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        entity.softDelete();
        campaignRepository.save(entity);

        log.info("行銷活動刪除成功，ID：{}", id);
    }

    // ========================================
    // 狀態操作
    // ========================================

    @Transactional
    public CampaignResponse publish(String id) {
        String tenantId = TenantContext.getTenantId();

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        entity.publish();
        entity = campaignRepository.save(entity);

        log.info("行銷活動已發布，ID：{}", id);

        // 發布後，如果有推播訊息，發送 LINE 通知給所有追蹤者
        if (entity.getPushMessage() != null && !entity.getPushMessage().isBlank()) {
            campaignPushService.sendCampaignPush(tenantId, entity);
        }

        return campaignMapper.toResponse(entity);
    }

    @Transactional
    public CampaignResponse pause(String id) {
        String tenantId = TenantContext.getTenantId();

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        entity.pause();
        entity = campaignRepository.save(entity);

        log.info("行銷活動已暫停，ID：{}", id);

        return campaignMapper.toResponse(entity);
    }

    @Transactional
    public CampaignResponse resume(String id) {
        String tenantId = TenantContext.getTenantId();

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        entity.resume();
        entity = campaignRepository.save(entity);

        log.info("行銷活動已恢復，ID：{}", id);

        return campaignMapper.toResponse(entity);
    }

    @Transactional
    public CampaignResponse end(String id) {
        String tenantId = TenantContext.getTenantId();

        Campaign entity = campaignRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CAMPAIGN_NOT_FOUND, "找不到指定的活動"
                ));

        entity.end();
        entity = campaignRepository.save(entity);

        log.info("行銷活動已結束，ID：{}", id);

        return campaignMapper.toResponse(entity);
    }
}
