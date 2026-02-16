package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.dto.response.ReferralDashboardResponse;
import com.booking.platform.dto.response.ReferralResponse;
import com.booking.platform.entity.system.TenantReferral;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.ReferralStatus;
import com.booking.platform.repository.TenantReferralRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 推薦服務
 *
 * <p>處理店家推薦機制：推薦碼生成、推薦記錄、獎勵發放
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReferralService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;
    private final TenantReferralRepository tenantReferralRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 推薦獎勵點數
     */
    private static final int REFERRAL_BONUS_POINTS = 500;

    // ========================================
    // 推薦碼管理
    // ========================================

    /**
     * 取得或生成推薦碼
     *
     * @param tenantId 租戶 ID
     * @return 推薦碼
     */
    @Transactional
    public String getOrGenerateReferralCode(String tenantId) {
        log.debug("取得推薦碼，租戶 ID：{}", tenantId);

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        if (tenant.getReferralCode() != null && !tenant.getReferralCode().isEmpty()) {
            return tenant.getReferralCode();
        }

        // 生成 8 字元推薦碼
        String code = generateUniqueCode();
        tenant.setReferralCode(code);
        tenantRepository.save(tenant);

        log.info("生成推薦碼，租戶：{}，推薦碼：{}", tenantId, code);
        return code;
    }

    // ========================================
    // 推薦儀表板
    // ========================================

    /**
     * 取得推薦儀表板資料
     *
     * @param tenantId 租戶 ID
     * @return 推薦儀表板
     */
    public ReferralDashboardResponse getDashboard(String tenantId) {
        log.debug("取得推薦儀表板，租戶 ID：{}", tenantId);

        // 確保有推薦碼
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));

        String referralCode = tenant.getReferralCode();
        if (referralCode == null || referralCode.isEmpty()) {
            referralCode = getOrGenerateReferralCode(tenantId);
        }

        // 統計資料
        long totalReferrals = tenantReferralRepository.countByReferrerTenantIdAndDeletedAtIsNull(tenantId);
        long completedReferrals = tenantReferralRepository.countByReferrerTenantIdAndStatusAndDeletedAtIsNull(
                tenantId, ReferralStatus.COMPLETED);
        long pendingReferrals = totalReferrals - completedReferrals;

        // 推薦歷史
        List<TenantReferral> referrals = tenantReferralRepository
                .findByReferrerTenantIdAndDeletedAtIsNull(tenantId);

        List<ReferralResponse> history = referrals.stream()
                .map(this::toReferralResponse)
                .collect(Collectors.toList());

        // 累計獲得點數
        int totalBonusPoints = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.COMPLETED)
                .mapToInt(TenantReferral::getReferrerBonusPoints)
                .sum();

        String referralLink = baseUrl + "/tenant/register?ref=" + referralCode;

        return ReferralDashboardResponse.builder()
                .referralCode(referralCode)
                .referralLink(referralLink)
                .totalReferrals(totalReferrals)
                .completedReferrals(completedReferrals)
                .pendingReferrals(pendingReferrals)
                .totalBonusPoints(totalBonusPoints)
                .referralHistory(history)
                .build();
    }

    // ========================================
    // 推薦處理
    // ========================================

    /**
     * 處理推薦註冊（建立推薦記錄）
     *
     * @param referralCode 推薦碼
     * @param newTenantId 新租戶 ID
     */
    @Transactional
    public void processReferral(String referralCode, String newTenantId) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        log.info("處理推薦，推薦碼：{}，新租戶：{}", referralCode, newTenantId);

        // 查詢推薦人
        Tenant referrer = tenantRepository.findByReferralCodeAndDeletedAtIsNull(referralCode)
                .orElse(null);

        if (referrer == null) {
            log.warn("推薦碼無效：{}", referralCode);
            return;
        }

        // 不能自己推薦自己
        if (referrer.getId().equals(newTenantId)) {
            log.warn("不能使用自己的推薦碼");
            return;
        }

        // 建立推薦記錄
        TenantReferral referral = TenantReferral.builder()
                .referrerTenantId(referrer.getId())
                .refereeTenantId(newTenantId)
                .referralCode(referralCode)
                .status(ReferralStatus.PENDING)
                .referrerBonusPoints(REFERRAL_BONUS_POINTS)
                .refereeBonusPoints(REFERRAL_BONUS_POINTS)
                .build();

        referral.setTenantId(referrer.getId());
        tenantReferralRepository.save(referral);

        // 記錄被推薦碼到新租戶
        Tenant newTenant = tenantRepository.findByIdAndDeletedAtIsNull(newTenantId).orElse(null);
        if (newTenant != null) {
            newTenant.setReferredByCode(referralCode);
            tenantRepository.save(newTenant);
        }

        log.info("推薦記錄建立成功，推薦人：{}，被推薦人：{}", referrer.getId(), newTenantId);
    }

    /**
     * 完成推薦，發放雙方獎勵
     *
     * @param refereeTenantId 被推薦人租戶 ID
     */
    @Transactional
    public void completeReferral(String refereeTenantId) {
        log.info("完成推薦獎勵，被推薦人：{}", refereeTenantId);

        TenantReferral referral = tenantReferralRepository
                .findByRefereeTenantIdAndDeletedAtIsNull(refereeTenantId)
                .orElse(null);

        if (referral == null || referral.getStatus() != ReferralStatus.PENDING) {
            return;
        }

        // 發放推薦人獎勵
        Tenant referrer = tenantRepository.findByIdAndDeletedAtIsNull(referral.getReferrerTenantId())
                .orElse(null);
        if (referrer != null) {
            referrer.addPoints(BigDecimal.valueOf(referral.getReferrerBonusPoints()));
            tenantRepository.save(referrer);
            log.info("推薦人獎勵已發放，租戶：{}，點數：{}", referrer.getId(), referral.getReferrerBonusPoints());
        }

        // 發放被推薦人獎勵
        Tenant referee = tenantRepository.findByIdAndDeletedAtIsNull(refereeTenantId)
                .orElse(null);
        if (referee != null) {
            referee.addPoints(BigDecimal.valueOf(referral.getRefereeBonusPoints()));
            tenantRepository.save(referee);
            log.info("被推薦人獎勵已發放，租戶：{}，點數：{}", refereeTenantId, referral.getRefereeBonusPoints());
        }

        // 更新推薦記錄
        referral.complete();
        tenantReferralRepository.save(referral);

        log.info("推薦獎勵發放完成，推薦人：{}，被推薦人：{}", referral.getReferrerTenantId(), refereeTenantId);
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 生成唯一推薦碼（8 字元大寫英數字）
     */
    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            attempts++;
        } while (tenantRepository.findByReferralCodeAndDeletedAtIsNull(code).isPresent() && attempts < 10);

        return code;
    }

    /**
     * 轉換為回應 DTO
     */
    private ReferralResponse toReferralResponse(TenantReferral referral) {
        // 查詢被推薦人資訊
        String refereeName = "";
        String refereeCode = "";
        Tenant referee = tenantRepository.findByIdAndDeletedAtIsNull(referral.getRefereeTenantId()).orElse(null);
        if (referee != null) {
            refereeName = referee.getName();
            refereeCode = referee.getCode();
        }

        return ReferralResponse.builder()
                .id(referral.getId())
                .refereeTenantName(refereeName)
                .refereeTenantCode(refereeCode)
                .status(referral.getStatus())
                .referrerBonusPoints(referral.getReferrerBonusPoints())
                .refereeBonusPoints(referral.getRefereeBonusPoints())
                .rewardedAt(referral.getRewardedAt())
                .createdAt(referral.getCreatedAt())
                .build();
    }
}
