# 系統檢查報告

**檢查日期**: 2026-02-05
**檢查範圍**: 前後端欄位對應、營收統計、未實作功能

---

## 已修復問題

### 1. 前端欄位名稱與後端 DTO 不匹配

| 檔案 | 問題 | 修正 |
|------|------|------|
| `bookings.html` | 使用 `b.totalAmount` | 改為 `b.price` |
| `customers.html` | 使用 `c.totalBookings` | 改為 `c.visitCount` |
| `customer-detail.html` | 使用 `c.totalBookings` | 改為 `c.visitCount` |
| `reports.html` | 使用 `d.bookings` | 改為 `d.bookingCount` |
| `reports.html` | 使用 `s.serviceName` | 改為 `s.name` |
| `reports.html` | 使用 `s.bookingCount` | 改為 `s.count` |
| `reports.html` | 使用 `s.revenue` | 改為 `s.amount` |
| `reports.html` | 使用 `s.staffName` | 改為 `s.name` |

### 2. 營收統計功能未實作

**問題描述**: 報表頁面的營收數據都顯示為 $0

**修正內容**:
- `BookingRepository`: 新增 `sumRevenueByTenantIdAndStatusAndDateRange()` 方法
- `BookingRepository`: 新增 `sumRevenueByTenantIdAndDate()` 方法
- `ReportService`: 實作服務營收計算（只計算 COMPLETED 狀態的預約）
- `ReportService`: 實作每日營收計算
- `ReportService`: 實作本月營收計算
- `ReportService`: 實作平均客單價計算

**營收計算邏輯**:
- 只有「已完成」(COMPLETED) 狀態的預約才計入營收
- 待確認、已確認、已取消、爽約的預約不計入營收
- 這符合實際業務邏輯：顧客實際到店完成服務後才算營收

---

## 待處理問題（影響較小）

### 1. 預約編號欄位 (bookingNo)

**檔案**: `bookings.html`
**問題**: 前端使用 `b.bookingNo`，但 BookingResponse 沒有此欄位
**現況**: 前端有 fallback `b.id.substring(0, 8)`，不會導致顯示錯誤
**建議**: 可選擇新增 bookingNo 欄位或保持現狀使用 UUID 前 8 碼

### 2. 變化百分比欄位

**檔案**: `reports.html`
**問題**: 前端使用 `d.bookingsChange` 和 `d.revenueChange`
**現況**: ReportSummaryResponse 沒有這些欄位，但前端有判斷存在才顯示
**影響**: 變化百分比不會顯示（因為欄位不存在）
**建議**: 可後續實作同期比較功能

### 3. AdminDashboardService 硬編碼零值

**位置**: `AdminDashboardService.java` 第 128-129 行
**問題**:
- `pendingTopUpAmount` 始終為 0
- `monthlyApprovedAmount` 始終為 0

**影響**: 超管儀表板的儲值金額統計不正確
**建議**: 從 PointTopUp 表計算實際金額

### 4. 商品營收統計

**位置**: `ReportService.java`
**問題**: `productRevenue` 始終為 0
**現況**: 商品訂單系統已實作，但尚未整合到報表
**建議**: 從 ProductOrder 表（COMPLETED 狀態）計算商品營收

### 5. 票券折扣金額

**位置**: `ReportService.java`
**問題**: `couponDiscountAmount` 始終為 0
**建議**: 從 CouponInstance 的 usedAt 計算實際折扣金額

### 6. 回頭客統計

**位置**: `ReportService.java` 第 141 行
**問題**: `returningCustomers` 始終為 0
**建議**: 查詢在時間範圍內有多次預約的不重複顧客數

### 7. 行銷推播標籤篩選

**位置**: `MarketingService.java` 第 297 行
**問題**: TAG 類型的篩選返回空列表
**影響**: 無法使用標籤篩選目標客群
**建議**: 實作標籤篩選邏輯

---

## 測試建議

1. **預約金額顯示**: 建立預約，確認後台列表顯示正確的服務價格
2. **營收統計**: 完成預約後，確認報表中的營收數據正確更新
3. **每日報表**: 確認圖表中的每日營收數據正確
4. **本月營收**: 確認儀表板中的本月營收正確
5. **顧客預約次數**: 確認顧客列表和詳情頁的預約次數正確

---

## 提交記錄

| Commit | 說明 |
|--------|------|
| `c2260ef` | feat: 新增庫存異動歷史和商品訂單管理功能 |
| `1263e83` | fix: 修復前端欄位名稱與後端 DTO 不匹配及營收統計問題 |
