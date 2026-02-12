package com.booking.platform.repository;

import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.enums.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服務項目 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, String> {

    // ========================================
    // 基本查詢
    // ========================================

    Optional<ServiceItem> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    // ========================================
    // 列表查詢
    // ========================================

    @Query("""
            SELECT s FROM ServiceItem s
            WHERE s.tenantId = :tenantId
            AND s.deletedAt IS NULL
            AND (:status IS NULL OR s.status = :status)
            AND (:categoryId IS NULL OR s.categoryId = :categoryId)
            AND (:keyword IS NULL OR s.name LIKE %:keyword%)
            ORDER BY s.sortOrder ASC, s.createdAt DESC
            """)
    Page<ServiceItem> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") ServiceStatus status,
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 查詢可預約的服務（給 LINE Bot 用）
     */
    @Query("""
            SELECT s FROM ServiceItem s
            WHERE s.tenantId = :tenantId
            AND s.deletedAt IS NULL
            AND s.status = 'ACTIVE'
            AND s.isVisible = true
            ORDER BY s.sortOrder ASC
            """)
    List<ServiceItem> findBookableServices(@Param("tenantId") String tenantId);

    /**
     * 按分類查詢可預約的服務
     */
    @Query("""
            SELECT s FROM ServiceItem s
            WHERE s.tenantId = :tenantId
            AND s.categoryId = :categoryId
            AND s.deletedAt IS NULL
            AND s.status = 'ACTIVE'
            AND s.isVisible = true
            ORDER BY s.sortOrder ASC
            """)
    List<ServiceItem> findBookableServicesByCategory(
            @Param("tenantId") String tenantId,
            @Param("categoryId") String categoryId
    );

    /**
     * 根據狀態查詢服務（給 LINE Bot 用）
     */
    List<ServiceItem> findByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, ServiceStatus status);

    /**
     * 查詢有可預約服務的分類 ID 清單
     */
    @Query("""
            SELECT DISTINCT s.categoryId FROM ServiceItem s
            WHERE s.tenantId = :tenantId
            AND s.categoryId IS NOT NULL
            AND s.deletedAt IS NULL
            AND s.status = 'ACTIVE'
            AND s.isVisible = true
            """)
    List<String> findDistinctBookableCategoryIds(@Param("tenantId") String tenantId);

    // ========================================
    // 存在性檢查
    // ========================================

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String excludeId);

    // ========================================
    // 統計查詢
    // ========================================

    long countByTenantIdAndDeletedAtIsNull(String tenantId);
}
