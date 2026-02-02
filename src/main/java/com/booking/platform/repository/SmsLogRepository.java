package com.booking.platform.repository;

import com.booking.platform.entity.system.SmsLog;
import com.booking.platform.enums.SmsStatus;
import com.booking.platform.enums.SmsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SMS 發送記錄 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 依租戶 ID 查詢 SMS 記錄（分頁）
     */
    Page<SmsLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * 依租戶 ID 和狀態查詢
     */
    Page<SmsLog> findByTenantIdAndStatusOrderByCreatedAtDesc(
            String tenantId,
            SmsStatus status,
            Pageable pageable
    );

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計租戶在指定時間範圍內的發送數量
     */
    @Query("""
            SELECT COUNT(s) FROM SmsLog s
            WHERE s.tenantId = :tenantId
            AND s.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 統計租戶在指定時間範圍內的成功發送數量
     */
    @Query("""
            SELECT COUNT(s) FROM SmsLog s
            WHERE s.tenantId = :tenantId
            AND s.status = :status
            AND s.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countByTenantIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("status") SmsStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 依類型統計
     */
    @Query("""
            SELECT s.smsType, COUNT(s) FROM SmsLog s
            WHERE s.tenantId = :tenantId
            AND s.createdAt BETWEEN :startDateTime AND :endDateTime
            GROUP BY s.smsType
            """)
    List<Object[]> countByTenantIdAndTypeGrouped(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // ========================================
    // 全平台統計（超管用）
    // ========================================

    /**
     * 統計全平台指定時間範圍內的發送數量
     */
    @Query("""
            SELECT COUNT(s) FROM SmsLog s
            WHERE s.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countByDateRange(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 依租戶統計發送量
     */
    @Query(value = """
            SELECT s.tenant_id, t.name, COUNT(s.id) as cnt
            FROM sms_logs s
            LEFT JOIN tenants t ON s.tenant_id = t.id
            WHERE s.created_at BETWEEN :startDateTime AND :endDateTime
            GROUP BY s.tenant_id, t.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> countByTenantGrouped(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("limit") int limit
    );
}
