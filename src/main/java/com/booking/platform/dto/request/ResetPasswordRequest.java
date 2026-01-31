package com.booking.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重設密碼請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    /**
     * 重設 Token
     */
    @NotBlank(message = "Token 不能為空")
    private String token;

    /**
     * 新密碼
     */
    @NotBlank(message = "新密碼不能為空")
    @Size(min = 8, max = 50, message = "密碼長度需在 8-50 字元之間")
    private String newPassword;

    /**
     * 確認新密碼
     */
    @NotBlank(message = "確認密碼不能為空")
    private String confirmPassword;
}
