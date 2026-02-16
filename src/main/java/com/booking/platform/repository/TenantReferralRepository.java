package com.booking.platform.repository;

import com.booking.platform.entity.system.TenantReferral;
import com.booking.platform.enums.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 租戶推薦 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface TenantReferralRepository extends JpaRepository<TenantReferral, String> {

    /**
     * 依推薦人租戶 ID 查詢推薦記錄
     */
    List<TenantReferral> findByReferrerTenantIdAndDeletedAtIsNull(String referrerTenantId);

    /**
     * 依被推薦人租戶 ID 查詢推薦記錄
     */
    Optional<TenantReferral> findByRefereeTenantIdAndDeletedAtIsNull(String refereeTenantId);

    /**
     * 計算推薦人的推薦數量
     */
    long countByReferrerTenantIdAndDeletedAtIsNull(String referrerTenantId);

    /**
     * 計算推薦人的已完成推薦數量
     */
    long countByReferrerTenantIdAndStatusAndDeletedAtIsNull(String referrerTenantId, ReferralStatus status);
}
