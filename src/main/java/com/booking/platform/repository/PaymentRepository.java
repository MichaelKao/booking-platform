package com.booking.platform.repository;

import com.booking.platform.entity.system.Payment;
import com.booking.platform.enums.PaymentStatus;
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
 * 支付記錄 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 依商店訂單編號查詢
     */
    Optional<Payment> findByMerchantTradeNoAndDeletedAtIsNull(String merchantTradeNo);

    /**
     * 依 ECPay 交易編號查詢
     */
    Optional<Payment> findByEcpayTradeNoAndDeletedAtIsNull(String ecpayTradeNo);

    /**
     * 依 ID 和租戶查詢
     */
    Optional<Payment> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    // ========================================
    // 列表查詢
    // ========================================

    /**
     * 依租戶 ID 查詢（分頁）
     */
    Page<Payment> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, Pageable pageable);

    /**
     * 依租戶 ID 和狀態查詢
     */
    Page<Payment> findByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, PaymentStatus status, Pageable pageable);

    /**
     * 依預約 ID 查詢
     */
    List<Payment> findByBookingIdAndDeletedAtIsNull(String bookingId);

    /**
     * 依顧客 ID 查詢
     */
    Page<Payment> findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            String tenantId, String customerId, Pageable pageable);

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計租戶在指定時間範圍內的成功交易金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.tenantId = :tenantId
            AND p.status = 'SUCCESS'
            AND p.deletedAt IS NULL
            AND p.paidAt BETWEEN :startDateTime AND :endDateTime
            """)
    BigDecimal sumSuccessAmountByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 統計租戶成功交易數量
     */
    @Query("""
            SELECT COUNT(p) FROM Payment p
            WHERE p.tenantId = :tenantId
            AND p.status = 'SUCCESS'
            AND p.deletedAt IS NULL
            AND p.paidAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countSuccessByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    // ========================================
    // 全平台統計（超管用）
    // ========================================

    /**
     * 統計全平台成功交易金額
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.status = 'SUCCESS'
            AND p.deletedAt IS NULL
            AND p.paidAt BETWEEN :startDateTime AND :endDateTime
            """)
    BigDecimal sumSuccessAmountByDateRange(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}
