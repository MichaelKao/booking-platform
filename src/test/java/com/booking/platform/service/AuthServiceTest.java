package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.dto.request.LoginRequest;
import com.booking.platform.dto.request.RefreshTokenRequest;
import com.booking.platform.dto.response.LoginResponse;
import com.booking.platform.entity.system.AdminUser;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.repository.AdminUserRepository;
import com.booking.platform.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * 認證服務測試
 *
 * @author Developer
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AdminUser testAdmin;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // 建立測試用管理員
        testAdmin = AdminUser.builder()
                .username("testadmin")
                .email("testadmin@test.com")
                .password(passwordEncoder.encode("password123"))
                .displayName("測試管理員")
                .isEnabled(true)
                .isLocked(false)
                .failedAttempts(0)
                .build();
        adminUserRepository.save(testAdmin);

        // 建立測試用租戶
        testTenant = Tenant.builder()
                .code("test-tenant")
                .name("測試租戶")
                .email("tenant@test.com")
                .status(TenantStatus.ACTIVE)
                .pointBalance(BigDecimal.ZERO)
                .build();
        String tenantId = java.util.UUID.randomUUID().toString();
        testTenant.setId(tenantId);
        testTenant.setTenantId(tenantId);
        tenantRepository.save(testTenant);
    }

    @Test
    @DisplayName("管理員登入成功")
    void adminLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("testadmin")
                .password("password123")
                .build();

        LoginResponse response = authService.adminLogin(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getUsername()).isEqualTo("testadmin");
    }

    @Test
    @DisplayName("管理員登入成功（使用 Email）")
    void adminLogin_WithEmail_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("testadmin@test.com")
                .password("password123")
                .build();

        LoginResponse response = authService.adminLogin(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("管理員登入失敗 - 密碼錯誤")
    void adminLogin_WrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("testadmin")
                .password("wrongpassword")
                .build();

        assertThatThrownBy(() -> authService.adminLogin(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("管理員登入失敗 - 帳號停用")
    void adminLogin_AccountDisabled() {
        testAdmin.setIsEnabled(false);
        adminUserRepository.save(testAdmin);

        LoginRequest request = LoginRequest.builder()
                .username("testadmin")
                .password("password123")
                .build();

        assertThatThrownBy(() -> authService.adminLogin(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("管理員登入失敗 - 帳號鎖定")
    void adminLogin_AccountLocked() {
        testAdmin.setIsLocked(true);
        adminUserRepository.save(testAdmin);

        LoginRequest request = LoginRequest.builder()
                .username("testadmin")
                .password("password123")
                .build();

        assertThatThrownBy(() -> authService.adminLogin(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("店家登入成功")
    void tenantLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("test-tenant")
                .password("password123")
                .build();

        LoginResponse response = authService.tenantLogin(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRole()).isEqualTo("TENANT");
        assertThat(response.getTenantId()).isEqualTo(testTenant.getId());
    }

    @Test
    @DisplayName("店家登入失敗 - 租戶停用")
    void tenantLogin_TenantSuspended() {
        testTenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(testTenant);

        LoginRequest request = LoginRequest.builder()
                .username("test-tenant")
                .password("password123")
                .build();

        assertThatThrownBy(() -> authService.tenantLogin(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("刷新 Token 成功")
    void refreshToken_Success() {
        // 先登入取得 Token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testadmin")
                .password("password123")
                .build();
        LoginResponse loginResponse = authService.adminLogin(loginRequest, "127.0.0.1");

        // 使用 Refresh Token 刷新
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        LoginResponse response = authService.refreshToken(refreshRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("刷新 Token 失敗 - 無效 Token")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("初始化預設管理員帳號")
    void initDefaultAdminUser() {
        // 清除所有管理員
        adminUserRepository.deleteAll();

        // 初始化
        authService.initDefaultAdminUser();

        // 驗證預設帳號已建立
        assertThat(adminUserRepository.findByUsername("admin")).isPresent();
    }
}
