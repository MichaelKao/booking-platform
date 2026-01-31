# Enums 規範

## 命名

類別：單數名詞 (`BookingStatus`)
值：大寫底線 (`PENDING_CONFIRMATION`)

## 主要列舉

| Enum | 值 |
|------|-----|
| TenantStatus | ACTIVE, SUSPENDED, FROZEN |
| BookingStatus | PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW |
| CustomerStatus | ACTIVE, INACTIVE, BLOCKED |
| StaffStatus | ACTIVE, INACTIVE, ON_LEAVE |
| CouponStatus | ACTIVE, INACTIVE, EXPIRED |
| CouponInstanceStatus | AVAILABLE, USED, EXPIRED, CANCELLED |
| CampaignStatus | DRAFT, SCHEDULED, ACTIVE, PAUSED, ENDED |
| ProductStatus | ACTIVE, INACTIVE, OUT_OF_STOCK |
| FeatureCode | LINE_BOT, PUSH_NOTIFICATION, COUPON, CAMPAIGN, REPORT, ... |
| TopUpStatus | PENDING, APPROVED, REJECTED |
| LineConfigStatus | PENDING, ACTIVE, INACTIVE |
| ConversationState | IDLE, SELECTING_SERVICE, SELECTING_STAFF, SELECTING_DATE, SELECTING_TIME, CONFIRMING_BOOKING |

## 結構

```java
@Getter
@RequiredArgsConstructor
public enum XxxStatus {
    ACTIVE("啟用"),
    INACTIVE("停用");

    private final String description;
}
```
