# Repository 規範

## 查詢原則

1. **必須包含 `tenant_id`**
2. **必須排除已刪除**：`DeletedAtIsNull`
3. **用投影減少資料傳輸**
4. **用 EXISTS 檢查存在性**
5. **用 JOIN FETCH 避免 N+1**

---

## Repository 列表 (19 個)

### 租戶相關

| Repository | Entity | 說明 |
|------------|--------|------|
| TenantRepository | Tenant | 店家查詢 |
| AdminUserRepository | AdminUser | 超級管理員查詢 |

### 員工相關

| Repository | Entity | 說明 |
|------------|--------|------|
| StaffRepository | Staff | 員工查詢 |
| StaffScheduleRepository | StaffSchedule | 員工每週排班查詢 |
| StaffLeaveRepository | StaffLeave | 員工特定日期請假查詢 |

### 業務相關

| Repository | Entity | 說明 |
|------------|--------|------|
| BookingRepository | Booking | 預約查詢 |
| CustomerRepository | Customer | 顧客查詢 |
| ServiceCategoryRepository | ServiceCategory | 服務分類查詢 |
| ServiceItemRepository | ServiceItem | 服務項目查詢 |
| ProductRepository | Product | 商品查詢 |

### 行銷相關

| Repository | Entity | 說明 |
|------------|--------|------|
| CouponRepository | Coupon | 票券查詢 |
| CouponInstanceRepository | CouponInstance | 票券實例查詢 |
| CampaignRepository | Campaign | 行銷活動查詢 |
| MembershipLevelRepository | MembershipLevel | 會員等級查詢 |

### 系統相關

| Repository | Entity | 說明 |
|------------|--------|------|
| FeatureRepository | Feature | 功能定義查詢 |
| TenantFeatureRepository | TenantFeature | 租戶功能訂閱查詢 |
| PointTopUpRepository | PointTopUp | 儲值申請查詢 |
| PointTransactionRepository | PointTransaction | 點數交易查詢 |

### LINE 相關

| Repository | Entity | 說明 |
|------------|--------|------|
| TenantLineConfigRepository | TenantLineConfig | LINE 設定查詢 |
| LineUserRepository | LineUser | LINE 用戶查詢 |

---

## 命名規範

```java
// 基本查詢
findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
findByTenantIdAndDeletedAtIsNull(tenantId)
findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status)

// 存在性檢查
existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)
existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, name)

// 計數
countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status)
countByTenantIdAndDeletedAtIsNull(tenantId)
```

---

## 員工請假查詢

```java
// 查詢員工特定日期請假
Optional<StaffLeave> findByStaffIdAndLeaveDateAndDeletedAtIsNull(String staffId, LocalDate date);

// 查詢日期範圍內的請假
@Query("SELECT sl FROM StaffLeave sl WHERE sl.staffId = :staffId " +
       "AND sl.leaveDate BETWEEN :startDate AND :endDate " +
       "AND sl.deletedAt IS NULL ORDER BY sl.leaveDate")
List<StaffLeave> findByStaffIdAndDateRange(String staffId, LocalDate startDate, LocalDate endDate);

// 檢查是否請假
@Query("SELECT COUNT(sl) > 0 FROM StaffLeave sl WHERE sl.staffId = :staffId " +
       "AND sl.leaveDate = :date AND sl.isFullDay = true AND sl.deletedAt IS NULL")
boolean isStaffOnLeave(String staffId, LocalDate date);
```

---

## 投影查詢

```java
@Query("""
    SELECT new com.booking.platform.dto.response.XxxResponse(
        e.id, e.name, e.status
    )
    FROM XxxEntity e
    WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL
    ORDER BY e.createdAt DESC
    """)
Page<XxxResponse> findListItems(@Param("tenantId") String tenantId, Pageable pageable);
```

---

## JOIN FETCH (避免 N+1)

```java
@Query("""
    SELECT b FROM Booking b
    LEFT JOIN FETCH b.customer
    LEFT JOIN FETCH b.staff
    LEFT JOIN FETCH b.serviceItem
    WHERE b.id = :id AND b.tenantId = :tenantId AND b.deletedAt IS NULL
    """)
Optional<Booking> findByIdWithDetails(@Param("id") String id, @Param("tenantId") String tenantId);
```

---

## 常用查詢範例

### 分頁查詢

```java
Page<Customer> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
    String tenantId, Pageable pageable);
```

### 日期範圍查詢

```java
@Query("""
    SELECT b FROM Booking b
    WHERE b.tenantId = :tenantId
      AND b.bookingDate BETWEEN :startDate AND :endDate
      AND b.deletedAt IS NULL
    """)
List<Booking> findByDateRange(
    @Param("tenantId") String tenantId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
```

### 統計查詢

```java
@Query("""
    SELECT COUNT(b) FROM Booking b
    WHERE b.tenantId = :tenantId
      AND b.status = :status
      AND b.bookingDate = :date
      AND b.deletedAt IS NULL
    """)
long countByTenantIdAndStatusAndDate(
    @Param("tenantId") String tenantId,
    @Param("status") BookingStatus status,
    @Param("date") LocalDate date);
```

### EXISTS 檢查

```java
@Query("""
    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
    FROM Booking b
    WHERE b.tenantId = :tenantId
      AND b.staffId = :staffId
      AND b.bookingDate = :date
      AND b.startTime < :endTime
      AND b.endTime > :startTime
      AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
      AND b.deletedAt IS NULL
    """)
boolean existsConflictingBooking(
    @Param("tenantId") String tenantId,
    @Param("staffId") String staffId,
    @Param("date") LocalDate date,
    @Param("startTime") LocalTime startTime,
    @Param("endTime") LocalTime endTime);
```
