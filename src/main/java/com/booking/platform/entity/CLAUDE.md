# Entity 規範

## 繼承

所有業務 Entity 繼承 `BaseEntity`：id, tenantId, createdAt, updatedAt, deletedAt

例外：`TenantLineConfig` 用 tenantId 作主鍵

## 標註

```java
@Entity
@Table(name = "xxx")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Xxx extends BaseEntity { }
```

## 目錄

| 目錄 | Entity |
|------|--------|
| tenant/ | Tenant |
| staff/ | Staff, StaffSchedule, StaffLeave |
| service/ | ServiceCategory, ServiceItem |
| booking/ | Booking, BookingHistory |
| customer/ | Customer, Membership, MembershipLevel |
| product/ | ProductCategory, Product |
| marketing/ | Coupon, CouponInstance, Campaign, PointTransaction |
| system/ | Feature, TenantFeature, PointTopUp, AuditLog, AdminUser |
| line/ | TenantLineConfig, LineUser |

## 索引

```java
@Table(name = "xxx", indexes = {
    @Index(name = "idx_xxx_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_xxx_tenant_deleted", columnList = "tenant_id, deleted_at")
})
```

## LINE Entity

```java
// TenantLineConfig - Token 加密儲存
channelSecretEncrypted      // AES-256-GCM
channelAccessTokenEncrypted // AES-256-GCM

// LineUser
lineUserId   // LINE 平台 ID
customerId   // 關聯 Customer（可 null）
```
