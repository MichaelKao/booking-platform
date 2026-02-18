package com.booking.platform.repository;

import com.booking.platform.entity.product.ProductOrder;
import com.booking.platform.enums.ProductOrderStatus;
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
 * 商品訂單 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, String> {

    /**
     * 依租戶 ID 查詢（分頁）
     */
    Page<ProductOrder> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, Pageable pageable);

    /**
     * 依 ID 和租戶 ID 查詢
     */
    Optional<ProductOrder> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    /**
     * 依訂單編號查詢
     */
    Optional<ProductOrder> findByOrderNoAndTenantIdAndDeletedAtIsNull(String orderNo, String tenantId);

    /**
     * 依顧客 ID 查詢
     */
    List<ProductOrder> findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, String customerId);

    /**
     * 依 LINE 用戶 ID 查詢
     */
    List<ProductOrder> findByTenantIdAndLineUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, String lineUserId);

    /**
     * 依狀態查詢
     */
    Page<ProductOrder> findByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, ProductOrderStatus status, Pageable pageable);

    /**
     * 依時間範圍查詢
     */
    @Query("SELECT o FROM ProductOrder o WHERE o.tenantId = :tenantId " +
           "AND o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<ProductOrder> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 統計各狀態訂單數量
     */
    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, ProductOrderStatus status);

    /**
     * 統計今日訂單
     */
    @Query("SELECT COUNT(o) FROM ProductOrder o WHERE o.tenantId = :tenantId " +
           "AND o.createdAt >= :startOfDay AND o.deletedAt IS NULL")
    long countTodayOrders(@Param("tenantId") String tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 計算今日營業額
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM ProductOrder o WHERE o.tenantId = :tenantId " +
           "AND o.status = 'COMPLETED' AND o.createdAt >= :startOfDay AND o.deletedAt IS NULL")
    java.math.BigDecimal sumTodayRevenue(@Param("tenantId") String tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 產生訂單編號
     */
    @Query("SELECT COUNT(o) FROM ProductOrder o WHERE o.tenantId = :tenantId " +
           "AND o.createdAt >= :startOfDay")
    long countOrdersToday(@Param("tenantId") String tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 計算指定日期範圍內已完成訂單的總營收（依建立時間）
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM ProductOrder o WHERE o.tenantId = :tenantId " +
           "AND o.status = 'COMPLETED' AND o.createdAt BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL")
    java.math.BigDecimal sumCompletedRevenueByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
