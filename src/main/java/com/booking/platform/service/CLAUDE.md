# Service 規範

## 必要標註
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class XxxService {
```

## 方法結構

使用分區註解：
```java
@Transactional
public XxxResponse create(CreateXxxRequest request) {
    
    // ========================================
    // 1. 取得當前租戶
    // ========================================
    
    String tenantId = TenantContext.getTenantId();
    
    // ========================================
    // 2. 驗證業務規則
    // ========================================
    
    // ...
    
    // ========================================
    // 3. 建立資料
    // ========================================
    
    // ...
}
```

## 必要步驟

1. 取得租戶 ID
2. 驗證業務規則
3. 執行主要邏輯
4. 記錄稽核日誌
5. 清除相關快取
6. 發送通知（如需要）

## 例外處理
```java
throw new BusinessException(ErrorCode.XXX_ERROR, "錯誤訊息");
throw new ResourceNotFoundException(ErrorCode.XXX_NOT_FOUND, "找不到資料");
```