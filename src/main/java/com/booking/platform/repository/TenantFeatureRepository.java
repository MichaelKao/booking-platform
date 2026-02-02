package com.booking.platform.repository;

import com.booking.platform.entity.system.TenantFeature;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.enums.FeatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 租戶功能訂閱 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface TenantFeatureRepository extends JpaRepository<TenantFeature, String> {

    Optional<TenantFeature> findByTenantIdAndFeatureCodeAndDeletedAtIsNull(
            String tenantId, FeatureCode featureCode
    );

    List<TenantFeature> findByTenantIdAndDeletedAtIsNullOrderByFeatureCodeAsc(String tenantId);

    @Query("""
            SELECT tf FROM TenantFeature tf
            WHERE tf.tenantId = :tenantId
            AND tf.deletedAt IS NULL
            AND tf.status = :status
            ORDER BY tf.featureCode ASC
            """)
    List<TenantFeature> findByTenantIdAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") FeatureStatus status
    );

    /**
     * 查詢即將到期的功能（用於通知）
     */
    @Query("""
            SELECT tf FROM TenantFeature tf
            WHERE tf.deletedAt IS NULL
            AND tf.status = 'ENABLED'
            AND tf.expiresAt IS NOT NULL
            AND tf.expiresAt BETWEEN :startTime AND :endTime
            ORDER BY tf.expiresAt ASC
            """)
    List<TenantFeature> findExpiringFeatures(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查詢已過期但狀態還是 ENABLED 的功能（用於排程更新）
     */
    @Query("""
            SELECT tf FROM TenantFeature tf
            WHERE tf.deletedAt IS NULL
            AND tf.status = 'ENABLED'
            AND tf.expiresAt IS NOT NULL
            AND tf.expiresAt < :now
            """)
    List<TenantFeature> findExpiredFeatures(@Param("now") LocalDateTime now);

    /**
     * 檢查租戶是否啟用指定功能
     */
    @Query("""
            SELECT CASE WHEN COUNT(tf) > 0 THEN true ELSE false END
            FROM TenantFeature tf
            WHERE tf.tenantId = :tenantId
            AND tf.featureCode = :featureCode
            AND tf.deletedAt IS NULL
            AND tf.status = 'ENABLED'
            AND (tf.expiresAt IS NULL OR tf.expiresAt > :now)
            """)
    boolean isFeatureEnabled(
            @Param("tenantId") String tenantId,
            @Param("featureCode") FeatureCode featureCode,
            @Param("now") LocalDateTime now
    );

    /**
     * 統計租戶啟用的功能數量
     */
    @Query("""
            SELECT COUNT(tf) FROM TenantFeature tf
            WHERE tf.tenantId = :tenantId
            AND tf.deletedAt IS NULL
            AND tf.status = 'ENABLED'
            """)
    long countEnabledFeatures(@Param("tenantId") String tenantId);

    boolean existsByTenantIdAndFeatureCodeAndDeletedAtIsNull(String tenantId, FeatureCode featureCode);

    /**
     * 查詢租戶功能（包含已刪除的，用於重新訂閱）
     */
    Optional<TenantFeature> findByTenantIdAndFeatureCode(String tenantId, FeatureCode featureCode);

    /**
     * 查詢所有啟用指定功能的租戶
     */
    List<TenantFeature> findByFeatureCodeAndDeletedAtIsNull(FeatureCode featureCode);
}
