package com.booking.platform.repository;

import com.booking.platform.entity.product.Product;
import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * 根據 ID 和租戶 ID 查詢（未刪除）
     */
    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    /**
     * 依條件查詢商品列表（分頁）
     */
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId " +
            "AND p.deletedAt IS NULL " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.sku LIKE %:keyword%)")
    Page<Product> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") ProductStatus status,
            @Param("category") ProductCategory category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 查詢上架中的商品
     */
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId " +
            "AND p.deletedAt IS NULL " +
            "AND p.status = 'ON_SALE' " +
            "AND p.isSellable = true " +
            "ORDER BY p.sortOrder, p.name")
    List<Product> findOnSaleByTenantId(@Param("tenantId") String tenantId);

    /**
     * 查詢低庫存商品
     */
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId " +
            "AND p.deletedAt IS NULL " +
            "AND p.trackInventory = true " +
            "AND p.safetyStock IS NOT NULL " +
            "AND p.stockQuantity <= p.safetyStock " +
            "ORDER BY p.stockQuantity")
    List<Product> findLowStockByTenantId(@Param("tenantId") String tenantId);

    /**
     * 依分類查詢商品
     */
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId " +
            "AND p.deletedAt IS NULL " +
            "AND p.category = :category " +
            "AND p.status = 'ON_SALE' " +
            "ORDER BY p.sortOrder, p.name")
    List<Product> findByCategoryAndTenantId(
            @Param("tenantId") String tenantId,
            @Param("category") ProductCategory category
    );

    /**
     * 檢查商品名稱是否重複
     */
    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    /**
     * 檢查商品名稱是否重複（排除自己）
     */
    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String id);

    /**
     * 檢查 SKU 是否重複
     */
    boolean existsByTenantIdAndSkuAndDeletedAtIsNull(String tenantId, String sku);

    /**
     * 檢查 SKU 是否重複（排除自己）
     */
    boolean existsByTenantIdAndSkuAndIdNotAndDeletedAtIsNull(String tenantId, String sku, String id);

    /**
     * 依 SKU 查詢商品
     */
    Optional<Product> findByTenantIdAndSkuAndDeletedAtIsNull(String tenantId, String sku);

    /**
     * 依狀態查詢商品
     */
    List<Product> findByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, ProductStatus status);
}
