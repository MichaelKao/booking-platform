# LINE Flex Message 模板

建立 LINE Flex Message 時的常用模板。

## 預約確認訊息
```json
{
  "type": "bubble",
  "header": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "預約確認",
        "weight": "bold",
        "size": "xl",
        "color": "#1DB446"
      }
    ]
  },
  "body": {
    "type": "box",
    "layout": "vertical",
    "contents": [
      {
        "type": "text",
        "text": "{serviceName}",
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
        "contents": [
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "日期", "color": "#666666", "flex": 1},
              {"type": "text", "text": "{date}", "flex": 2}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "時間", "color": "#666666", "flex": 1},
              {"type": "text", "text": "{time}", "flex": 2}
            ]
          },
          {
            "type": "box",
            "layout": "horizontal",
            "contents": [
              {"type": "text", "text": "服務人員", "color": "#666666", "flex": 1},
              {"type": "text", "text": "{staffName}", "flex": 2}
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
          "type": "postback",
          "label": "查看預約",
          "data": "action=view_booking&id={bookingId}"
        },
        "style": "primary"
      },
      {
        "type": "button",
        "action": {
          "type": "postback",
          "label": "取消預約",
          "data": "action=cancel_booking&id={bookingId}"
        },
        "style": "secondary",
        "margin": "sm"
      }
    ]
  }
}
```

## 服務選擇輪播
```json
{
  "type": "carousel",
  "contents": [
    {
      "type": "bubble",
      "body": {
        "type": "box",
        "layout": "vertical",
        "contents": [
          {
            "type": "text",
            "text": "{serviceName}",
            "weight": "bold",
            "size": "lg"
          },
          {
            "type": "text",
            "text": "{description}",
            "size": "sm",
            "color": "#666666",
            "margin": "sm"
          },
          {
            "type": "text",
            "text": "NT$ {price}",
            "weight": "bold",
            "size": "lg",
            "margin": "md"
          },
          {
            "type": "text",
            "text": "約 {duration} 分鐘",
            "size": "sm",
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
              "data": "action=select_service&id={serviceId}"
            },
            "style": "primary"
          }
        ]
      }
    }
  ]
}
```