# LINE Flex Message 模板

建立 LINE Flex Message 時的常用模板。

---

## 1. 主選單（Rich Menu 替代方案）
```json
{
  "type": "bubble",
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "歡迎光臨 {shopName}",
        "weight": "bold",
        "size": "lg",
        "align": "center"
      },
      {
        "type": "text",
        "text": "請選擇您要的服務",
        "size": "sm",
        "color": "#666666",
        "align": "center",
        "margin": "md"
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "vertical",
    "spacing": "sm",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "📅 我要預約",
          "data": "action=start_booking"
        },
        "style": "primary"
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "📋 我的預約",
          "data": "action=my_bookings"
        },
        "style": "secondary"
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "🎫 我的票券",
          "data": "action=my_coupons"
        },
        "style": "secondary"
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "📞 聯絡店家",
          "data": "action=contact_shop"
        },
        "style": "secondary"
      }
    ]
  }
}
```

---

## 2. 服務選擇輪播
```json
{
  "type": "carousel",
  "contents": [
    {
      "type": "bubble",
      "size": "micro",
      "body": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "text",
            "text": "{serviceName}",
            "weight": "bold",
            "size": "md",
            "wrap": true
          },
          {
            "type": "text",
            "text": "NT$ {price}",
            "size": "lg",
            "weight": "bold",
            "color": "#1DB446",
            "margin": "md"
          },
          {
            "type": "text",
            "text": "約 {duration} 分鐘",
            "size": "xs",
            "color": "#999999"
          }
        ]
      },
      "footer": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "button",
            "action": {
              "type": "postback",
              "label": "選擇",
              "data": "action=select_service&serviceId={serviceId}"
            },
            "style": "primary",
            "height": "sm"
          }
        ]
      }
    }
  ]
}
```

---

## 3. 員工選擇輪播
```json
{
  "type": "carousel",
  "contents": [
    {
      "type": "bubble",
      "size": "micro",
      "hero": {
        "type": "image",
        "url": "{staffAvatarUrl}",
        "size": "full",
        "aspectRatio": "1:1",
        "aspectMode": "cover"
      },
      "body": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "text",
            "text": "{staffName}",
            "weight": "bold",
            "size": "md",
            "align": "center"
          },
          {
            "type": "text",
            "text": "{staffTitle}",
            "size": "xs",
            "color": "#999999",
            "align": "center"
          }
        ]
      },
      "footer": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "button",
            "action": {
              "type": "postback",
              "label": "選擇",
              "data": "action=select_staff&staffId={staffId}"
            },
            "style": "primary",
            "height": "sm"
          }
        ]
      }
    },
    {
      "type": "bubble",
      "size": "micro",
      "body": {
        "type": "box",
        "layout": "vertical",
        "justifyContent": "center",
        "alignItems": "center",
        "contents": [
          {
            "type": "text",
            "text": "不指定",
            "weight": "bold",
            "size": "md"
          },
          {
            "type": "text",
            "text": "由店家安排",
            "size": "xs",
            "color": "#999999",
            "margin": "sm"
          }
        ]
      },
      "footer": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "button",
            "action": {
              "type": "postback",
              "label": "不指定",
              "data": "action=select_staff&staffId=none"
            },
            "style": "secondary",
            "height": "sm"
          }
        ]
      }
    }
  ]
}
```

---

## 4. 日期選擇（使用 Datetime Picker）
```json
{
  "type": "bubble",
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "請選擇預約日期",
        "weight": "bold",
        "size": "lg"
      },
      {
        "type": "text",
        "text": "可預約日期：{startDate} ~ {endDate}",
        "size": "sm",
        "color": "#666666",
        "margin": "md"
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "datetimepicker",
          "label": "選擇日期",
          "data": "action=select_date",
          "mode": "date",
          "initial": "{today}",
          "min": "{minDate}",
          "max": "{maxDate}"
        },
        "style": "primary"
      }
    ]
  }
}
```

---

## 5. 時段選擇
```json
{
  "type": "bubble",
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "{date} 可預約時段",
        "weight": "bold",
        "size": "lg"
      },
      {
        "type": "separator",
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "vertical",
        "margin": "md",
        "spacing": "sm",
        "contents": [
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {
                "type": "button",
                "action": {
                  "type": "postback",
                  "label": "10:00",
                  "data": "action=select_time&time=10:00"
                },
                "style": "secondary",
                "height": "sm",
                "flex": 1
              },
              {
                "type": "button",
                "action": {
                  "type": "postback",
                  "label": "10:30",
                  "data": "action=select_time&time=10:30"
                },
                "style": "secondary",
                "height": "sm",
                "flex": 1
              },
              {
                "type": "button",
                "action": {
                  "type": "postback",
                  "label": "11:00",
                  "data": "action=select_time&time=11:00"
                },
                "style": "secondary",
                "height": "sm",
                "flex": 1
              }
            ],
            "spacing": "sm"
          }
        ]
      }
    ]
  }
}
```

---

## 6. 預約確認
```json
{
  "type": "bubble",
  "header": {
    "type": "box",
    "layout": "vertical",
    "backgroundColor": "#1DB446",
    "paddingAll": "md",
    "contents": [
      {
        "type": "text",
        "text": "請確認預約資訊",
        "color": "#FFFFFF",
        "weight": "bold",
        "size": "lg"
      }
    ]
  },
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "box",
        "layout": "horizontal",
        "contents": [
          {"type": "text", "text": "服務", "color": "#666666", "flex": 2},
          {"type": "text", "text": "{serviceName}", "flex": 3, "weight": "bold"}
        ],
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "horizontal",
        "contents": [
          {"type": "text", "text": "日期", "color": "#666666", "flex": 2},
          {"type": "text", "text": "{date}", "flex": 3, "weight": "bold"}
        ],
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "horizontal",
        "contents": [
          {"type": "text", "text": "時間", "color": "#666666", "flex": 2},
          {"type": "text", "text": "{time}", "flex": 3, "weight": "bold"}
        ],
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "horizontal",
        "contents": [
          {"type": "text", "text": "服務人員", "color": "#666666", "flex": 2},
          {"type": "text", "text": "{staffName}", "flex": 3, "weight": "bold"}
        ],
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "horizontal",
        "contents": [
          {"type": "text", "text": "預估金額", "color": "#666666", "flex": 2},
          {"type": "text", "text": "NT$ {price}", "flex": 3, "weight": "bold", "color": "#1DB446"}
        ],
        "margin": "md"
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "horizontal",
    "spacing": "sm",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "取消",
          "data": "action=cancel_booking_flow"
        },
        "style": "secondary",
        "flex": 1
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "確認預約",
          "data": "action=confirm_booking"
        },
        "style": "primary",
        "flex": 2
      }
    ]
  }
}
```

---

## 7. 預約成功通知
```json
{
  "type": "bubble",
  "header": {
    "type": "box",
    "layout": "vertical",
    "backgroundColor": "#1DB446",
    "paddingAll": "md",
    "contents": [
      {
        "type": "text",
        "text": "✅ 預約成功",
        "color": "#FFFFFF",
        "weight": "bold",
        "size": "xl",
        "align": "center"
      }
    ]
  },
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "預約編號：{bookingNo}",
        "size": "sm",
        "color": "#999999"
      },
      {
        "type": "separator",
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "vertical",
        "margin": "md",
        "spacing": "sm",
        "contents": [
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "服務", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{serviceName}", "flex": 3}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "日期", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{date}", "flex": 3}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "時間", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{time}", "flex": 3}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "服務人員", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{staffName}", "flex": 3}
            ]
          }
        ]
      },
      {
        "type": "separator",
        "margin": "md"
      },
      {
        "type": "text",
        "text": "📍 {shopAddress}",
        "size": "sm",
        "color": "#666666",
        "margin": "md",
        "wrap": true
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "vertical",
    "spacing": "sm",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "查看預約詳情",
          "data": "action=view_booking&bookingId={bookingId}"
        },
        "style": "primary"
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "取消預約",
          "data": "action=cancel_booking&bookingId={bookingId}"
        },
        "style": "secondary"
      }
    ]
  }
}
```

---

## 8. 預約提醒
```json
{
  "type": "bubble",
  "header": {
    "type": "box",
    "layout": "vertical",
    "backgroundColor": "#FF9800",
    "paddingAll": "md",
    "contents": [
      {
        "type": "text",
        "text": "⏰ 預約提醒",
        "color": "#FFFFFF",
        "weight": "bold",
        "size": "lg"
      }
    ]
  },
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "您的預約即將開始",
        "weight": "bold",
        "size": "md"
      },
      {
        "type": "separator",
        "margin": "md"
      },
      {
        "type": "box",
        "layout": "vertical",
        "margin": "md",
        "spacing": "sm",
        "contents": [
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "服務", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{serviceName}", "flex": 3}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "時間", "color": "#666666", "flex": 2},
              {"type": "text", "text": "{dateTime}", "flex": 3, "weight": "bold", "color": "#FF9800"}
            ]
          }
        ]
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "uri",
          "label": "導航前往",
          "uri": "https://www.google.com/maps/search/?api=1&query={encodedAddress}"
        },
        "style": "primary"
      }
    ]
  }
}
```

---

## 9. 我的預約列表
```json
{
  "type": "carousel",
  "contents": [
    {
      "type": "bubble",
      "size": "kilo",
      "body": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "text",
            "text": "{serviceName}",
            "weight": "bold",
            "size": "md"
          },
          {
            "type": "text",
            "text": "{date} {time}",
            "size": "sm",
            "color": "#1DB446",
            "margin": "sm"
          },
          {
            "type": "text",
            "text": "服務人員：{staffName}",
            "size": "xs",
            "color": "#666666",
            "margin": "sm"
          },
          {
            "type": "box",
            "layout": "horizontal",
            "margin": "md",
            "contents": [
              {
                "type": "text",
                "text": "{statusBadge}",
                "size": "xs",
                "color": "#FFFFFF",
                "align": "center",
                "backgroundColor": "{statusColor}",
                "cornerRadius": "sm",
                "paddingAll": "xs"
              }
            ]
          }
        ]
      },
      "footer": {
        "type": "box",
        "layout": "horizontal",
        "spacing": "sm",
        "contents": [
          {
            "type": "button",
            "action": {
              "type": "postback",
              "label": "詳情",
              "data": "action=view_booking&bookingId={bookingId}"
            },
            "style": "secondary",
            "height": "sm"
          },
          {
            "type": "button",
            "action": {
              "type": "postback",
              "label": "取消",
              "data": "action=cancel_booking&bookingId={bookingId}"
            },
            "style": "secondary",
            "height": "sm"
          }
        ]
      }
    }
  ]
}
```

---

## 10. 票券卡片
```json
{
  "type": "bubble",
  "size": "kilo",
  "header": {
    "type": "box",
    "layout": "vertical",
    "backgroundColor": "#FF5722",
    "paddingAll": "md",
    "contents": [
      {
        "type": "text",
        "text": "🎫 {couponName}",
        "color": "#FFFFFF",
        "weight": "bold"
      }
    ]
  },
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "{discountText}",
        "weight": "bold",
        "size": "xl",
        "color": "#FF5722"
      },
      {
        "type": "text",
        "text": "有效期限：{expireDate}",
        "size": "xs",
        "color": "#999999",
        "margin": "md"
      },
      {
        "type": "text",
        "text": "{condition}",
        "size": "xs",
        "color": "#666666",
        "margin": "sm",
        "wrap": true
      }
    ]
  },
  "footer": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "立即使用",
          "data": "action=use_coupon&couponId={couponId}"
        },
        "style": "primary",
        "height": "sm"
      }
    ]
  }
}
```
