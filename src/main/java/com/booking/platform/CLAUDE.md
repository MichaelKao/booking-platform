# Java 程式碼規範

## 必要標註

```java
@Service/@Controller/@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Service 用
@Slf4j
```

## 套件結構

| 套件 | 內容 |
|------|------|
| common/config | Security, Redis, Jackson, Async |
| common/exception | BusinessException, ErrorCode |
| common/security | JwtTokenProvider, Filter |
| common/tenant | TenantContext |
| controller/admin | 超管 API |
| controller/auth | 認證 API |
| controller/page | 頁面路由 |
| service | 業務邏輯 |

## 多租戶

```java
String tenantId = TenantContext.getTenantId();
// Repository 查詢必須包含 tenantId
```

## 例外

```java
throw new BusinessException(ErrorCode.XXX, "訊息");
throw new ResourceNotFoundException(ErrorCode.XXX_NOT_FOUND, "找不到");
```

## 方法結構

```java
@Transactional
public XxxResponse create(CreateXxxRequest request) {
    // ========================================
    // 1. 取得租戶
    // ========================================
    String tenantId = TenantContext.getTenantId();

    // ========================================
    // 2. 驗證
    // ========================================

    // ========================================
    // 3. 執行
    // ========================================
}
```

## 加密

LINE Token 用 `EncryptionService` AES-256-GCM 加密儲存
