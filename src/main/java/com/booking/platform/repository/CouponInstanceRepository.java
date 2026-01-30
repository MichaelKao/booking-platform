package com.booking.platform.repository;

import com.booking.platform.entity.marketing.CouponInstance;
import com.booking.platform.enums.CouponInstanceStatus;
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
 * 票券實例 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface CouponInstanceRepository extends JpaRepository<CouponInstance, String> {

    Optional<CouponInstance> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    Optional<CouponInstance> findByTenantIdAndCodeAndDeletedAtIsNull(String tenantId, String code);

    /**
     * 查詢顧客的票券
     */
    @Query("""
            SELECT ci FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.customerId = :customerId
            AND ci.deletedAt IS NULL
            AND (:status IS NULL OR ci.status = :status)
            ORDER BY ci.createdAt DESC
            """)
    Page<CouponInstance> findByCustomer(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            @Param("status") CouponInstanceStatus status,
            Pageable pageable
    );

    /**
     * 查詢顧客可用的票券
     */
    @Query("""
            SELECT ci FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.customerId = :customerId
            AND ci.deletedAt IS NULL
            AND ci.status = 'UNUSED'
            AND (ci.validFrom IS NULL OR ci.validFrom <= :now)
            AND (ci.expiresAt IS NULL OR ci.expiresAt >= :now)
            ORDER BY ci.expiresAt ASC NULLS LAST
            """)
    List<CouponInstance> findUsableByCustomer(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            @Param("now") LocalDateTime now
    );

    /**
     * 查詢票券定義的發放記錄
     */
    @Query("""
            SELECT ci FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.couponId = :couponId
            AND ci.deletedAt IS NULL
            ORDER BY ci.createdAt DESC
            """)
    Page<CouponInstance> findByCouponId(
            @Param("tenantId") String tenantId,
            @Param("couponId") String couponId,
            Pageable pageable
    );

    /**
     * 統計顧客領取某票券的數量
     */
    @Query("""
            SELECT COUNT(ci) FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.couponId = :couponId
            AND ci.customerId = :customerId
            AND ci.deletedAt IS NULL
            """)
    long countByCustomerAndCoupon(
            @Param("tenantId") String tenantId,
            @Param("couponId") String couponId,
            @Param("customerId") String customerId
    );

    /**
     * 查詢即將到期的票券（用於通知）
     */
    @Query("""
            SELECT ci FROM CouponInstance ci
            WHERE ci.deletedAt IS NULL
            AND ci.status = 'UNUSED'
            AND ci.expiresAt BETWEEN :startTime AND :endTime
            ORDER BY ci.expiresAt ASC
            """)
    List<CouponInstance> findExpiringCoupons(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查詢已過期但狀態還是 UNUSED 的票券（用於排程更新）
     */
    @Query("""
            SELECT ci FROM CouponInstance ci
            WHERE ci.deletedAt IS NULL
            AND ci.status = 'UNUSED'
            AND ci.expiresAt < :now
            """)
    List<CouponInstance> findExpiredCoupons(@Param("now") LocalDateTime now);

    long countByTenantIdAndCustomerIdAndStatusAndDeletedAtIsNull(
            String tenantId, String customerId, CouponInstanceStatus status
    );

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(String tenantId, String code);

    // ========================================
    // 報表查詢
    // ========================================

    /**
     * 統計日期區間內發放的票券數
     */
    @Query("""
            SELECT COUNT(ci) FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.deletedAt IS NULL
            AND ci.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countIssuedByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 統計日期區間內使用的票券數
     */
    @Query("""
            SELECT COUNT(ci) FROM CouponInstance ci
            WHERE ci.tenantId = :tenantId
            AND ci.deletedAt IS NULL
            AND ci.status = 'USED'
            AND ci.usedAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countUsedByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
