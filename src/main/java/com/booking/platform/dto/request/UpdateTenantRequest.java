package com.booking.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新租戶請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    /**
     * 店家名稱（必填）
     */
    @NotBlank(message = "店家名稱不能為空")
    @Size(max = 100, message = "店家名稱長度不能超過 100 字")
    private String name;

    /**
     * 店家描述（選填）
     */
    @Size(max = 500, message = "店家描述長度不能超過 500 字")
    private String description;

    /**
     * 店家 Logo URL
     */
    @Size(max = 500, message = "Logo URL 長度不能超過 500 字")
    private String logoUrl;

    /**
     * 聯絡電話（選填）
     */
    @Size(max = 20, message = "聯絡電話長度不能超過 20 字")
    private String phone;

    /**
     * 聯絡信箱（選填）
     */
    @Email(message = "請輸入有效的電子郵件地址")
    @Size(max = 100, message = "信箱長度不能超過 100 字")
    private String email;

    /**
     * 店家地址（選填）
     */
    @Size(max = 200, message = "地址長度不能超過 200 字")
    private String address;

    /**
     * 重設密碼（選填，僅超管可用）
     */
    @Size(min = 8, max = 50, message = "密碼長度需在 8-50 字元之間")
    private String password;
}
