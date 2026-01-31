# Service 規範

## 標註

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
```

## 寫入方法

```java
@Transactional  // 單獨加
public XxxResponse create(...) { }
```

## 必要步驟

1. 取得 `TenantContext.getTenantId()`
2. 驗證業務規則
3. 執行主邏輯
4. 記錄 AuditLog（重要操作）
5. 清快取（如有）
6. 發通知（如需）

## 目錄

| 目錄 | 服務 |
|------|------|
| / | AuthService, TenantService, SettingsService |
| admin/ | AdminTenantService, AdminFeatureService |
| notification/ | EmailService, NotificationService |
| line/ | LineWebhookService, LineConversationService |

## LINE 對話狀態

```java
// Redis Key: line:conversation:{tenantId}:{lineUserId}
// TTL: 30 分鐘
lineConversationService.getState(tenantId, lineUserId);
lineConversationService.setState(tenantId, lineUserId, state, context);
```
