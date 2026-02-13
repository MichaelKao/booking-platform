package com.booking.platform.service;

import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.entity.marketing.Campaign;
import com.booking.platform.repository.CampaignRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.line.LineUserRepository;
import com.booking.platform.service.line.LineMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活動推播與獎勵觸發服務
 *
 * <p>負責活動發布時的 LINE 推播、票券發放、點數贈送
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignPushService {

    private final LineUserRepository lineUserRepository;
    private final LineMessageService lineMessageService;
    private final CouponService couponService;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;

    /**
     * 發布活動時推播 LINE 訊息給所有追蹤者
     *
     * <p>非同步執行，不阻塞主流程
     */
    @Async
    public void sendCampaignPush(String tenantId, Campaign campaign) {
        log.info("開始推播活動，租戶：{}，活動：{}", tenantId, campaign.getName());

        try {
            // 取得所有 LINE 追蹤者
            List<LineUser> followers = lineUserRepository
                    .findByTenantIdAndIsFollowedAndDeletedAtIsNull(tenantId, true);

            log.debug("追蹤者數量：{}", followers.size());

            int successCount = 0;
            for (LineUser user : followers) {
                try {
                    // 推播訊息
                    lineMessageService.pushText(tenantId, user.getLineUserId(), campaign.getPushMessage());

                    // 如果有關聯票券，自動發給每位顧客
                    if (campaign.getCouponId() != null && user.getCustomerId() != null) {
                        try {
                            TenantContext.setTenantId(tenantId);
                            couponService.issueToCustomer(campaign.getCouponId(), user.getCustomerId());
                        } catch (Exception e) {
                            log.debug("自動發放票券失敗（可能已領取）：{}", e.getMessage());
                        } finally {
                            TenantContext.clear();
                        }
                    }
                    successCount++;
                } catch (Exception e) {
                    log.warn("推播失敗，LINE User：{}，錯誤：{}", user.getLineUserId(), e.getMessage());
                }
            }

            // 更新參與人數
            campaign.setParticipantCount(successCount);
            campaignRepository.save(campaign);

            log.info("活動推播完成，活動：{}，成功：{}/{}", campaign.getName(), successCount, followers.size());

        } catch (Exception e) {
            log.error("活動推播執行失敗，活動：{}，錯誤：{}", campaign.getName(), e.getMessage(), e);
        }
    }

    /**
     * 針對單一顧客觸發活動獎勵（排程器用）
     *
     * <p>發放票券和贈送點數
     */
    public void triggerForCustomer(String tenantId, Campaign campaign, Customer customer) {
        log.debug("觸發活動獎勵，活動：{}，顧客：{}", campaign.getName(), customer.getId());

        // 發放票券
        if (campaign.getCouponId() != null) {
            try {
                TenantContext.setTenantId(tenantId);
                couponService.issueToCustomer(campaign.getCouponId(), customer.getId());
            } catch (Exception e) {
                log.debug("活動票券發放失敗：{}", e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        // 贈送點數
        if (campaign.getBonusPoints() != null && campaign.getBonusPoints() > 0) {
            customer.addPoints(campaign.getBonusPoints());
            customerRepository.save(customer);
        }

        // 遞增參與人數
        campaign.incrementParticipant();
        campaignRepository.save(campaign);
    }
}
