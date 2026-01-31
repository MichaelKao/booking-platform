# Common 共用元件

## 結構

| 目錄 | 內容 |
|------|------|
| config/ | SecurityConfig, RedisConfig, JacksonConfig, AsyncConfig |
| exception/ | BusinessException, ResourceNotFoundException, GlobalExceptionHandler, ErrorCode |
| response/ | ApiResponse, PageResponse |
| security/ | JwtTokenProvider, JwtAuthenticationFilter |
| tenant/ | TenantContext, TenantFilter |

## TenantContext

```java
// 取得當前租戶 ID（由 JwtAuthenticationFilter 設定）
String tenantId = TenantContext.getTenantId();

// 手動設定（測試用）
TenantContext.setTenantId(tenantId);

// 清除（Filter 結束時自動清除）
TenantContext.clear();
```

## ApiResponse

```java
return ApiResponse.ok(data);                    // 成功
return ApiResponse.ok("訊息", data);            // 成功 + 訊息
return ApiResponse.error("CODE", "訊息");       // 錯誤
return ApiResponse.error(ErrorCode.XXX);        // 錯誤（用 ErrorCode）
```

## 例外處理

```java
throw new BusinessException(ErrorCode.XXX, "自訂訊息");
throw new ResourceNotFoundException(ErrorCode.XXX_NOT_FOUND);
// GlobalExceptionHandler 會攔截並轉換為 ApiResponse
```

## ErrorCode 分類

| 前綴 | 類別 |
|------|------|
| AUTH_ | 認證錯誤 |
| TENANT_ | 租戶錯誤 |
| BOOKING_ | 預約錯誤 |
| SYS_ | 系統錯誤 |

## JWT

```java
// JwtTokenProvider
generateAccessToken(userId, username, role, tenantId)
generateRefreshToken(userId, role)
validateToken(token)
getUserId(token), getRole(token), getTenantId(token)
```
