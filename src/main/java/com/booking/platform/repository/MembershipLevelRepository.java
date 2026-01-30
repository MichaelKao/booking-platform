package com.booking.platform.repository;

import com.booking.platform.entity.customer.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 會員等級 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, String> {

    Optional<MembershipLevel> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    @Query("""
            SELECT m FROM MembershipLevel m
            WHERE m.tenantId = :tenantId
            AND m.deletedAt IS NULL
            AND (:activeOnly = false OR m.isActive = true)
            ORDER BY m.sortOrder ASC
            """)
    List<MembershipLevel> findByTenantId(
            @Param("tenantId") String tenantId,
            @Param("activeOnly") boolean activeOnly
    );

    /**
     * 查詢預設等級
     */
    Optional<MembershipLevel> findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(String tenantId);

    /**
     * 根據消費金額查詢適用的等級
     */
    @Query("""
            SELECT m FROM MembershipLevel m
            WHERE m.tenantId = :tenantId
            AND m.deletedAt IS NULL
            AND m.isActive = true
            AND m.upgradeThreshold <= :totalSpent
            ORDER BY m.upgradeThreshold DESC
            LIMIT 1
            """)
    Optional<MembershipLevel> findApplicableLevel(
            @Param("tenantId") String tenantId,
            @Param("totalSpent") BigDecimal totalSpent
    );

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String excludeId);

    List<MembershipLevel> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(String tenantId);

    @Query("SELECT MAX(m.sortOrder) FROM MembershipLevel m WHERE m.tenantId = :tenantId AND m.deletedAt IS NULL")
    Integer findMaxSortOrderByTenantId(@Param("tenantId") String tenantId);
}
