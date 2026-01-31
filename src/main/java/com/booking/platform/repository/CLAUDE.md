# Repository 規範

## 查詢原則

1. **必須包含 `tenant_id`**
2. **必須排除已刪除**：`DeletedAtIsNull`
3. **用投影減少資料傳輸**
4. **用 EXISTS 檢查存在性**

## 命名

```java
findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)
```

## 投影查詢

```java
@Query("""
    SELECT new com.xxx.XxxResponse(e.id, e.name)
    FROM XxxEntity e
    WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL
    """)
Page<XxxResponse> findListItems(@Param("tenantId") String tenantId, Pageable pageable);
```

## JOIN FETCH

```java
@Query("SELECT e FROM Xxx e LEFT JOIN FETCH e.category WHERE e.id = :id")
Optional<Xxx> findByIdWithDetails(@Param("id") String id);
```

## 主要 Repository

| Repository | 用途 |
|------------|------|
| TenantRepository | 店家 |
| AdminUserRepository | 超管帳號 |
| BookingRepository | 預約 |
| CustomerRepository | 顧客 |
| StaffRepository | 員工 |
| ServiceItemRepository | 服務 |
| TenantLineConfigRepository | LINE 設定 |
