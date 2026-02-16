package com.booking.platform.repository;

import com.booking.platform.dto.response.TenantListItemResponse;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 租戶 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 依 ID 查詢（排除已刪除）
     */
    Optional<Tenant> findByIdAndDeletedAtIsNull(String id);

    /**
     * 依代碼查詢（排除已刪除）
     */
    Optional<Tenant> findByCodeAndDeletedAtIsNull(String code);

    /**
     * 依推薦碼查詢（排除已刪除）
     */
    Optional<Tenant> findByReferralCodeAndDeletedAtIsNull(String referralCode);

    // ========================================
    // 存在性檢查
    // ========================================

    /**
     * 檢查 ID 是否存在
     */
    boolean existsByIdAndDeletedAtIsNull(String id);

    /**
     * 檢查代碼是否存在
     */
    boolean existsByCodeAndDeletedAtIsNull(String code);

    /**
     * 檢查代碼是否存在（排除指定 ID）
     */
    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, String excludeId);

    /**
     * 檢查 Email 是否存在
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    // ========================================
    // 投影查詢（效能優化）
    // ========================================

    /**
     * 查詢列表項目
     */
    @Query("""
            SELECT new com.booking.platform.dto.response.TenantListItemResponse(
                t.id, t.code, t.name, t.status, t.phone, t.email,
                t.pointBalance, t.isTestAccount, t.createdAt
            )
            FROM Tenant t
            WHERE t.deletedAt IS NULL
            AND (:status IS NULL OR t.status = :status)
            AND (:keyword IS NULL OR t.name LIKE %:keyword% OR t.code LIKE %:keyword%)
            ORDER BY t.createdAt DESC
            """)
    Page<TenantListItemResponse> findListItems(
            @Param("status") TenantStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計各狀態數量
     */
    @Query("""
            SELECT COUNT(t)
            FROM Tenant t
            WHERE t.deletedAt IS NULL
            AND (:status IS NULL OR t.status = :status)
            """)
    long countByStatus(@Param("status") TenantStatus status);

    // ========================================
    // 批次操作
    // ========================================

    /**
     * 重置所有租戶的月度推送計數
     */
    @Modifying
    @Query("""
            UPDATE Tenant t
            SET t.monthlyPushUsed = 0
            WHERE t.deletedAt IS NULL
            """)
    int resetAllMonthlyPushUsed();

    /**
     * 重置所有租戶的月度 SMS 計數
     */
    @Modifying
    @Query("""
            UPDATE Tenant t
            SET t.monthlySmsUsed = 0
            WHERE t.deletedAt IS NULL
            """)
    int resetAllMonthlySmsUsed();
}
