package com.booking.platform.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * JWT Token 提供者測試
 *
 * @author Developer
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("產生 Access Token 成功")
    void generateAccessToken_Success() {
        String token = jwtTokenProvider.generateAccessToken(
                "user-123", "testuser", "ADMIN", null
        );

        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("user-123");
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("產生 Refresh Token 成功")
    void generateRefreshToken_Success() {
        String token = jwtTokenProvider.generateRefreshToken("user-123", "ADMIN");

        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("user-123");
    }

    @Test
    @DisplayName("產生 Access Token（含租戶 ID）")
    void generateAccessToken_WithTenantId() {
        String token = jwtTokenProvider.generateAccessToken(
                "tenant-123", "tenantuser", "TENANT", "tenant-123"
        );

        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.getTenantId(token)).isEqualTo("tenant-123");
    }

    @Test
    @DisplayName("驗證無效 Token")
    void validateToken_Invalid() {
        // 無效格式的 Token 應返回 false
        assertThat(jwtTokenProvider.validateToken("invalid-token")).isFalse();
    }

    @Test
    @DisplayName("取得 Token 剩餘有效期")
    void getExpirationSeconds() {
        String token = jwtTokenProvider.generateAccessToken(
                "user-123", "testuser", "ADMIN", null
        );

        long seconds = jwtTokenProvider.getExpirationSeconds(token);

        assertThat(seconds).isGreaterThan(0);
    }

    @Test
    @DisplayName("區分 Access Token 和 Refresh Token")
    void distinguishTokenType() {
        String accessToken = jwtTokenProvider.generateAccessToken(
                "user-123", "testuser", "ADMIN", null
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken("user-123", "ADMIN");

        assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();

        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
    }
}
