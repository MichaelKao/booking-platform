# CRUD 模組模板

建立新的 CRUD 模組時，請依照此模板建立所有檔案。

## 需要建立的檔案

假設模組名稱為 `Product`：

### 1. Entity
```
entity/catalog/Product.java
```

### 2. Repository
```
repository/ProductRepository.java
```

### 3. Service
```
service/catalog/ProductService.java
```

### 4. Controller
```
controller/tenant/ProductController.java
```

### 5. DTO
```
dto/request/product/CreateProductRequest.java
dto/request/product/UpdateProductRequest.java
dto/response/product/ProductResponse.java
dto/response/product/ProductListItemResponse.java
dto/response/product/ProductDetailResponse.java
```

### 6. Mapper
```
mapper/ProductMapper.java
```

## API 端點規範
```
GET    /api/products           # 列表（分頁）
GET    /api/products/{id}      # 詳情
POST   /api/products           # 建立
PUT    /api/products/{id}      # 更新
DELETE /api/products/{id}      # 刪除（軟刪除）
```

## 必要功能

1. 所有查詢都要加上 `tenant_id` 條件
2. 刪除使用軟刪除
3. 列表支援分頁（最大 100 筆）
4. 列表支援篩選和搜尋
5. 重要操作記錄 AuditLog
6. 適當使用快取

## Entity 必要欄位
```java
// 繼承 BaseEntity，自動擁有：
// - id (UUID)
// - tenantId
// - createdAt, createdBy
// - updatedAt, updatedBy
// - deletedAt (軟刪除)

// 業務欄位範例
private String name;
private String description;
private XxxStatus status;
private Boolean isActive;
```

## Repository 必要方法
```java
// 基本查詢
Optional findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

// 存在性檢查
boolean existsByTenantIdAndNameAndDeletedAtIsNull(String tenantId, String name);

// 投影查詢（列表用）
@Query("SELECT new ...ListItemResponse(...) FROM Entity e WHERE ...")
Page findListItems(..., Pageable pageable);

// 統計
long countByTenantIdAndDeletedAtIsNull(String tenantId);
```

## Service 必要步驟
```java
@Transactional
public XxxResponse create(CreateXxxRequest request) {
    // 1. 取得當前租戶
    // 2. 驗證業務規則
    // 3. 建立 Entity
    // 4. 儲存到資料庫
    // 5. 記錄稽核日誌
    // 6. 清除快取
    // 7. 發送通知（如需要）
    // 8. 返回結果
}
```

## 範例指令
```
請依照 .claude/templates/crud-module.md 模板，建立 Product 模組的完整 CRUD，包含：
- Product Entity（名稱、描述、價格、庫存、狀態、分類ID）
- ProductRepository
- ProductService
- ProductController
- 所有 DTO
- ProductMapper
```
