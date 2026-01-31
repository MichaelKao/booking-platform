# Entity 規範

## 繼承結構

所有業務 Entity 繼承 `BaseEntity`：

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 軟刪除
}
```

**例外**：`TenantLineConfig` 使用 `tenantId` 作為主鍵

---

## 標註

```java
@Entity
@Table(name = "xxx", indexes = {
    @Index(name = "idx_xxx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_xxx_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Xxx extends BaseEntity {
    // ...
}
```

---

## Entity 列表 (18 個)

### 租戶相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Tenant | tenants | 店家資訊 |
| AdminUser | admin_users | 超級管理員 |

### 人員相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Staff | staff | 員工資訊 |

### 預約相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Booking | bookings | 預約記錄 |

### 顧客相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Customer | customers | 顧客資訊 |
| MembershipLevel | membership_levels | 會員等級定義 |
| PointTransaction | point_transactions | 點數交易記錄 |

### 服務相關

| Entity | 表名 | 說明 |
|--------|------|------|
| ServiceCategory | service_categories | 服務分類 |
| ServiceItem | service_items | 服務項目 |

### 商品相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Product | products | 商品資訊 |

### 行銷相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Coupon | coupons | 票券定義 |
| CouponInstance | coupon_instances | 票券實例 |
| Campaign | campaigns | 行銷活動 |

### 系統相關

| Entity | 表名 | 說明 |
|--------|------|------|
| Feature | features | 功能定義 |
| TenantFeature | tenant_features | 租戶功能訂閱狀態 |
| PointTopUp | point_topups | 儲值申請記錄 |

### LINE 相關

| Entity | 表名 | 說明 |
|--------|------|------|
| TenantLineConfig | tenant_line_configs | 店家 LINE 設定 (加密) |
| LineUser | line_users | LINE 用戶關聯 |

---

## 目錄結構

```
entity/
├── system/              # 系統實體
│   ├── Feature
│   ├── TenantFeature
│   └── PointTopUp
├── tenant/              # 租戶實體
│   └── Tenant
└── /                    # 業務實體
    ├── AdminUser
    ├── Booking
    ├── Campaign
    ├── Coupon
    ├── CouponInstance
    ├── Customer
    ├── LineUser
    ├── MembershipLevel
    ├── PointTransaction
    ├── Product
    ├── ServiceCategory
    ├── ServiceItem
    ├── Staff
    └── TenantLineConfig
```

---

## 索引規範

```java
@Table(name = "xxx", indexes = {
    @Index(name = "idx_xxx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_xxx_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_xxx_tenant_status", columnList = "tenant_id, status")
})
```

---

## LINE Entity

```java
// TenantLineConfig - Token 加密儲存
@Entity
@Table(name = "tenant_line_configs")
public class TenantLineConfig {
    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    private String channelId;
    private String channelSecret;       // AES-256-GCM 加密
    private String channelAccessToken;  // AES-256-GCM 加密

    @Enumerated(EnumType.STRING)
    private LineConfigStatus status;
}

// LineUser - LINE 用戶與顧客關聯
@Entity
@Table(name = "line_users")
public class LineUser extends BaseEntity {
    private String lineUserId;    // LINE 平台 ID
    private String customerId;    // 關聯 Customer（可 null）
    private String displayName;
    private String pictureUrl;
    private Boolean isFollowed;
}
```

---

## 軟刪除

```java
// 刪除
entity.setDeletedAt(LocalDateTime.now());
repository.save(entity);

// 查詢排除已刪除
findByTenantIdAndDeletedAtIsNull(tenantId);
```
