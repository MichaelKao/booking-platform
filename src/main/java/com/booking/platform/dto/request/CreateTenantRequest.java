package com.booking.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建立租戶請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    /**
     * 租戶代碼（必填，用於 URL）
     */
    @NotBlank(message = "租戶代碼不能為空")
    @Size(min = 3, max = 50, message = "租戶代碼長度需在 3-50 字之間")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "租戶代碼只能包含小寫英文、數字和連字號")
    private String code;

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
     * 是否為測試帳號
     */
    private Boolean isTestAccount;
}
