# Controller 規範

## 必要標註
```java
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Validated
@Slf4j
public class XxxController {
```

## 回應格式

所有方法回傳 `ApiResponse` 包裝：
```java
@GetMapping
public ApiResponse<PageResponse<XxxResponse>> getList(...) {
    return ApiResponse.ok(xxxService.getList(...));
}
```

## 參數驗證

- 使用 `@Valid` 驗證 RequestBody
- 使用 `@Validated` 啟用方法參數驗證
- 使用 JSR-303 註解（@NotBlank, @Size 等）

## 日誌記錄

寫入操作要記錄日誌：
```java
@PostMapping
public ApiResponse<XxxResponse> create(@Valid @RequestBody CreateXxxRequest request) {
    log.info("收到建立 Xxx 請求：{}", request);
    return ApiResponse.ok(xxxService.create(request));
}
```

## 分頁處理

限制最大分頁大小為 100：
```java
size = Math.min(size, 100);
```