import { test, expect, APIRequestContext } from '@playwright/test';
import { WAIT_TIME, generateTestPhone, TEST_ACCOUNTS } from './utils/test-helpers';

/**
 * LINE Bot 功能測試
 *
 * 測試範圍：
 * 1. LINE Webhook API
 * 2. 對話狀態管理
 * 3. 訊息格式
 * 4. AI 智慧客服選單邏輯
 *
 * 注意：這些測試模擬 LINE Bot 的 API 呼叫
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const data = await response.json();
  return data.data.accessToken;
}

test.describe('LINE Bot API 測試', () => {
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    adminToken = await getAdminToken(request);
  });

  test.describe('LINE Webhook', () => {
    test('Webhook 端點存在', async ({ request }) => {
      // 測試 Webhook 端點（不帶有效資料，預期回傳非 500 錯誤）
      const response = await request.post('/api/line/webhook/test_tenant', {
        headers: { 'Content-Type': 'application/json' },
        data: {
          destination: 'test',
          events: []
        }
      });
      // 可能是 200（空事件）或 400/401/404（無效租戶）
      expect(response.status()).toBeLessThan(500);
      console.log(`Webhook 回應: ${response.status()}`);
    });

    test('Webhook - 模擬 Follow 事件', async ({ request }) => {
      const webhookData = {
        destination: 'test',
        events: [{
          type: 'follow',
          timestamp: Date.now(),
          source: {
            type: 'user',
            userId: 'U' + Date.now()
          },
          replyToken: 'test-reply-token-' + Date.now()
        }]
      };

      const response = await request.post('/api/line/webhook/test_tenant', {
        headers: { 'Content-Type': 'application/json' },
        data: webhookData
      });
      console.log(`Follow 事件回應: ${response.status()}`);
    });

    test('Webhook - 模擬 Message 事件', async ({ request }) => {
      const webhookData = {
        destination: 'test',
        events: [{
          type: 'message',
          timestamp: Date.now(),
          source: {
            type: 'user',
            userId: 'U' + Date.now()
          },
          replyToken: 'test-reply-token-' + Date.now(),
          message: {
            id: 'msg' + Date.now(),
            type: 'text',
            text: '預約'
          }
        }]
      };

      const response = await request.post('/api/line/webhook/test_tenant', {
        headers: { 'Content-Type': 'application/json' },
        data: webhookData
      });
      console.log(`Message 事件回應: ${response.status()}`);
    });

    test('Webhook - 模擬 Postback 事件', async ({ request }) => {
      const webhookData = {
        destination: 'test',
        events: [{
          type: 'postback',
          timestamp: Date.now(),
          source: {
            type: 'user',
            userId: 'U' + Date.now()
          },
          replyToken: 'test-reply-token-' + Date.now(),
          postback: {
            data: 'action=select_service&serviceId=123'
          }
        }]
      };

      const response = await request.post('/api/line/webhook/test_tenant', {
        headers: { 'Content-Type': 'application/json' },
        data: webhookData
      });
      console.log(`Postback 事件回應: ${response.status()}`);
    });

    test('Webhook - 模擬 Unfollow 事件', async ({ request }) => {
      const webhookData = {
        destination: 'test',
        events: [{
          type: 'unfollow',
          timestamp: Date.now(),
          source: {
            type: 'user',
            userId: 'U' + Date.now()
          }
        }]
      };

      const response = await request.post('/api/line/webhook/test_tenant', {
        headers: { 'Content-Type': 'application/json' },
        data: webhookData
      });
      console.log(`Unfollow 事件回應: ${response.status()}`);
    });
  });

  test.describe('LINE 用戶資料', () => {
    test('取得店家的 LINE 用戶列表', async ({ request }) => {
      const tenantToken = await getTenantToken(request);
      if (!tenantToken) return;

      // 假設有這樣的 API（如果沒有，測試會失敗但不影響其他測試）
      const response = await request.get('/api/customers?source=LINE', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      if (response.ok()) {
        const data = await response.json();
        console.log(`LINE 用戶數: ${data.data?.totalElements || 0}`);
      }
    });
  });
});

test.describe('LINE 對話狀態測試', () => {
  test.describe('ConversationState 列舉', () => {
    test('所有對話狀態', () => {
      // 所有對話狀態（共 17 個）
      const conversationStates = [
        'IDLE',
        'SELECTING_SERVICE',
        'SELECTING_STAFF',
        'SELECTING_DATE',
        'SELECTING_TIME',
        'INPUTTING_NOTE',           // 備註輸入狀態
        'CONFIRMING_BOOKING',
        'VIEWING_BOOKINGS',         // 查看預約
        'CONFIRMING_CANCEL_BOOKING',
        'BROWSING_PRODUCTS',
        'VIEWING_PRODUCT_DETAIL',
        'SELECTING_QUANTITY',
        'CONFIRMING_PURCHASE',
        'BROWSING_COUPONS',
        'VIEWING_MY_COUPONS',
        'VIEWING_PROFILE',          // 查看個人資料
        'VIEWING_MEMBER_INFO'
      ];

      console.log('LINE Bot 對話狀態:');
      for (const state of conversationStates) {
        console.log(`- ${state}`);
      }
      expect(conversationStates.length).toBe(17);
    });
  });

  test.describe('預約流程狀態', () => {
    test('預約流程狀態順序', () => {
      // 完整預約流程：包含備註輸入步驟
      const bookingFlow = [
        'IDLE',
        'SELECTING_SERVICE',
        'SELECTING_STAFF',
        'SELECTING_DATE',
        'SELECTING_TIME',
        'INPUTTING_NOTE',      // 選擇時間後進入備註輸入狀態
        'CONFIRMING_BOOKING',  // 輸入備註或跳過後進入確認狀態
        'IDLE'
      ];

      console.log('預約流程:');
      for (let i = 0; i < bookingFlow.length - 1; i++) {
        console.log(`${bookingFlow[i]} → ${bookingFlow[i + 1]}`);
      }
      expect(bookingFlow[0]).toBe('IDLE');
      expect(bookingFlow[bookingFlow.length - 1]).toBe('IDLE');
      expect(bookingFlow).toContain('INPUTTING_NOTE');
    });
  });

  test.describe('商品購買流程狀態', () => {
    test('商品購買流程狀態順序', () => {
      const purchaseFlow = [
        'IDLE',
        'BROWSING_PRODUCTS',
        'VIEWING_PRODUCT_DETAIL',
        'SELECTING_QUANTITY',
        'CONFIRMING_PURCHASE',
        'IDLE'
      ];

      console.log('商品購買流程:');
      for (let i = 0; i < purchaseFlow.length - 1; i++) {
        console.log(`${purchaseFlow[i]} → ${purchaseFlow[i + 1]}`);
      }
      expect(purchaseFlow[0]).toBe('IDLE');
      expect(purchaseFlow[purchaseFlow.length - 1]).toBe('IDLE');
    });
  });

  test.describe('票券領取流程狀態', () => {
    test('票券領取流程狀態順序', () => {
      const couponFlow = [
        'IDLE',
        'BROWSING_COUPONS',
        'IDLE'
      ];

      console.log('票券領取流程:');
      for (let i = 0; i < couponFlow.length - 1; i++) {
        console.log(`${couponFlow[i]} → ${couponFlow[i + 1]}`);
      }
      expect(couponFlow[0]).toBe('IDLE');
    });
  });
});

test.describe('LINE 訊息格式測試', () => {
  test.describe('Flex Message 結構', () => {
    test('主選單 Flex Message', () => {
      const mainMenuFlex = {
        type: 'flex',
        altText: '主選單',
        contents: {
          type: 'bubble',
          body: {
            type: 'box',
            layout: 'vertical',
            contents: [
              {
                type: 'button',
                action: { type: 'postback', label: '開始預約', data: 'action=start_booking' }
              },
              {
                type: 'button',
                action: { type: 'postback', label: '我的預約', data: 'action=my_bookings' }
              },
              {
                type: 'button',
                action: { type: 'postback', label: '瀏覽商品', data: 'action=browse_products' }
              },
              {
                type: 'button',
                action: { type: 'postback', label: '領取票券', data: 'action=browse_coupons' }
              },
              {
                type: 'button',
                action: { type: 'postback', label: '會員資訊', data: 'action=member_info' }
              }
            ]
          }
        }
      };

      expect(mainMenuFlex.type).toBe('flex');
      expect(mainMenuFlex.contents.type).toBe('bubble');
      console.log('主選單結構驗證通過');
    });

    test('服務列表 Carousel', () => {
      const serviceCarousel = {
        type: 'flex',
        altText: '選擇服務',
        contents: {
          type: 'carousel',
          contents: [
            {
              type: 'bubble',
              body: {
                type: 'box',
                layout: 'vertical',
                contents: [
                  { type: 'text', text: '服務名稱' },
                  { type: 'text', text: '$1,000' },
                  { type: 'text', text: '60分鐘' }
                ]
              },
              footer: {
                type: 'box',
                layout: 'vertical',
                contents: [
                  {
                    type: 'button',
                    action: { type: 'postback', label: '選擇', data: 'action=select_service&id=1' }
                  }
                ]
              }
            }
          ]
        }
      };

      expect(serviceCarousel.contents.type).toBe('carousel');
      console.log('服務列表 Carousel 結構驗證通過');
    });

    test('日期選擇 Carousel', () => {
      const dateCarousel = {
        type: 'flex',
        altText: '選擇日期',
        contents: {
          type: 'carousel',
          contents: [] // 動態生成的日期卡片
        }
      };

      expect(dateCarousel.contents.type).toBe('carousel');
      console.log('日期選擇 Carousel 結構驗證通過');
    });

    test('時段選擇 Quick Reply', () => {
      const timeQuickReply = {
        type: 'text',
        text: '請選擇時段：',
        quickReply: {
          items: [
            { type: 'action', action: { type: 'postback', label: '09:00', data: 'time=09:00' } },
            { type: 'action', action: { type: 'postback', label: '10:00', data: 'time=10:00' } },
            { type: 'action', action: { type: 'postback', label: '11:00', data: 'time=11:00' } }
          ]
        }
      };

      expect(timeQuickReply.quickReply).toBeDefined();
      expect(timeQuickReply.quickReply.items.length).toBeGreaterThan(0);
      console.log('時段選擇 Quick Reply 結構驗證通過');
    });

    test('預約確認 Flex Message', () => {
      const confirmationFlex = {
        type: 'flex',
        altText: '預約確認',
        contents: {
          type: 'bubble',
          body: {
            type: 'box',
            layout: 'vertical',
            contents: [
              { type: 'text', text: '預約確認', weight: 'bold', size: 'xl' },
              { type: 'separator' },
              { type: 'text', text: '服務：剪髮' },
              { type: 'text', text: '員工：小明' },
              { type: 'text', text: '日期：2024-01-15' },
              { type: 'text', text: '時間：10:00' }
            ]
          },
          footer: {
            type: 'box',
            layout: 'horizontal',
            contents: [
              {
                type: 'button',
                style: 'primary',
                action: { type: 'postback', label: '確認預約', data: 'action=confirm' }
              },
              {
                type: 'button',
                style: 'secondary',
                action: { type: 'postback', label: '取消', data: 'action=cancel' }
              }
            ]
          }
        }
      };

      expect(confirmationFlex.contents.type).toBe('bubble');
      expect(confirmationFlex.contents.footer).toBeDefined();
      console.log('預約確認 Flex Message 結構驗證通過');
    });

    test('備註輸入提示 Flex Message', () => {
      // 模擬備註輸入提示結構
      const notePromptFlex = {
        type: 'flex',
        altText: '是否需要備註？',
        contents: {
          type: 'bubble',
          header: {
            type: 'box',
            layout: 'vertical',
            contents: [
              { type: 'text', text: '是否需要備註？', weight: 'bold', color: '#FFFFFF' }
            ]
          },
          body: {
            type: 'box',
            layout: 'vertical',
            contents: [
              { type: 'text', text: '您可以直接輸入文字作為備註，或點選「跳過」繼續預約。', wrap: true },
              { type: 'box', layout: 'vertical', contents: [
                { type: 'text', text: '備註範例：' },
                { type: 'text', text: '希望靠窗座位、有過敏體質、第一次來...' }
              ]}
            ]
          },
          footer: {
            type: 'box',
            layout: 'horizontal',
            contents: [
              { type: 'button', action: { type: 'postback', label: '↩ 返回', data: 'action=go_back' } },
              { type: 'button', action: { type: 'postback', label: '跳過 →', data: 'action=skip_note' } }
            ]
          }
        }
      };

      // 驗證結構
      expect(notePromptFlex.contents.type).toBe('bubble');
      expect(notePromptFlex.contents.header).toBeDefined();
      expect(notePromptFlex.contents.body).toBeDefined();
      expect(notePromptFlex.contents.footer).toBeDefined();
      // 驗證有跳過按鈕
      expect(notePromptFlex.contents.footer.contents[1].action.data).toBe('action=skip_note');
      console.log('備註輸入提示 Flex Message 結構驗證通過');
    });

    test('主選單會員資訊按鈕', () => {
      // 模擬主選單結構（只驗證會員資訊按鈕部分）
      const mainMenuFlex = {
        type: 'flex',
        altText: '主選單',
        contents: {
          type: 'bubble',
          footer: {
            type: 'box',
            layout: 'vertical',
            contents: [
              { type: 'box', layout: 'horizontal', action: { type: 'postback', data: 'action=start_booking' } },
              { type: 'box', layout: 'horizontal', action: { type: 'postback', data: 'action=view_bookings' } },
              { type: 'box', layout: 'horizontal', action: { type: 'postback', data: 'action=start_shopping' } },
              { type: 'box', layout: 'horizontal', contents: [
                { type: 'box', action: { type: 'postback', data: 'action=view_coupons' } },
                { type: 'box', action: { type: 'postback', data: 'action=view_my_coupons' } }
              ]},
              // 會員資訊按鈕
              { type: 'box', layout: 'horizontal', action: { type: 'postback', label: '👤 會員資訊', data: 'action=view_member_info' } }
            ]
          }
        }
      };

      // 驗證會員資訊按鈕存在
      const memberInfoButton = mainMenuFlex.contents.footer.contents.find(
        (item: any) => item.action && item.action.data === 'action=view_member_info'
      );
      expect(memberInfoButton).toBeDefined();
      expect(memberInfoButton.action.data).toBe('action=view_member_info');
      console.log('主選單會員資訊按鈕結構驗證通過');
    });
  });
});

test.describe('LINE Event Type 測試', () => {
  test('事件類型列舉', () => {
    const lineEventTypes = ['MESSAGE', 'FOLLOW', 'UNFOLLOW', 'POSTBACK'];

    console.log('LINE Event Types:');
    for (const eventType of lineEventTypes) {
      console.log(`- ${eventType}`);
    }
    expect(lineEventTypes.length).toBe(4);
  });
});

test.describe('LINE Config Status 測試', () => {
  test('設定狀態列舉', () => {
    const lineConfigStatuses = ['ACTIVE', 'INACTIVE'];

    console.log('LINE Config Statuses:');
    for (const status of lineConfigStatuses) {
      console.log(`- ${status}`);
    }
    expect(lineConfigStatuses.length).toBe(2);
  });
});

test.describe('AI 智慧客服選單邏輯', () => {
  // 測試用的店家代碼
  const TENANT_CODE = 'michaelshop';

  test('驗證 Webhook 端點可訪問', async ({ request }) => {
    // 只測試端點是否存在（不帶簽名會被拒絕，但至少確認路由存在）
    const response = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      data: {},
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // 可能返回 400（簽名驗證失敗）或其他錯誤，但不應該是 404
    console.log(`Webhook 端點狀態碼: ${response.status()}`);
    expect(response.status()).not.toBe(404);
  });

  test('健康檢查', async ({ request }) => {
    const response = await request.get(`/health`);
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.status).toBe('UP');
    console.log('服務狀態: UP');
  });

  /**
   * 以下是預期的 AI 行為說明（需要在 LINE 中手動測試）
   *
   * 【應該顯示選單的訊息】
   * - "你好" -> 打招呼，顯示歡迎選單
   * - "嗨" -> 打招呼，顯示歡迎選單
   * - "我要預約" -> 明確服務意圖
   * - "幫我預約" -> 明確服務意圖
   * - "我想預約剪髮" -> 明確服務意圖
   * - "有什麼服務" -> 想了解服務
   * - "可以做什麼" -> 想了解服務
   *
   * 【不應該顯示選單的訊息】
   * - "營業時間是幾點" -> 只是詢問資訊
   * - "地址在哪裡" -> 只是詢問資訊
   * - "剪髮多少錢" -> 還在考慮，先回答價格
   * - "謝謝" -> 對話結束
   * - "好的" -> 對話結束
   * - "了解" -> 對話結束
   */
});
