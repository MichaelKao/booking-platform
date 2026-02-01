#!/bin/bash
# 全面 API 測試腳本
# 使用方式: bash test-all-apis.sh

BASE_URL="https://booking-platform-production-1e08.up.railway.app"
EMAIL="g0909095118@gmail.com"
PASSWORD="gaojunting11"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

# 測試函數
test_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${BASE_URL}${endpoint}" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${BASE_URL}${endpoint}" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    # 檢查是否成功
    if echo "$body" | grep -q '"success":true' || [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}[PASS]${NC} $description"
        ((PASS++))
        return 0
    else
        echo -e "${RED}[FAIL]${NC} $description"
        echo "       HTTP: $http_code"
        echo "       Response: $(echo $body | head -c 200)"
        ((FAIL++))
        return 1
    fi
}

echo "======================================"
echo "  預約平台 API 全面測試"
echo "======================================"
echo ""

# 1. 登入取得 Token
echo -e "${YELLOW}[登入]${NC}"
login_response=$(curl -s -X POST "${BASE_URL}/api/auth/tenant/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")

if echo "$login_response" | grep -q '"success":true'; then
    TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}[PASS]${NC} 登入成功"
    ((PASS++))
else
    echo -e "${RED}[FAIL]${NC} 登入失敗"
    echo "$login_response"
    exit 1
fi

echo ""
echo -e "${YELLOW}[店家設定 API]${NC}"
test_api "GET" "/api/settings" "" "取得設定"
test_api "PUT" "/api/settings" '{"name":"測試店家"}' "更新基本設定"
test_api "PUT" "/api/settings" '{"businessStartTime":"09:00","businessEndTime":"21:00","bookingInterval":30,"maxAdvanceBookingDays":30}' "更新營業設定"

echo ""
echo -e "${YELLOW}[服務項目 API]${NC}"
test_api "GET" "/api/services" "" "取得服務列表"
test_api "GET" "/api/service-categories" "" "取得服務分類"
test_api "GET" "/api/services/bookable" "" "取得可預約服務"

echo ""
echo -e "${YELLOW}[員工 API]${NC}"
test_api "GET" "/api/staff" "" "取得員工列表"
test_api "GET" "/api/staff/bookable" "" "取得可預約員工"

echo ""
echo -e "${YELLOW}[顧客 API]${NC}"
test_api "GET" "/api/customers" "" "取得顧客列表"

echo ""
echo -e "${YELLOW}[預約 API]${NC}"
test_api "GET" "/api/bookings" "" "取得預約列表"
test_api "GET" "/api/bookings/calendar?start=$(date +%Y-%m-01)&end=$(date +%Y-%m-28)" "" "取得行事曆預約"

echo ""
echo -e "${YELLOW}[商品 API]${NC}"
test_api "GET" "/api/products" "" "取得商品列表"

echo ""
echo -e "${YELLOW}[票券 API]${NC}"
test_api "GET" "/api/coupons" "" "取得票券列表"

echo ""
echo -e "${YELLOW}[行銷活動 API]${NC}"
test_api "GET" "/api/campaigns" "" "取得活動列表"

echo ""
echo -e "${YELLOW}[會員等級 API]${NC}"
test_api "GET" "/api/membership-levels" "" "取得會員等級"

echo ""
echo -e "${YELLOW}[報表 API]${NC}"
test_api "GET" "/api/reports/dashboard" "" "取得儀表板"
test_api "GET" "/api/reports/summary" "" "取得報表摘要"
test_api "GET" "/api/reports/today" "" "取得今日報表"

echo ""
echo -e "${YELLOW}[點數 API]${NC}"
test_api "GET" "/api/points/balance" "" "取得點數餘額"
test_api "GET" "/api/points/transactions" "" "取得交易記錄"

echo ""
echo -e "${YELLOW}[功能商店 API]${NC}"
test_api "GET" "/api/feature-store" "" "取得功能商店"

echo ""
echo -e "${YELLOW}[LINE 設定 API]${NC}"
test_api "GET" "/api/settings/line" "" "取得 LINE 設定"

echo ""
echo "======================================"
echo -e "  測試結果: ${GREEN}通過 $PASS${NC} / ${RED}失敗 $FAIL${NC}"
echo "======================================"

if [ $FAIL -gt 0 ]; then
    exit 1
fi
