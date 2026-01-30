/**
 * Repository 範例
 * 
 * <p>所有 Repository 都要遵循此範例的風格
 * 
 * @author Developer
 * @since 1.0.0
 */
public interface ExampleRepository extends JpaRepository<ExampleEntity, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 依 ID 和租戶 ID 查詢
     */
    Optional<ExampleEntity> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    // ========================================
    // 存在性檢查
    // ========================================

    /**
     * 檢查名稱是否存在
     */
    boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

    /**
     * 檢查名稱是否存在（排除指定 ID）
     */
    boolean existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(String tenantId, String name, String excludeId);

    // ========================================
    // 投影查詢（效能優化）
    // ========================================

    /**
     * 查詢列表項目
     * 
     * <p>只查詢列表需要的欄位
     */
    @Query("""
            SELECT new com.booking.platform.dto.response.example.ExampleListItemResponse(
                e.id, e.name, e.status, e.isActive, e.createdAt
            )
            FROM ExampleEntity e
            WHERE e.tenantId = :tenantId
            AND e.deletedAt IS NULL
            AND (:status IS NULL OR e.status = :status)
            AND (:keyword IS NULL OR e.name LIKE %:keyword%)
            ORDER BY e.createdAt DESC
            """)
    Page<ExampleListItemResponse> findListItems(
            @Param("tenantId") String tenantId,
            @Param("status") ExampleStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ========================================
    // 統計查詢
    // ========================================

    /**
     * 統計數量
     */
    @Query("""
            SELECT COUNT(e)
            FROM ExampleEntity e
            WHERE e.tenantId = :tenantId
            AND e.deletedAt IS NULL
            AND (:status IS NULL OR e.status = :status)
            """)
    long countByTenantIdAndStatus(
            @Param("tenantId") String tenantId,
            @Param("status") ExampleStatus status
    );

    // ========================================
    // 批次操作
    // ========================================

    /**
     * 批次軟刪除
     */
    @Modifying
    @Query("""
            UPDATE ExampleEntity e
            SET e.deletedAt = CURRENT_TIMESTAMP
            WHERE e.id IN :ids
            AND e.tenantId = :tenantId
            AND e.deletedAt IS NULL
            """)
    int batchSoftDelete(@Param("ids") List<String> ids, @Param("tenantId") String tenantId);
}