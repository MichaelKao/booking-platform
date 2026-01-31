# Controller 規範

## 標註

```java
@RestController  // API
@Controller      // 頁面
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Validated
@Slf4j
```

## 目錄

| 目錄 | 用途 |
|------|------|
| admin/ | 超管 API：AdminTenantController, AdminFeatureController |
| auth/ | 認證：AuthController |
| page/ | 頁面路由：AdminPageController, TenantPageController |
| line/ | LINE Webhook |

## 回應格式

```java
return ApiResponse.ok(data);
return ApiResponse.ok("訊息", data);
return ApiResponse.error("CODE", "訊息");
```

## 分頁

```java
size = Math.min(size, 100);  // 限制最大 100
```

## 頁面 Controller

```java
@Controller
@RequestMapping("/tenant")
public class TenantPageController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        return "tenant/dashboard";
    }
}
```
