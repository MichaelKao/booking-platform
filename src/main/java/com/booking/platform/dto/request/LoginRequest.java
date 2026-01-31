package com.booking.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登入請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 使用者名稱或電子郵件
     */
    @NotBlank(message = "帳號不能為空")
    private String username;

    /**
     * 密碼
     */
    @NotBlank(message = "密碼不能為空")
    private String password;
}
