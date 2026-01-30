# CRUD 模組模板

建立新的 CRUD 模組時，請依照此模板建立所有檔案。

## 需要建立的檔案

假設模組名稱為 `Product`：

### Entity
- `entity/catalog/Product.java`

### Repository
- `repository/ProductRepository.java`

### Service
- `service/catalog/ProductService.java`

### Controller
- `controller/tenant/ProductController.java`

### DTO
- `dto/request/product/CreateProductRequest.java`
- `dto/request/product/UpdateProductRequest.java`
- `dto/response/product/ProductResponse.java`
- `dto/response/product/ProductListItemResponse.java`
- `dto/response/product/ProductDetailResponse.java`

### Mapper
- `mapper/ProductMapper.java`

## API 端點
```
GET    /api/products           # 列表（分頁）
GET    /api/products/{id}      # 詳情
POST   /api/products           # 建立
PUT    /api/products/{id}      # 更新
DELETE /api/products/{id}      # 刪除
```

## 必要功能

1. 所有查詢都要加上 tenant_id 條件
2. 刪除使用軟刪除
3. 列表支援分頁和篩選
4. 重要操作記錄 AuditLog
5. 適當使用快取

## 範例指令
```
請依照 .claude/templates/crud-module.md 模板，建立 Product 模組的完整 CRUD，包含：
- Product Entity（商品名稱、描述、價格、庫存、狀態、分類ID）
- ProductRepository
- ProductService
- ProductController
- 所有 DTO
- ProductMapper
```