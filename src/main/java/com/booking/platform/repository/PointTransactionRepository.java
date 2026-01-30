package com.booking.platform.repository;

import com.booking.platform.entity.customer.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 點數交易 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, String> {

    @Query("""
            SELECT p FROM PointTransaction p
            WHERE p.tenantId = :tenantId
            AND p.customerId = :customerId
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    Page<PointTransaction> findByCustomerId(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            Pageable pageable
    );

    /**
     * 計算顧客的點數總和（用於驗證）
     */
    @Query("""
            SELECT COALESCE(SUM(p.points), 0) FROM PointTransaction p
            WHERE p.tenantId = :tenantId
            AND p.customerId = :customerId
            AND p.deletedAt IS NULL
            """)
    int sumPointsByCustomerId(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId
    );
}
