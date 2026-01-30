package com.booking.platform.repository;

import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 票券定義 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, String> {

    Optional<Coupon> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    @Query("""
            SELECT c FROM Coupon c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            AND (:type IS NULL OR c.type = :type)
            ORDER BY c.createdAt DESC
            """)
    Page<Coupon> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") CouponStatus status,
            @Param("type") CouponType type,
            Pageable pageable
    );

    /**
     * 查詢可發放的票券
     */
    @Query("""
            SELECT c FROM Coupon c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND c.status = 'PUBLISHED'
            AND (c.totalQuantity IS NULL OR c.issuedQuantity < c.totalQuantity)
            ORDER BY c.name ASC
            """)
    List<Coupon> findAvailableByTenantId(@Param("tenantId") String tenantId);

    /**
     * 查詢已發布的票券列表
     */
    List<Coupon> findByTenantIdAndStatusAndDeletedAtIsNullOrderByNameAsc(
            String tenantId, CouponStatus status
    );

    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, CouponStatus status);

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String excludeId);
}
