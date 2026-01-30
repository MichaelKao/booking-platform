package com.booking.platform.repository;

import com.booking.platform.entity.catalog.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服務分類 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, String> {

    Optional<ServiceCategory> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    @Query("""
            SELECT c FROM ServiceCategory c
            WHERE c.tenantId = :tenantId
            AND c.deletedAt IS NULL
            AND (:activeOnly = false OR c.isActive = true)
            ORDER BY c.sortOrder ASC
            """)
    List<ServiceCategory> findByTenantId(
            @Param("tenantId") String tenantId,
            @Param("activeOnly") boolean activeOnly
    );

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);
}
