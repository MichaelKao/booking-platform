# Repository 規範

## 查詢原則

1. 所有查詢都要包含 `tenant_id` 條件
2. 所有查詢都要排除已刪除資料（`deleted_at IS NULL`）
3. 使用投影查詢減少資料傳輸
4. 使用 `EXISTS` 檢查存在性

## 命名規則
```java
// 基本查詢
findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)

// 存在性檢查
existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, name)

// 投影查詢
findListItems(tenantId, status, pageable)
```

## 效能優化
```java
// 使用 COUNT 而非載入 Entity
@Query("SELECT COUNT(e) FROM XxxEntity e WHERE ...")
long countByXxx(...);

// 使用投影
@Query("SELECT new com.xxx.XxxResponse(e.id, e.name) FROM XxxEntity e WHERE ...")
Page<XxxResponse> findListItems(...);

// 使用 JOIN FETCH 避免 N+1
@Query("SELECT e FROM XxxEntity e LEFT JOIN FETCH e.category WHERE e.id = :id")
Optional<XxxEntity> findByIdWithDetails(@Param("id") String id);
```