package com.booking.platform.repository;

import com.booking.platform.entity.product.InventoryLog;
import com.booking.platform.enums.InventoryActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 庫存異動記錄 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, String> {

    /**
     * 依租戶 ID 查詢（分頁）
     */
    Page<InventoryLog> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, Pageable pageable);

    /**
     * 依商品 ID 查詢
     */
    List<InventoryLog> findByTenantIdAndProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, String productId);

    /**
     * 依商品 ID 查詢（分頁）
     */
    Page<InventoryLog> findByTenantIdAndProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, String productId, Pageable pageable);

    /**
     * 依時間範圍查詢
     */
    @Query("SELECT l FROM InventoryLog l WHERE l.tenantId = :tenantId " +
           "AND l.createdAt BETWEEN :startDate AND :endDate " +
           "AND l.deletedAt IS NULL ORDER BY l.createdAt DESC")
    List<InventoryLog> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 依異動類型查詢
     */
    Page<InventoryLog> findByTenantIdAndActionTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, InventoryActionType actionType, Pageable pageable);

    /**
     * 計算商品異動次數
     */
    long countByTenantIdAndProductIdAndDeletedAtIsNull(String tenantId, String productId);
}
