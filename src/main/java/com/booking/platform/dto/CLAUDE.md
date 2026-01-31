# DTO 規範

## 結構

```
dto/
├── request/    # 請求 DTO
└── response/   # 回應 DTO
```

## 命名

| 類型 | 命名 | 範例 |
|------|------|------|
| 新增請求 | CreateXxxRequest | CreateBookingRequest |
| 更新請求 | UpdateXxxRequest | UpdateBookingRequest |
| 回應 | XxxResponse | BookingResponse |
| 列表項 | XxxListItemResponse | TenantListItemResponse |

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

## 常用驗證

| 標註 | 用途 |
|------|------|
| @NotBlank | 字串必填且非空白 |
| @NotNull | 必填 |
| @Size(max=n) | 長度限制 |
| @Email | 信箱格式 |
| @Pattern | 正則驗證 |
| @Min/@Max | 數值範圍 |

## 認證相關

```java
LoginRequest          // username, password
TenantRegisterRequest // code, name, email, phone, password, confirmPassword
ForgotPasswordRequest // email
ResetPasswordRequest  // token, newPassword, confirmPassword
ChangePasswordRequest // currentPassword, newPassword, confirmPassword
LoginResponse         // accessToken, refreshToken, userId, role, tenantId
```
