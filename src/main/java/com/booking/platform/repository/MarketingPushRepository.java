package com.booking.platform.repository;

import com.booking.platform.entity.marketing.MarketingPush;
import com.booking.platform.enums.MarketingPushStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 行銷推播 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface MarketingPushRepository extends JpaRepository<MarketingPush, String> {

    Optional<MarketingPush> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    /**
     * 查詢推播列表（分頁）
     */
    @Query("""
            SELECT mp FROM MarketingPush mp
            WHERE mp.tenantId = :tenantId
            AND mp.deletedAt IS NULL
            AND (:status IS NULL OR mp.status = :status)
            ORDER BY mp.createdAt DESC
            """)
    Page<MarketingPush> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") MarketingPushStatus status,
            Pageable pageable
    );

    /**
     * 查詢待執行的排程推播
     *
     * <p>查詢條件：
     * <ul>
     *   <li>狀態為 SCHEDULED</li>
     *   <li>排程時間已到</li>
     * </ul>
     */
    @Query("""
            SELECT mp FROM MarketingPush mp
            WHERE mp.deletedAt IS NULL
            AND mp.status = 'SCHEDULED'
            AND mp.scheduledAt <= :now
            ORDER BY mp.scheduledAt ASC
            """)
    List<MarketingPush> findPendingScheduledPushes(@Param("now") LocalDateTime now);

    /**
     * 查詢指定租戶的待執行排程推播
     */
    @Query("""
            SELECT mp FROM MarketingPush mp
            WHERE mp.tenantId = :tenantId
            AND mp.deletedAt IS NULL
            AND mp.status = 'SCHEDULED'
            AND mp.scheduledAt <= :now
            ORDER BY mp.scheduledAt ASC
            """)
    List<MarketingPush> findPendingScheduledPushesByTenant(
            @Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now
    );

    /**
     * 統計各狀態數量
     */
    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, MarketingPushStatus status);

    /**
     * 檢查是否有重複標題
     */
    boolean existsByTenantIdAndTitleAndDeletedAtIsNull(String tenantId, String title);

    boolean existsByTenantIdAndTitleAndIdNotAndDeletedAtIsNull(String tenantId, String title, String excludeId);
}
