# Java 程式碼規範

## 必要標註

```java
// Controller
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Validated
@Slf4j

// Service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j

// Repository
public interface XxxRepository extends JpaRepository<Xxx, String> { }
```

---

## 套件結構

| 套件 | 內容 |
|------|------|
| common/config | SecurityConfig, RedisConfig, JacksonConfig, AsyncConfig |
| common/exception | BusinessException, ErrorCode, GlobalExceptionHandler |
| common/response | ApiResponse, PageResponse |
| common/security | JwtTokenProvider, JwtAuthenticationFilter |
| common/tenant | TenantContext, TenantFilter |
| controller/admin | 超管 API (4 個 Controller) |
| controller/auth | 認證 API (AuthController) |
| controller/line | LINE Webhook |
| controller/page | 頁面路由 (Admin/Tenant PageController) |
| controller/tenant | 店家 API (LineConfigController) |
| controller/ | 業務 API (21 個 Controller) |
| service/ | 業務邏輯 (36 個 Service) |
| service/admin | 超管服務 |
| service/line | LINE 相關服務 (8 個) |
| service/notification | 通知服務 (4 個) |
| service/export | 匯出服務 (Excel/PDF) |
| service/payment | 金流服務 (ECPay) |
| scheduler/ | 排程任務 (5 個) |
| repository/ | 資料存取 (23 個 Repository) |
| entity/ | 資料庫實體 (23 個 Entity) |
| dto/request | 請求 DTO |
| dto/response | 回應 DTO |
| enums/ | 列舉 (26 個 Enum) |
| mapper/ | Entity <-> DTO 轉換 |

---

## 多租戶

```java
// 取得當前租戶 ID
String tenantId = TenantContext.getTenantId();

// Repository 查詢必須包含 tenantId
findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
```

---

## 例外處理

```java
// 業務例外
throw new BusinessException(ErrorCode.BOOKING_TIME_CONFLICT, "該時段已被預約");

// 資源未找到
throw new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "找不到顧客");
```

---

## 方法結構模板

```java
@Transactional
public XxxResponse create(CreateXxxRequest request) {
    // ========================================
    // 1. 取得租戶
    // ========================================
    String tenantId = TenantContext.getTenantId();
    log.debug("建立 Xxx，租戶 ID：{}", tenantId);

    // ========================================
    // 2. 驗證業務規則
    // ========================================
    validateRequest(request);

    // ========================================
    // 3. 執行主邏輯
    // ========================================
    Xxx entity = mapper.toEntity(request);
    entity.setTenantId(tenantId);
    xxxRepository.save(entity);

    // ========================================
    // 4. 記錄日誌
    // ========================================
    log.info("建立 Xxx 成功，ID：{}", entity.getId());

    return mapper.toResponse(entity);
}
```

---

## 加密

LINE Token 使用 `EncryptionService` 進行 AES-256-GCM 加密儲存：

```java
@Autowired
private EncryptionService encryptionService;

// 加密
String encrypted = encryptionService.encrypt(channelAccessToken);

// 解密
String decrypted = encryptionService.decrypt(encryptedToken);
```

---

## 註解規範

1. 繁體中文撰寫
2. 寫在程式碼上方，不要寫在程式碼後面
3. 使用分區註解標示流程

```java
// ========================================
// 1. 區塊標題
// ========================================
```

---

## 統計

| 項目 | 數量 |
|------|------|
| Controller | 30 |
| Service | 36 |
| Entity | 23 |
| Repository | 23 |
| DTO | 70+ |
| Enum | 26 |
| Scheduler | 5 |
