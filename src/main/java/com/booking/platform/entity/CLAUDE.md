# Entity 規範

## 必要標註
```java
@Entity
@Table(name = "xxx", indexes = {...})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XxxEntity extends BaseEntity {
```

## 繼承 BaseEntity

所有業務 Entity 都要繼承 `BaseEntity`，自動擁有：
- id（UUID）
- tenantId（租戶 ID）
- createdAt, createdBy
- updatedAt, updatedBy
- deletedAt（軟刪除）

## 索引設計
```java
@Table(
    name = "xxx",
    indexes = {
        // 列表查詢
        @Index(name = "idx_xxx_tenant_status", columnList = "tenant_id, status"),
        // 軟刪除過濾
        @Index(name = "idx_xxx_tenant_deleted", columnList = "tenant_id, deleted_at")
    }
)
```

## 欄位註解
```java
/**
 * 欄位說明
 */
@Column(name = "column_name", nullable = false, length = 100)
private String fieldName;
```