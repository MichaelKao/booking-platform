# Booking Platform - 多租戶預約平台系統

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Railway](https://img.shields.io/badge/Deploy-Railway-purple.svg)](https://railway.app/)

一個 SaaS 多租戶預約平台，讓各種服務業（美髮、美甲、刺青、SPA、寵物美容、健身教練等）可以透過 LINE Bot 提供預約服務。

## 線上演示

- **店家後台**: https://booking-platform-production-1e08.up.railway.app/tenant/login
- **超管後台**: https://booking-platform-production-1e08.up.railway.app/admin/login

---

## 功能特色

### 三個角色

| 角色 | 說明 |
|------|------|
| **超級管理員** | 管理所有店家帳號、控制功能開關、審核儲值申請、查看全平台數據 |
| **店家（租戶）** | 管理預約、顧客、員工、服務、商品，使用平台功能 |
| **顧客** | 透過 LINE Bot 預約服務、查看預約記錄、接收通知 |

### 核心功能

- **預約管理**: 預約建立、確認、取消、完成、行事曆檢視
- **員工管理**: 班表設定、請假管理、服務項目指派
- **顧客管理**: 會員等級、點數系統、封鎖/解封
- **服務管理**: 分類、項目、時長、定價
- **商品管理**: 庫存追蹤、上下架、商品訂單、庫存異動
- **行銷功能**: 票券系統、行銷活動、行銷推播
- **報表分析**: 營收統計、預約趨勢、Excel/PDF 匯出
- **LINE Bot 整合**: 對話式預約、通知推播、Rich Menu
- **金流整合**: ECPay 綠界（信用卡、ATM、超商）
- **AI 智慧客服**: Groq Llama 3.3 自動回答
- **即時通知**: SSE 即時推播（新預約、新訂單等）
- **SMS 通知**: 三竹簡訊（預約提醒、行銷推播）
- **郵件通知**: Resend API（密碼重設、歡迎郵件）
- **功能商店**: 付費功能訂閱制
- **SEO 頁面**: 首頁、功能介紹、價格方案、行業頁面
- **多語系**: 繁體中文、簡體中文、英文
- **RWD**: 手機、平板、電腦自適應

---

## 技術棧

| 類別 | 技術 |
|------|------|
| 後端 | Java 17, Spring Boot 3.2, Maven |
| 資料庫 | PostgreSQL 15 |
| 快取 | Redis |
| 認證 | Spring Security + JWT |
| 前端 | Thymeleaf, Bootstrap 5, JavaScript |
| 行事曆 | FullCalendar |
| 圖表 | Chart.js |
| 報表匯出 | Apache POI (Excel) + OpenPDF (PDF) |
| LINE 整合 | LINE Messaging API |
| 金流 | ECPay 綠界 |
| 郵件 | Resend (HTTP API) |
| SMS | 三竹簡訊 |
| AI | Groq + Llama 3.3 |
| 部署 | Railway (Docker) |
| E2E 測試 | Playwright |

---

## 專案統計

| 項目 | 數量 |
|------|------|
| Java 類別 | 265 |
| Controller | 33 |
| Service | 38 |
| Entity | 25 |
| Repository | 25 |
| DTO | 76+ |
| Enum | 28 |
| Scheduler | 5 |
| HTML 頁面 | 51 |
| CSS 檔案 | 3 |
| JS 檔案 | 4 |
| E2E 測試 | 886 |

---

## 專案結構

```
booking-platform/
├── src/main/java/com/booking/platform/
│   ├── common/           # 共用元件 (設定、例外、安全)
│   ├── controller/       # API 控制器 (33 個)
│   ├── service/          # 業務邏輯 (38 個)
│   ├── repository/       # 資料存取 (25 個)
│   ├── entity/           # 資料庫實體 (25 個)
│   ├── dto/              # 資料傳輸物件 (76+)
│   ├── enums/            # 列舉型別 (28 個)
│   ├── scheduler/        # 排程任務 (5 個)
│   └── mapper/           # 物件轉換
├── src/main/resources/
│   ├── templates/        # Thymeleaf 模板 (51 頁面)
│   ├── static/           # CSS/JS 靜態資源
│   └── application*.yml  # 設定檔
├── Dockerfile            # Docker 建構檔
├── railway.json          # Railway 部署設定
└── pom.xml               # Maven 設定
```

---

## 本地開發

### 環境需求

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis (可選)

### 步驟

1. **複製專案**
   ```bash
   git clone https://github.com/your-repo/booking-platform.git
   cd booking-platform
   ```

2. **設定資料庫**
   ```bash
   # 建立 PostgreSQL 資料庫
   createdb booking
   ```

3. **設定環境變數**
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/booking
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=your_password
   export JWT_SECRET=your-jwt-secret-key-must-be-at-least-256-bits
   ```

4. **啟動應用**
   ```bash
   mvn spring-boot:run
   ```

5. **訪問應用**
   - 店家後台: http://localhost:8080/tenant/login
   - 超管後台: http://localhost:8080/admin/login

---

## Railway 部署

### 步驟

1. **建立 Railway 專案**
   - 前往 [Railway](https://railway.app/)
   - 建立新專案，選擇 GitHub 連結

2. **新增 PostgreSQL 服務**
   - 點擊「Add Service」→「Database」→「PostgreSQL」

3. **設定環境變數**
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
   SPRING_DATASOURCE_USERNAME=${PGUSER}
   SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
   SPRING_PROFILES_ACTIVE=prod
   JWT_SECRET=your-secure-jwt-secret-key-256-bits-minimum
   ```

4. **部署**
   - Railway 會自動偵測 Dockerfile 並建構部署

### 健康檢查

應用提供 `/health` 端點供 Railway 健康檢查：

```json
{
  "status": "UP"
}
```

---

## 環境變數

| 變數 | 說明 | 必填 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | PostgreSQL 連線 URL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | 資料庫使用者 | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | 資料庫密碼 | ✅ |
| `SPRING_PROFILES_ACTIVE` | Spring Profile (dev/prod) | ✅ |
| `JWT_SECRET` | JWT 簽名密鑰 (至少 256 bits) | ✅ |
| `REDIS_URL` | Redis 連線 URL | ❌ |
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token | ❌ |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret | ❌ |
| `RESEND_API_KEY` | Resend 郵件 API Key | ❌ |
| `MAIL_FROM` | 郵件寄件人地址 | ❌ |
| `ENCRYPTION_SECRET_KEY` | 加密密鑰 (Base64) | ❌ |
| `ECPAY_MERCHANT_ID` | ECPay 商店代號 | ❌ |
| `ECPAY_HASH_KEY` | ECPay HashKey | ❌ |
| `ECPAY_HASH_IV` | ECPay HashIV | ❌ |
| `SMS_ENABLED` | 啟用 SMS | ❌ |
| `SMS_USERNAME` | SMS 帳號 | ❌ |
| `SMS_PASSWORD` | SMS 密碼 | ❌ |
| `GROQ_ENABLED` | 啟用 AI 客服 | ❌ |
| `GROQ_API_KEY` | Groq API Key | ❌ |

---

## API 文件

### 認證 API

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/auth/admin/login` | 超管登入 |
| POST | `/api/auth/tenant/login` | 店家登入 |
| POST | `/api/auth/tenant/register` | 店家註冊 |
| POST | `/api/auth/refresh` | 刷新 Token |
| POST | `/api/auth/logout` | 登出 |

### 業務 API

| 模組 | 路徑 | 說明 |
|------|------|------|
| 預約 | `/api/bookings` | 預約管理 |
| 顧客 | `/api/customers` | 顧客管理 |
| 員工 | `/api/staff` | 員工管理 |
| 服務 | `/api/services` | 服務項目 |
| 商品 | `/api/products` | 商品管理 |
| 商品訂單 | `/api/product-orders` | 商品訂單管理 |
| 庫存 | `/api/inventory` | 庫存異動 |
| 票券 | `/api/coupons` | 票券管理 |
| 活動 | `/api/campaigns` | 行銷活動 |
| 推播 | `/api/marketing` | 行銷推播 |
| 會員等級 | `/api/membership-levels` | 會員等級 |
| 報表 | `/api/reports` | 報表統計 |
| 匯出 | `/api/export` | Excel/PDF 匯出 |
| 設定 | `/api/settings` | 店家設定 |
| 點數 | `/api/points` | 點數管理 |
| 功能商店 | `/api/feature-store` | 功能訂閱 |
| 金流 | `/api/payments` | ECPay 支付 |
| 通知 | `/api/notifications` | SSE 即時通知 |

### 超管 API

| 路徑 | 說明 |
|------|------|
| `/api/admin/tenants` | 租戶管理 |
| `/api/admin/features` | 功能管理 |
| `/api/admin/point-topups` | 儲值審核 |
| `/api/admin/dashboard` | 儀表板 |

詳細 API 文件請參考 `src/main/java/com/booking/platform/controller/CLAUDE.md`

---

## LINE Bot 整合

### Webhook URL

```
POST https://{domain}/api/line/webhook/{tenantCode}
```

### 對話流程

```
IDLE → SELECTING_SERVICE → SELECTING_DATE → SELECTING_STAFF → SELECTING_TIME → INPUTTING_NOTE → CONFIRMING_BOOKING → IDLE
```

### 設定步驟

1. 在 LINE Developers Console 建立 Messaging API Channel
2. 在店家後台設定 Channel Secret 和 Channel Access Token
3. 設定 Webhook URL 為 `https://your-domain/api/line/webhook/{tenantCode}`

---

## 開發規範

- 使用繁體中文撰寫註解
- 所有業務資料都需包含 `tenant_id`
- 使用軟刪除（設定 `deleted_at`）
- 重要操作需寫入稽核日誌
- 敏感資料需加密儲存

詳細規範請參考 `CLAUDE.md`

---

## 授權

此專案為私有專案。

---

## 聯絡方式

如有問題請聯繫專案負責人。
