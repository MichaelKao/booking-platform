package com.booking.platform.repository.line;

import com.booking.platform.entity.line.LineUser;
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
 * LINE 用戶 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface LineUserRepository extends JpaRepository<LineUser, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 根據租戶 ID 和 LINE User ID 查詢
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return LINE 用戶
     */
    Optional<LineUser> findByTenantIdAndLineUserIdAndDeletedAtIsNull(
            String tenantId, String lineUserId
    );

    /**
     * 根據 ID 和租戶 ID 查詢
     *
     * @param id       主鍵 ID
     * @param tenantId 租戶 ID
     * @return LINE 用戶
     */
    Optional<LineUser> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    /**
     * 根據租戶 ID 和顧客 ID 查詢
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID
     * @return LINE 用戶
     */
    Optional<LineUser> findByTenantIdAndCustomerIdAndDeletedAtIsNull(
            String tenantId, String customerId
    );

    // ========================================
    // 列表查詢
    // ========================================

    /**
     * 查詢租戶的所有追蹤中用戶
     *
     * @param tenantId 租戶 ID
     * @param pageable 分頁參數
     * @return LINE 用戶分頁
     */
    @Query("""
            SELECT lu FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.isFollowed = true
            ORDER BY lu.lastInteractionAt DESC NULLS LAST
            """)
    Page<LineUser> findFollowedUsersByTenantId(
            @Param("tenantId") String tenantId,
            Pageable pageable
    );

    /**
     * 查詢租戶的所有用戶
     *
     * @param tenantId 租戶 ID
     * @param pageable 分頁參數
     * @return LINE 用戶分頁
     */
    Page<LineUser> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    /**
     * 查詢未綁定顧客的用戶
     *
     * @param tenantId 租戶 ID
     * @return LINE 用戶列表
     */
    @Query("""
            SELECT lu FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.customerId IS NULL
            AND lu.isFollowed = true
            ORDER BY lu.createdAt DESC
            """)
    List<LineUser> findUnboundUsersByTenantId(@Param("tenantId") String tenantId);

    // ========================================
    // 存在性檢查
    // ========================================

    /**
     * 檢查 LINE User ID 是否已存在
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return true 表示已存在
     */
    boolean existsByTenantIdAndLineUserIdAndDeletedAtIsNull(String tenantId, String lineUserId);

    /**
     * 檢查顧客是否已綁定 LINE
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID
     * @return true 表示已綁定
     */
    boolean existsByTenantIdAndCustomerIdAndDeletedAtIsNull(String tenantId, String customerId);

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計租戶的追蹤中用戶數量
     *
     * @param tenantId 租戶 ID
     * @return 數量
     */
    long countByTenantIdAndIsFollowedTrueAndDeletedAtIsNull(String tenantId);

    /**
     * 統計租戶的所有用戶數量
     *
     * @param tenantId 租戶 ID
     * @return 數量
     */
    long countByTenantIdAndDeletedAtIsNull(String tenantId);

    /**
     * 統計指定時間區間內的新追蹤用戶數量
     *
     * @param tenantId  租戶 ID
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 數量
     */
    @Query("""
            SELECT COUNT(lu) FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.followedAt BETWEEN :startTime AND :endTime
            """)
    long countNewFollowersByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ========================================
    // 批量查詢
    // ========================================

    /**
     * 根據 LINE User ID 列表批量查詢
     *
     * @param tenantId    租戶 ID
     * @param lineUserIds LINE User ID 列表
     * @return LINE 用戶列表
     */
    @Query("""
            SELECT lu FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.lineUserId IN :lineUserIds
            """)
    List<LineUser> findByTenantIdAndLineUserIdIn(
            @Param("tenantId") String tenantId,
            @Param("lineUserIds") List<String> lineUserIds
    );

    /**
     * 查詢可推送訊息的用戶
     *
     * @param tenantId 租戶 ID
     * @return LINE 用戶列表
     */
    @Query("""
            SELECT lu FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.isFollowed = true
            """)
    List<LineUser> findPushableUsersByTenantId(@Param("tenantId") String tenantId);

    // ========================================
    // 行銷推播查詢
    // ========================================

    /**
     * 統計追蹤中用戶數量
     */
    long countByTenantIdAndIsFollowedAndDeletedAtIsNull(String tenantId, Boolean isFollowed);

    /**
     * 查詢追蹤中用戶
     */
    List<LineUser> findByTenantIdAndIsFollowedAndDeletedAtIsNull(String tenantId, Boolean isFollowed);

    /**
     * 依會員等級統計追蹤中用戶數量
     */
    @Query("""
            SELECT COUNT(lu) FROM LineUser lu
            JOIN Customer c ON lu.customerId = c.id
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.isFollowed = :isFollowed
            AND c.membershipLevelId = :membershipLevelId
            """)
    long countByTenantIdAndMembershipLevelAndIsFollowedAndDeletedAtIsNull(
            @Param("tenantId") String tenantId,
            @Param("membershipLevelId") String membershipLevelId,
            @Param("isFollowed") Boolean isFollowed
    );

    /**
     * 依會員等級查詢追蹤中用戶
     */
    @Query("""
            SELECT lu FROM LineUser lu
            JOIN Customer c ON lu.customerId = c.id
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.isFollowed = :isFollowed
            AND c.membershipLevelId = :membershipLevelId
            """)
    List<LineUser> findByTenantIdAndMembershipLevelAndIsFollowedAndDeletedAtIsNull(
            @Param("tenantId") String tenantId,
            @Param("membershipLevelId") String membershipLevelId,
            @Param("isFollowed") Boolean isFollowed
    );

    /**
     * 依 LINE User ID 列表查詢
     */
    @Query("""
            SELECT lu FROM LineUser lu
            WHERE lu.tenantId = :tenantId
            AND lu.deletedAt IS NULL
            AND lu.lineUserId IN :lineUserIds
            """)
    List<LineUser> findByTenantIdAndLineUserIdInAndDeletedAtIsNull(
            @Param("tenantId") String tenantId,
            @Param("lineUserIds") List<String> lineUserIds
    );

    /**
     * 依標籤統計追蹤中用戶數量
     */
    @Query(value = """
            SELECT COUNT(DISTINCT lu.id)
            FROM line_users lu
            JOIN customers c ON lu.customer_id = c.id
            WHERE lu.tenant_id = :tenantId
            AND lu.deleted_at IS NULL
            AND lu.is_followed = true
            AND c.tags LIKE CONCAT('%', :tag, '%')
            """, nativeQuery = true)
    long countByTenantIdAndTagAndIsFollowedAndDeletedAtIsNull(
            @Param("tenantId") String tenantId,
            @Param("tag") String tag
    );

    /**
     * 依標籤查詢追蹤中用戶
     */
    @Query(value = """
            SELECT lu.*
            FROM line_users lu
            JOIN customers c ON lu.customer_id = c.id
            WHERE lu.tenant_id = :tenantId
            AND lu.deleted_at IS NULL
            AND lu.is_followed = true
            AND c.tags LIKE CONCAT('%', :tag, '%')
            """, nativeQuery = true)
    List<LineUser> findByTenantIdAndTagAndIsFollowedAndDeletedAtIsNull(
            @Param("tenantId") String tenantId,
            @Param("tag") String tag
    );
}
