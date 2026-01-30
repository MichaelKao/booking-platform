package com.booking.platform.repository;

import com.booking.platform.entity.marketing.Campaign;
import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
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
 * 行銷活動 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, String> {

    Optional<Campaign> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    @Query("""
            SELECT c FROM Campaign c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            AND (:type IS NULL OR c.type = :type)
            ORDER BY c.createdAt DESC
            """)
    Page<Campaign> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") CampaignStatus status,
            @Param("type") CampaignType type,
            Pageable pageable
    );

    /**
     * 查詢進行中的活動
     */
    @Query("""
            SELECT c FROM Campaign c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND c.status = 'ACTIVE'
            AND (c.startAt IS NULL OR c.startAt <= :now)
            AND (c.endAt IS NULL OR c.endAt >= :now)
            ORDER BY c.createdAt DESC
            """)
    List<Campaign> findActiveByTenantId(
            @Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now
    );

    /**
     * 查詢特定類型的進行中活動
     */
    @Query("""
            SELECT c FROM Campaign c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND c.type = :type
            AND c.status = 'ACTIVE'
            AND (c.startAt IS NULL OR c.startAt <= :now)
            AND (c.endAt IS NULL OR c.endAt >= :now)
            """)
    List<Campaign> findActiveByTenantIdAndType(
            @Param("tenantId") String tenantId,
            @Param("type") CampaignType type,
            @Param("now") LocalDateTime now
    );

    /**
     * 查詢需要自動觸發的生日活動
     */
    @Query("""
            SELECT c FROM Campaign c
            WHERE c.deletedAt IS NULL
            AND c.type = 'BIRTHDAY'
            AND c.status = 'ACTIVE'
            AND c.isAutoTrigger = true
            """)
    List<Campaign> findAutoBirthdayCampaigns();

    /**
     * 查詢需要自動觸發的喚回活動
     */
    @Query("""
            SELECT c FROM Campaign c
            WHERE c.deletedAt IS NULL
            AND c.type = 'RECALL'
            AND c.status = 'ACTIVE'
            AND c.isAutoTrigger = true
            """)
    List<Campaign> findAutoRecallCampaigns();

    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, CampaignStatus status);

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String excludeId);
}
