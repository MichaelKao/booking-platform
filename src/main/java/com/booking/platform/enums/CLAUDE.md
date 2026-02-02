# Enums 規範

## 命名規範

- **類別**：單數名詞 (`BookingStatus`)
- **值**：大寫底線 (`PENDING_CONFIRMATION`)

---

## Enum 列表 (26 個)

### 業務狀態

| Enum | 值 | 說明 |
|------|-----|------|
| BookingStatus | PENDING_CONFIRMATION, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW | 預約狀態 |
| CustomerStatus | ACTIVE, BLOCKED | 顧客狀態 |
| StaffStatus | ACTIVE, INACTIVE | 員工狀態 |
| ServiceStatus | ACTIVE, INACTIVE | 服務狀態 |
| ProductStatus | ON_SALE, OFF_SHELF, ARCHIVED | 商品狀態 |
| CouponStatus | DRAFT, PUBLISHED, PAUSED, ENDED | 票券狀態 |
| CouponInstanceStatus | UNUSED, USED, EXPIRED, VOIDED | 票券實例狀態 |
| CampaignStatus | DRAFT, PUBLISHED, PAUSED, ENDED | 行銷活動狀態 |

### 員工相關

| Enum | 值 | 說明 |
|------|-----|------|
| LeaveType | PERSONAL, SICK, VACATION, ANNUAL, OTHER | 請假類型（事假、病假、休假、特休、其他） |

### 系統狀態

| Enum | 值 | 說明 |
|------|-----|------|
| TenantStatus | ACTIVE, SUSPENDED, FROZEN | 租戶狀態 |
| FeatureStatus | ACTIVE, SUSPENDED, EXPIRED, INACTIVE | 功能狀態 |
| TopUpStatus | PENDING, APPROVED, REJECTED | 儲值狀態 |

### 功能代碼

| Enum | 值 |
|------|-----|
| FeatureCode | BASIC_REPORT, LINE_BOOKING, COUPON_MANAGEMENT, CAMPAIGN, ADVANCE_BOOKING, MEMBERSHIP, PRODUCT_SALES, ADVANCED_REPORT, LINE_NOTIFICATION, SMS_NOTIFICATION, EMAIL_NOTIFICATION, MULTI_STAFF, CUSTOM_BRANDING |

### 分類與類型

| Enum | 值 | 說明 |
|------|-----|------|
| ProductCategory | VOUCHER, MERCHANDISE, SERVICE | 商品分類 |
| CouponType | PERCENTAGE, FIXED_AMOUNT, FREE_ITEM | 票券類型 |
| CampaignType | DISCOUNT, GIFT, LOYALTY, REFERRAL | 活動類型 |
| Gender | MALE, FEMALE, UNKNOWN | 性別 |

### 行銷推播相關

| Enum | 值 | 說明 |
|------|-----|------|
| MarketingPushStatus | DRAFT, SCHEDULED, SENT, CANCELLED | 推播狀態 |
| MarketingPushTargetType | ALL, ACTIVE, INACTIVE, MEMBERSHIP_LEVEL | 推播目標類型 |

### SMS 相關

| Enum | 值 | 說明 |
|------|-----|------|
| SmsStatus | PENDING, SENT, FAILED | 簡訊發送狀態 |
| SmsType | BOOKING_CONFIRMATION, BOOKING_REMINDER, MARKETING | 簡訊類型 |

### 金流相關

| Enum | 值 | 說明 |
|------|-----|------|
| PaymentStatus | PENDING, PAID, FAILED, REFUNDED | 支付狀態 |
| PaymentType | CREDIT_CARD, ATM, CVS, BARCODE | 支付方式 |

### LINE 相關

| Enum | 值 | 說明 |
|------|-----|------|
| ConversationState | IDLE, SELECTING_SERVICE, SELECTING_STAFF, SELECTING_DATE, SELECTING_TIME, CONFIRMING_BOOKING, CONFIRMING_CANCEL_BOOKING, BROWSING_PRODUCTS, VIEWING_PRODUCT_DETAIL, SELECTING_QUANTITY, CONFIRMING_PURCHASE, BROWSING_COUPONS, VIEWING_MY_COUPONS, VIEWING_MEMBER_INFO | 對話狀態 |
| LineEventType | MESSAGE, FOLLOW, UNFOLLOW, POSTBACK | LINE 事件類型 |
| LineConfigStatus | ACTIVE, INACTIVE | LINE 設定狀態 |

---

## 結構範例

```java
@Getter
@RequiredArgsConstructor
public enum BookingStatus {
    PENDING_CONFIRMATION("待確認"),
    CONFIRMED("已確認"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    NO_SHOW("未到場");

    private final String description;
}
```

---

## 狀態流轉

### 預約狀態流轉

```
PENDING_CONFIRMATION → CONFIRMED → COMPLETED
                    ↘          ↘
                     CANCELLED   NO_SHOW
```

### 票券狀態流轉

```
DRAFT → PUBLISHED → PAUSED → ENDED
              ↓
           ENDED
```

### 活動狀態流轉

```
DRAFT → PUBLISHED → PAUSED → ENDED
              ↓
           ENDED
```

### 票券實例狀態流轉

```
UNUSED → USED
      ↘
       EXPIRED
      ↘
       VOIDED
```

---

## 使用範例

```java
// Entity 中使用
@Enumerated(EnumType.STRING)
@Column(name = "status")
private BookingStatus status;

// 查詢中使用
List<Booking> findByTenantIdAndStatusAndDeletedAtIsNull(
    String tenantId, BookingStatus status);

// 業務邏輯中使用
if (booking.getStatus() == BookingStatus.PENDING_CONFIRMATION) {
    booking.setStatus(BookingStatus.CONFIRMED);
}
```
