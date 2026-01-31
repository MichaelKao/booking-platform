package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登入回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * Token 類型（固定為 Bearer）
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access Token 有效期（秒）
     */
    private Long expiresIn;

    /**
     * 使用者 ID
     */
    private String userId;

    /**
     * 使用者名稱
     */
    private String username;

    /**
     * 顯示名稱
     */
    private String displayName;

    /**
     * 角色
     */
    private String role;

    /**
     * 租戶 ID（店家登入時回傳）
     */
    private String tenantId;

    /**
     * 店家名稱（店家登入時回傳）
     */
    private String tenantName;
}
