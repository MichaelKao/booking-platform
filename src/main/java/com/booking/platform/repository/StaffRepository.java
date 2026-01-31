package com.booking.platform.repository;

import com.booking.platform.entity.staff.Staff;
import com.booking.platform.enums.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 員工 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    // ========================================
    // 基本查詢
    // ========================================

    Optional<Staff> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    // ========================================
    // 列表查詢
    // ========================================

    @Query("""
            SELECT s FROM Staff s
            WHERE s.tenantId = :tenantId
            AND s.deletedAt IS NULL
            AND (:status IS NULL OR s.status = :status)
            AND (:keyword IS NULL OR s.name LIKE %:keyword% OR s.displayName LIKE %:keyword%)
            ORDER BY s.sortOrder ASC, s.createdAt DESC
            """)
    Page<Staff> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") StaffStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 查詢可預約的員工
     */
    @Query("""
            SELECT s FROM Staff s
            WHERE s.tenantId = :tenantId
            AND s.deletedAt IS NULL
            AND s.status = 'ACTIVE'
            AND s.isBookable = true
            AND s.isVisible = true
            ORDER BY s.sortOrder ASC
            """)
    List<Staff> findBookableStaffs(@Param("tenantId") String tenantId);

    /**
     * 根據狀態查詢員工（給 LINE Bot 用）
     */
    List<Staff> findByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, StaffStatus status);

    // ========================================
    // 統計查詢
    // ========================================

    long countByTenantIdAndDeletedAtIsNull(String tenantId);

    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, StaffStatus status);
}
