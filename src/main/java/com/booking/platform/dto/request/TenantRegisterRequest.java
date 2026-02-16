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
 * 店家註冊請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegisterRequest {

    /**
     * 店家代碼（用於登入和 LINE Webhook）
     */
    @NotBlank(message = "店家代碼不能為空")
    @Size(min = 3, max = 50, message = "店家代碼長度需在 3-50 字元之間")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "店家代碼只能包含小寫英文、數字和連字號")
    private String code;

    /**
     * 店家名稱
     */
    @NotBlank(message = "店家名稱不能為空")
    @Size(max = 100, message = "店家名稱不能超過 100 字元")
    private String name;

    /**
     * 電子郵件
     */
    @NotBlank(message = "電子郵件不能為空")
    @Email(message = "請輸入有效的電子郵件格式")
    private String email;

    /**
     * 聯絡電話
     */
    @NotBlank(message = "聯絡電話不能為空")
    @Pattern(regexp = "^[0-9]{10}$", message = "請輸入有效的手機號碼（10位數字）")
    private String phone;

    /**
     * 登入密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 50, message = "密碼長度需在 8-50 字元之間")
    private String password;

    /**
     * 確認密碼
     */
    @NotBlank(message = "確認密碼不能為空")
    private String confirmPassword;

    /**
     * 店家描述（選填）
     */
    @Size(max = 500, message = "店家描述不能超過 500 字元")
    private String description;

    /**
     * 店家地址（選填）
     */
    @Size(max = 200, message = "地址不能超過 200 字元")
    private String address;

    /**
     * 推薦碼（選填）
     */
    @Size(max = 20, message = "推薦碼不能超過 20 字元")
    private String referralCode;
}
