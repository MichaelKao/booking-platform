package com.booking.platform.repository;

import com.booking.platform.entity.system.PointTopUp;
import com.booking.platform.enums.TopUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 點數儲值申請 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface PointTopUpRepository extends JpaRepository<PointTopUp, String> {

    Optional<PointTopUp> findByIdAndDeletedAtIsNull(String id);

    Optional<PointTopUp> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    /**
     * 分頁查詢所有儲值申請（管理員用）
     */
    @Query("""
            SELECT p FROM PointTopUp p
            WHERE p.deletedAt IS NULL
            AND (:status IS NULL OR p.status = :status)
            AND (:tenantId IS NULL OR p.tenantId = :tenantId)
            ORDER BY p.createdAt DESC
            """)
    Page<PointTopUp> findAllWithFilters(
            @Param("status") TopUpStatus status,
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    /**
     * 查詢店家的儲值申請
     */
    @Query("""
            SELECT p FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.deletedAt IS NULL
            AND (:status IS NULL OR p.status = :status)
            ORDER BY p.createdAt DESC
            """)
    Page<PointTopUp> findByTenantId(
            @Param("tenantId") String tenantId,
            @Param("status") TopUpStatus status,
            Pageable pageable
    );

    /**
     * 查詢待審核的儲值申請
     */
    List<PointTopUp> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(TopUpStatus status);

    /**
     * 統計待審核數量
     */
    long countByStatusAndDeletedAtIsNull(TopUpStatus status);

    /**
     * 統計店家儲值總金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.deletedAt IS NULL
            AND p.status = 'APPROVED'
            """)
    BigDecimal sumApprovedAmountByTenantId(@Param("tenantId") String tenantId);

    /**
     * 統計店家儲值總點數
     */
    @Query("""
            SELECT COALESCE(SUM(p.points), 0) FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.deletedAt IS NULL
            AND p.status = 'APPROVED'
            """)
    Integer sumApprovedPointsByTenantId(@Param("tenantId") String tenantId);

    /**
     * 統計時間範圍內的儲值
     */
    @Query("""
            SELECT COALESCE(SUM(p.points), 0) FROM PointTopUp p
            WHERE p.deletedAt IS NULL
            AND p.status = 'APPROVED'
            AND p.reviewedAt BETWEEN :startTime AND :endTime
            """)
    Integer sumApprovedPointsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查詢店家的儲值申請（不帶狀態篩選）
     */
    @Query("""
            SELECT p FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    Page<PointTopUp> findByTenantId(
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    /**
     * 查詢店家的儲值申請（帶狀態篩選）
     */
    @Query("""
            SELECT p FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.status = :status
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    Page<PointTopUp> findByTenantIdAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") TopUpStatus status,
            Pageable pageable
    );

    /**
     * 統計店家待審核儲值金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM PointTopUp p
            WHERE p.tenantId = :tenantId
            AND p.deletedAt IS NULL
            AND p.status = 'PENDING'
            """)
    BigDecimal sumPendingAmountByTenantId(@Param("tenantId") String tenantId);

    /**
     * 查詢店家的儲值記錄（多狀態篩選）
     */
    Page<PointTopUp> findByTenantIdAndStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId,
            List<TopUpStatus> statuses,
            Pageable pageable
    );

    /**
     * 統計全平台待審核金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM PointTopUp p
            WHERE p.deletedAt IS NULL
            AND p.status = 'PENDING'
            """)
    BigDecimal sumPendingAmount();

    /**
     * 統計全平台時間範圍內審核通過金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM PointTopUp p
            WHERE p.deletedAt IS NULL
            AND p.status = 'APPROVED'
            AND p.reviewedAt BETWEEN :startTime AND :endTime
            """)
    BigDecimal sumApprovedAmountBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
