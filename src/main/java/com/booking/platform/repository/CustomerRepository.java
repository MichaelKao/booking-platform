package com.booking.platform.repository;

import com.booking.platform.entity.customer.Customer;
import com.booking.platform.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 顧客 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    // ========================================
    // 基本查詢
    // ========================================

    Optional<Customer> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    Optional<Customer> findByTenantIdAndLineUserIdAndDeletedAtIsNull(String tenantId, String lineUserId);

    Optional<Customer> findByTenantIdAndPhoneAndDeletedAtIsNull(String tenantId, String phone);

    // ========================================
    // 列表查詢
    // ========================================

    @Query("""
            SELECT c FROM Customer c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            AND (:keyword IS NULL OR c.name LIKE %:keyword% OR c.phone LIKE %:keyword% OR c.lineDisplayName LIKE %:keyword%)
            ORDER BY c.lastVisitAt DESC NULLS LAST
            """)
    Page<Customer> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") CustomerStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ========================================
    // 存在性檢查
    // ========================================

    boolean existsByTenantIdAndLineUserIdAndDeletedAtIsNull(String tenantId, String lineUserId);

    boolean existsByTenantIdAndPhoneAndDeletedAtIsNull(String tenantId, String phone);

    boolean existsByTenantIdAndPhoneAndIdNotAndDeletedAtIsNull(String tenantId, String phone, String excludeId);

    // ========================================
    // 特殊查詢
    // ========================================

    /**
     * 查詢今天生日的顧客
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND c.status = 'ACTIVE'
            AND EXTRACT(MONTH FROM c.birthday) = :month
            AND EXTRACT(DAY FROM c.birthday) = :day
            """)
    List<Customer> findBirthdayCustomers(
            @Param("tenantId") String tenantId,
            @Param("month") int month,
            @Param("day") int day
    );

    /**
     * 查詢指定會員等級的顧客數量
     */
    long countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(String tenantId, String membershipLevelId);

    // ========================================
    // 統計查詢
    // ========================================

    long countByTenantIdAndDeletedAtIsNull(String tenantId);

    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, CustomerStatus status);

    /**
     * 統計日期區間內的新顧客數
     */
    @Query("""
            SELECT COUNT(c) FROM Customer c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND c.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countNewByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
