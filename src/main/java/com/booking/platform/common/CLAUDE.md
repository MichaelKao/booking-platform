# Common 共用元件

## 目錄結構

```
common/
├── config/        # 設定類
├── exception/     # 例外處理
├── response/      # 統一回應
├── security/      # 安全相關
├── tenant/        # 多租戶
├── entity/        # 基礎實體
└── line/          # LINE 相關
```

---

## 各目錄內容

| 目錄 | 類別 | 說明 |
|------|------|------|
| config/ | SecurityConfig | Spring Security 設定 |
| config/ | RedisConfig | Redis 快取設定 |
| config/ | JacksonConfig | JSON 序列化設定 |
| config/ | AsyncConfig | 非同步任務設定 |
| config/ | RestTemplateConfig | HTTP 客戶端設定 |
| config/ | DataInitializer | 資料初始化 |
| exception/ | BusinessException | 業務例外 |
| exception/ | ResourceNotFoundException | 資源未找到例外 |
| exception/ | GlobalExceptionHandler | 全域例外處理 |
| exception/ | ErrorCode | 統一錯誤代碼 |
| response/ | ApiResponse | 統一 API 回應 |
| response/ | PageResponse | 分頁回應 |
| security/ | JwtTokenProvider | JWT Token 生成與驗證 |
| security/ | JwtAuthenticationFilter | JWT 認證過濾器 |
| tenant/ | TenantContext | 租戶 ID 上下文 |
| tenant/ | TenantFilter | 租戶 ID 提取過濾器 |
| entity/ | BaseEntity | 所有實體的基類 |
| line/ | LineSignatureValidator | LINE 簽章驗證 |

---

## TenantContext 使用

```java
// 取得當前租戶 ID（由 JwtAuthenticationFilter 設定）
String tenantId = TenantContext.getTenantId();

// 手動設定（測試用）
TenantContext.setTenantId(tenantId);

// 清除（Filter 結束時自動清除）
TenantContext.clear();
```

---

## ApiResponse 使用

```java
// 成功
return ApiResponse.ok(data);
return ApiResponse.ok("操作成功", data);

// 錯誤
return ApiResponse.error("ERROR_CODE", "錯誤訊息");
return ApiResponse.error(ErrorCode.BOOKING_NOT_FOUND);

// 回應格式
{
    "success": true,
    "code": "SUCCESS",
    "message": "操作成功",
    "data": { ... }
}
```

---

## 例外處理

```java
// 業務例外
throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "該時段已被預約");

// 資源未找到
throw new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "找不到顧客");

// GlobalExceptionHandler 會攔截並轉換為 ApiResponse
```

---

## ErrorCode 分類

| 前綴 | 類別 | 範例 |
|------|------|------|
| AUTH_ | 認證錯誤 | AUTH_INVALID_TOKEN, AUTH_EXPIRED_TOKEN |
| TENANT_ | 租戶錯誤 | TENANT_NOT_FOUND, TENANT_SUSPENDED |
| BOOKING_ | 預約錯誤 | BOOKING_NOT_FOUND, BOOKING_TIME_CONFLICT |
| CUSTOMER_ | 顧客錯誤 | CUSTOMER_NOT_FOUND, CUSTOMER_BLOCKED |
| STAFF_ | 員工錯誤 | STAFF_NOT_FOUND, STAFF_UNAVAILABLE |
| SERVICE_ | 服務錯誤 | SERVICE_NOT_FOUND, SERVICE_INACTIVE |
| COUPON_ | 票券錯誤 | COUPON_NOT_FOUND, COUPON_EXPIRED |
| POINT_ | 點數錯誤 | POINT_INSUFFICIENT |
| FEATURE_ | 功能錯誤 | FEATURE_NOT_FOUND, FEATURE_NOT_SUBSCRIBED |
| SYS_ | 系統錯誤 | SYS_INTERNAL_ERROR |

---

## JWT 使用

```java
@Autowired
private JwtTokenProvider jwtTokenProvider;

// 生成 Token
String accessToken = jwtTokenProvider.generateAccessToken(userId, username, role, tenantId);
String refreshToken = jwtTokenProvider.generateRefreshToken(userId, role);

// 驗證 Token
boolean isValid = jwtTokenProvider.validateToken(token);

// 從 Token 取得資訊
String userId = jwtTokenProvider.getUserId(token);
String role = jwtTokenProvider.getRole(token);
String tenantId = jwtTokenProvider.getTenantId(token);
```

---

## 加密服務

```java
@Autowired
private EncryptionService encryptionService;

// AES-256-GCM 加密（用於 LINE Token）
String encrypted = encryptionService.encrypt(plainText);
String decrypted = encryptionService.decrypt(encryptedText);
```
