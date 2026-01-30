# Java 程式碼規範

## 總體原則

1. 所有程式碼使用繁體中文註解
2. 註解寫在程式碼上方，不要寫在後面
3. 使用分區註解標示邏輯流程
4. 參考 `.claude/examples/` 的範例風格

## 套件結構
```
com.booking.platform
├── common/          # 共用元件
├── controller/      # API 控制器
├── service/         # 業務邏輯
├── repository/      # 資料存取
├── entity/          # 資料庫實體
├── dto/             # 資料傳輸物件
├── enums/           # 列舉
├── mapper/          # 物件轉換
└── scheduler/       # 排程任務
```

## 依賴注入

使用 `@RequiredArgsConstructor` + `private final`：
```java
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
}
```

## 交易管理

- 類別層級：`@Transactional(readOnly = true)`
- 寫入方法：單獨加 `@Transactional`

## 例外處理

- 業務例外：`BusinessException`
- 找不到資源：`ResourceNotFoundException`
- 權限不足：`AccessDeniedException`