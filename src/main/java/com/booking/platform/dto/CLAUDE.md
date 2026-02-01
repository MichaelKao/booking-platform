# DTO 規範

## 目錄結構

```
dto/
├── request/    # 請求 DTO
└── response/   # 回應 DTO
```

---

## 命名規範

| 類型 | 命名 | 範例 |
|------|------|------|
| 新增請求 | CreateXxxRequest | CreateBookingRequest |
| 更新請求 | UpdateXxxRequest | UpdateBookingRequest |
| 回應 | XxxResponse | BookingResponse |
| 列表項 | XxxListItemResponse | TenantListItemResponse |
| 詳情 | XxxDetailResponse | TenantDetailResponse |

---

## 標註

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateXxxRequest {
    @NotBlank(message = "欄位不能為空")
    @Size(max = 100, message = "長度不能超過100")
    private String name;

    @NotNull(message = "必填")
    private LocalDate date;
}
```

---

## DTO 列表 (60+ 個)

### 認證相關

| DTO | 說明 |
|-----|------|
| LoginRequest | 登入請求 (username, password, type) |
| LoginResponse | 登入回應 (accessToken, refreshToken, expiresIn, user) |
| RefreshTokenRequest | Token 刷新請求 |
| TenantRegisterRequest | 店家註冊請求 |
| ForgotPasswordRequest | 忘記密碼請求 |
| ResetPasswordRequest | 重設密碼請求 |
| ChangePasswordRequest | 更改密碼請求 |

### 預約相關

| DTO | 說明 |
|-----|------|
| CreateBookingRequest | 建立預約 |
| UpdateBookingRequest | 更新預約 |
| BookingResponse | 預約資訊 |

### 顧客相關

| DTO | 說明 |
|-----|------|
| CreateCustomerRequest | 建立顧客 |
| UpdateCustomerRequest | 更新顧客 |
| CustomerResponse | 顧客資訊 |
| AdjustPointsRequest | 調整點數 |

### 員工相關

| DTO | 說明 |
|-----|------|
| CreateStaffRequest | 建立員工 |
| UpdateStaffRequest | 更新員工 |
| StaffResponse | 員工資訊 |

### 服務相關

| DTO | 說明 |
|-----|------|
| CreateServiceItemRequest | 建立服務項目 |
| UpdateServiceItemRequest | 更新服務項目 |
| ServiceItemResponse | 服務項目資訊 |
| ServiceCategoryResponse | 服務分類資訊 |

### 商品相關

| DTO | 說明 |
|-----|------|
| CreateProductRequest | 建立商品 |
| UpdateProductRequest | 更新商品 |
| ProductResponse | 商品資訊 |

### 票券相關

| DTO | 說明 |
|-----|------|
| CreateCouponRequest | 建立票券 |
| UpdateCouponRequest | 更新票券 |
| IssueCouponRequest | 發放票券 |
| RedeemCouponRequest | 核銷票券 |
| CouponResponse | 票券定義資訊 |
| CouponInstanceResponse | 票券實例資訊 |

### 行銷活動相關

| DTO | 說明 |
|-----|------|
| CreateCampaignRequest | 建立活動 |
| UpdateCampaignRequest | 更新活動 |
| CampaignResponse | 活動資訊 |

### 會員等級相關

| DTO | 說明 |
|-----|------|
| CreateMembershipLevelRequest | 建立會員等級 |
| UpdateMembershipLevelRequest | 更新會員等級 |
| MembershipLevelResponse | 會員等級資訊 |

### 租戶相關

| DTO | 說明 |
|-----|------|
| CreateTenantRequest | 建立租戶 |
| UpdateTenantRequest | 更新租戶 |
| TenantResponse | 租戶基本資訊 |
| TenantListItemResponse | 租戶列表項目 |
| TenantDetailResponse | 租戶詳細資訊 |

### 點數相關

| DTO | 說明 |
|-----|------|
| CreatePointTopUpRequest | 申請儲值 |
| ReviewTopUpRequest | 審核儲值 |
| PointBalanceResponse | 點數餘額 |
| PointTopUpResponse | 儲值申請資訊 |
| PointTransactionResponse | 交易記錄 |

### 功能相關

| DTO | 說明 |
|-----|------|
| ApplyFeatureRequest | 申請功能訂閱 |
| UpdateFeatureRequest | 更新功能 (name, description, isActive, isFree, monthlyPoints, icon, category, sortOrder) |
| FeatureResponse | 功能定義 |
| TenantFeatureResponse | 租戶功能訂閱狀態 |
| FeatureStoreItemResponse | 功能商店項目 |

### 設定相關

| DTO | 說明 |
|-----|------|
| UpdateSettingsRequest | 更新設定 |
| SettingsResponse | 設定資訊 |
| SaveLineConfigRequest | 儲存 LINE 設定 |
| LineConfigResponse | LINE 設定資訊 |

### 報表相關

| DTO | 說明 |
|-----|------|
| ReportSummaryResponse | 報表摘要 |
| DailyReportResponse | 每日報表 |
| TopItemResponse | 排名項目 |
| AdminDashboardResponse | 超級管理儀表板 |

### LINE 相關

| DTO | 說明 |
|-----|------|
| ConversationContext | 對話上下文（詳見下方） |

#### ConversationContext 欄位

**預約相關**
| 欄位 | 類型 | 說明 |
|------|------|------|
| selectedServiceId | String | 選擇的服務 ID |
| selectedServiceName | String | 選擇的服務名稱 |
| selectedStaffId | String | 選擇的員工 ID |
| selectedStaffName | String | 選擇的員工名稱 |
| selectedDate | String | 選擇的日期 (YYYY-MM-DD) |
| selectedTime | String | 選擇的時間 (HH:mm) |
| cancelBookingId | String | 欲取消的預約 ID |

**商品相關**
| 欄位 | 類型 | 說明 |
|------|------|------|
| selectedProductId | String | 選擇的商品 ID |
| selectedProductName | String | 選擇的商品名稱 |
| selectedProductPrice | Integer | 選擇的商品價格 |
| selectedQuantity | Integer | 選擇的數量 |

**票券相關**
| 欄位 | 類型 | 說明 |
|------|------|------|
| selectedCouponId | String | 選擇的票券 ID |
| selectedCouponName | String | 選擇的票券名稱 |

---

## 常用驗證標註

| 標註 | 用途 | 範例 |
|------|------|------|
| @NotBlank | 字串必填且非空白 | `@NotBlank(message = "名稱不能為空")` |
| @NotNull | 必填 | `@NotNull(message = "日期必填")` |
| @Size | 長度限制 | `@Size(max = 100, message = "長度不能超過100")` |
| @Email | 信箱格式 | `@Email(message = "請輸入有效的信箱")` |
| @Pattern | 正則驗證 | `@Pattern(regexp = "^[A-Z0-9]+$")` |
| @Min/@Max | 數值範圍 | `@Min(0) @Max(100)` |
| @Positive | 正數 | `@Positive(message = "必須為正數")` |
| @Future | 未來日期 | `@Future(message = "必須為未來日期")` |

---

## Request DTO 範例

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "顧客 ID 不能為空")
    private String customerId;

    @NotBlank(message = "服務項目 ID 不能為空")
    private String serviceItemId;

    private String staffId;  // 可選，不指定員工

    @NotNull(message = "預約日期不能為空")
    private LocalDate bookingDate;

    @NotNull(message = "開始時間不能為空")
    private LocalTime startTime;

    private String note;
}
```

---

## Response DTO 範例

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String id;
    private String customerId;
    private String customerName;
    private String staffId;
    private String staffName;
    private String serviceItemId;
    private String serviceItemName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private String note;
    private LocalDateTime createdAt;
}
```
