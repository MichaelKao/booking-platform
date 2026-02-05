# Controller 規範

## 標註

```java
@RestController  // API Controller
@Controller      // 頁面 Controller
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
@Validated
@Slf4j
```

## 目錄結構 (30 個 Controller)

| 目錄 | Controller | 用途 |
|------|------------|------|
| admin/ | AdminTenantController | 租戶管理 |
| admin/ | AdminFeatureController | 功能管理 |
| admin/ | AdminPointController | 點數/儲值管理 |
| admin/ | AdminDashboardController | 儀表板 |
| auth/ | AuthController | 認證 (登入/註冊/密碼) |
| line/ | LineWebhookController | LINE Webhook |
| page/ | AdminPageController | 超管頁面路由 |
| page/ | TenantPageController | 店家頁面路由 |
| tenant/ | LineConfigController | LINE 設定 |
| / | BookingController | 預約管理 |
| / | CustomerController | 顧客管理 |
| / | StaffController | 員工管理 |
| / | ServiceItemController | 服務項目 |
| / | ServiceCategoryController | 服務分類 |
| / | ProductController | 商品管理 |
| / | CouponController | 票券管理 |
| / | CampaignController | 行銷活動 |
| / | MarketingController | 行銷推播 |
| / | MembershipLevelController | 會員等級 |
| / | PointController | 點數管理 |
| / | ReportController | 報表 |
| / | SettingsController | 店家設定 |
| / | FeatureStoreController | 功能商店 |
| / | NotificationController | SSE 即時通知 |
| / | ExportController | 匯出 (Excel/PDF) |
| / | PaymentController | 金流支付 |
| / | PublicBookingController | 公開預約頁面 (自助取消) |
| / | HealthController | 健康檢查 |
| / | FaviconController | Favicon |

---

## API 端點詳細

### 認證 (`/api/auth/`)
```
POST /login              # 統一登入
POST /admin/login        # 超管登入
POST /tenant/login       # 店家登入
POST /tenant/register    # 店家註冊
POST /forgot-password    # 忘記密碼
POST /reset-password     # 重設密碼
POST /change-password    # 更改密碼
POST /refresh            # 刷新 Token
POST /logout             # 登出
```

### 預約 (`/api/bookings`)
```
GET    /                           # 列表 (分頁)
GET    /{id}                       # 詳情
GET    /staff/{staffId}/date/{date}  # 員工特定日期預約
GET    /calendar                   # 行事曆資料
POST   /                           # 建立
POST   /{id}/confirm               # 確認
POST   /{id}/complete              # 完成
POST   /{id}/cancel                # 取消
POST   /{id}/no-show               # 標記爽約
```

### 顧客 (`/api/customers`)
```
GET    /                    # 列表 (分頁)
GET    /{id}                # 詳情
GET    /by-line-user/{lineUserId}  # 依 LINE User ID
GET    /birthdays/today     # 今日壽星
POST   /                    # 建立
PUT    /{id}                # 更新
DELETE /{id}                # 刪除
POST   /points/adjust       # 調整點數
POST   /{id}/points/add     # 增加點數
POST   /{id}/points/deduct  # 扣除點數
POST   /{id}/block          # 封鎖
POST   /{id}/unblock        # 解除封鎖
```

### 員工 (`/api/staff`)
```
GET    /           # 列表 (分頁)
GET    /{id}       # 詳情
GET    /bookable   # 可預約員工
POST   /           # 建立
PUT    /{id}       # 更新
DELETE /{id}       # 刪除
```

### 服務 (`/api/services`)
```
GET    /           # 列表 (分頁)
GET    /{id}       # 詳情
GET    /bookable   # 可預約服務
POST   /           # 建立
PUT    /{id}       # 更新
DELETE /{id}       # 刪除
```

### 商品 (`/api/products`)
```
GET    /                    # 列表 (分頁)
GET    /{id}                # 詳情
GET    /on-sale             # 上架中
GET    /low-stock           # 低庫存
GET    /category/{category} # 依分類
POST   /                    # 建立
PUT    /{id}                # 更新
DELETE /{id}                # 刪除
POST   /{id}/on-sale        # 上架
POST   /{id}/off-shelf      # 下架
POST   /{id}/adjust-stock   # 調整庫存
```

### 票券 (`/api/coupons`)
```
GET    /                           # 列表 (分頁)
GET    /{id}                       # 詳情
GET    /available                  # 可發放票券
POST   /                           # 建立
PUT    /{id}                       # 更新
DELETE /{id}                       # 刪除
POST   /{id}/publish               # 發布
POST   /{id}/pause                 # 暫停
POST   /{id}/resume                # 恢復
POST   /{id}/issue                 # 發放給顧客
POST   /instances/{instanceId}/redeem  # 核銷 (依實例 ID)
POST   /redeem-by-code             # 核銷 (依代碼)
GET    /customers/{customerId}     # 顧客的票券
GET    /customers/{customerId}/usable  # 顧客可用票券
```

### 行銷活動 (`/api/campaigns`)
```
GET    /            # 列表 (分頁)
GET    /{id}        # 詳情
GET    /active      # 進行中的活動
POST   /            # 建立
PUT    /{id}        # 更新
DELETE /{id}        # 刪除
POST   /{id}/publish  # 發布
POST   /{id}/pause    # 暫停
POST   /{id}/resume   # 恢復
POST   /{id}/end      # 結束
```

### 會員等級 (`/api/membership-levels`)
```
GET    /                     # 所有等級
GET    /{id}                 # 詳情
GET    /default              # 預設等級
POST   /                     # 建立
PUT    /{id}                 # 更新
DELETE /{id}                 # 刪除
POST   /{id}/toggle-active   # 切換啟用
POST   /sort-order           # 更新排序
```

### 報表 (`/api/reports`)
```
GET /summary       # 報表摘要 (時間範圍)
GET /dashboard     # 儀表板統計
GET /today         # 今日統計
GET /weekly        # 本週統計
GET /monthly       # 本月統計
GET /daily         # 每日報表 (趨勢)
GET /top-services  # 熱門服務
GET /top-staff     # 熱門員工
```

### 點數 (`/api/points`)
```
GET  /balance       # 點數餘額
POST /topup         # 申請儲值
GET  /topups        # 儲值記錄
GET  /transactions  # 交易記錄
```

### 功能商店 (`/api/feature-store`)
```
GET  /              # 功能列表
GET  /{code}        # 功能詳情
POST /{code}/apply  # 申請訂閱
POST /{code}/cancel # 取消訂閱
```

### 設定 (`/api/settings`)
```
GET /               # 取得設定
PUT /               # 更新設定
GET /line           # LINE 設定
PUT /line           # 更新 LINE 設定
POST /line/activate   # 啟用 LINE Bot
POST /line/deactivate # 停用 LINE Bot
POST /line/test       # 測試 LINE Bot 連線
GET  /line/rich-menu           # 取得 Rich Menu 資訊
POST /line/rich-menu/create    # 建立 Rich Menu（使用主題配色）
POST /line/rich-menu/upload-image  # 上傳自訂 Rich Menu 圖片
DELETE /line/rich-menu         # 刪除 Rich Menu
```

### 即時通知 (`/api/notifications`)
```
GET /stream         # SSE 訂閱（店家後台即時通知）
```

### 行銷推播 (`/api/marketing`)
```
GET    /pushes              # 推播列表
POST   /pushes              # 建立推播
GET    /pushes/{id}         # 推播詳情
DELETE /pushes/{id}         # 取消推播
POST   /pushes/{id}/send    # 立即發送
```

### 匯出 (`/api/export`)
```
GET /bookings/excel   # 匯出預約 Excel (startDate, endDate, status)
GET /bookings/pdf     # 匯出預約 PDF
GET /reports/excel    # 匯出報表 Excel (range=month)
GET /reports/pdf      # 匯出報表 PDF
GET /customers/excel  # 匯出顧客 Excel
```

### 金流 (`/api/payments`)
```
POST /create          # 建立支付
POST /notify          # ECPay 回調通知
GET  /{id}            # 查詢支付狀態
GET  /                # 支付記錄列表
```

### 公開預約 (`/booking`)
```
GET  /cancel/{token}  # 顯示取消頁面
POST /cancel/{token}  # 執行取消預約
```

---

## 超級管理 API (`/api/admin/`)

### 租戶管理
```
GET    /tenants                    # 列表 (分頁)
GET    /tenants/{id}               # 詳情
POST   /tenants                    # 建立
PUT    /tenants/{id}               # 更新
DELETE /tenants/{id}               # 刪除
PUT    /tenants/{id}/status        # 更新狀態
POST   /tenants/{id}/activate      # 啟用
POST   /tenants/{id}/suspend       # 停用
POST   /tenants/{id}/freeze        # 凍結
POST   /tenants/{id}/points/add    # 增加點數
GET    /tenants/{id}/topups        # 儲值記錄
```

### 功能管理
```
GET  /features              # 所有功能定義
GET  /features/free         # 免費功能
GET  /features/paid         # 付費功能
GET  /features/{code}       # 單一功能
PUT  /features/{code}       # 更新功能
POST /features/initialize   # 初始化功能

POST /tenants/{tenantId}/features/{featureCode}/enable    # 啟用
POST /tenants/{tenantId}/features/{featureCode}/disable   # 停用
POST /tenants/{tenantId}/features/{featureCode}/suspend   # 凍結
POST /tenants/{tenantId}/features/{featureCode}/unsuspend # 解凍
POST /tenants/batch/features/{featureCode}/enable         # 批次啟用
POST /tenants/batch/features/{featureCode}/disable        # 批次停用
```

### 儲值管理
```
GET  /point-topups              # 所有申請
GET  /point-topups/pending      # 待審核
GET  /point-topups/pending/count  # 待審核數量
GET  /point-topups/stats        # 統計資料
GET  /point-topups/{id}         # 詳情
POST /point-topups/{id}/approve # 審核通過
POST /point-topups/{id}/reject  # 審核駁回
POST /tenants/{tenantId}/points/adjust  # 手動調整
```

### 儀表板
```
GET /dashboard  # 儀表板資料
```

---

## 回應格式

```java
// 成功
return ApiResponse.ok(data);
return ApiResponse.ok("操作成功", data);

// 失敗
return ApiResponse.error("ERROR_CODE", "錯誤訊息");
```

## 分頁限制

```java
// 限制最大 100 筆
size = Math.min(size, 100);
```

## 頁面 Controller 範例

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
