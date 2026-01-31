package com.booking.platform.repository.line;

import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.enums.line.LineConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 店家 LINE 設定 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface TenantLineConfigRepository extends JpaRepository<TenantLineConfig, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 根據租戶 ID 查詢設定
     *
     * @param tenantId 租戶 ID
     * @return LINE 設定
     */
    Optional<TenantLineConfig> findByTenantId(String tenantId);

    // ========================================
    // 狀態查詢
    // ========================================

    /**
     * 查詢指定狀態的設定
     *
     * @param status 狀態
     * @return 設定列表
     */
    List<TenantLineConfig> findByStatus(LineConfigStatus status);

    /**
     * 查詢所有已啟用的設定
     *
     * @return 設定列表
     */
    List<TenantLineConfig> findByStatusAndBookingEnabledTrue(LineConfigStatus status);

    // ========================================
    // 存在性檢查
    // ========================================

    /**
     * 檢查租戶是否已有設定
     *
     * @param tenantId 租戶 ID
     * @return true 表示已存在
     */
    boolean existsByTenantId(String tenantId);

    /**
     * 檢查 Channel ID 是否已被使用
     *
     * @param channelId 頻道 ID
     * @return true 表示已被使用
     */
    boolean existsByChannelId(String channelId);

    /**
     * 檢查 Channel ID 是否被其他租戶使用
     *
     * @param channelId 頻道 ID
     * @param tenantId  排除的租戶 ID
     * @return true 表示已被其他租戶使用
     */
    boolean existsByChannelIdAndTenantIdNot(String channelId, String tenantId);

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計指定狀態的設定數量
     *
     * @param status 狀態
     * @return 數量
     */
    long countByStatus(LineConfigStatus status);

    /**
     * 查詢需要重置推送計數的設定
     *
     * @return 設定列表
     */
    @Query("""
            SELECT t FROM TenantLineConfig t
            WHERE t.status = 'ACTIVE'
            AND (t.pushCountResetAt IS NULL
                 OR EXTRACT(MONTH FROM t.pushCountResetAt) != EXTRACT(MONTH FROM CURRENT_DATE)
                 OR EXTRACT(YEAR FROM t.pushCountResetAt) != EXTRACT(YEAR FROM CURRENT_DATE))
            """)
    List<TenantLineConfig> findConfigsNeedingPushCountReset();
}
