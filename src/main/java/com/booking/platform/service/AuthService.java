package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.security.JwtTokenProvider;
import com.booking.platform.dto.request.ChangePasswordRequest;
import com.booking.platform.dto.request.ForgotPasswordRequest;
import com.booking.platform.dto.request.LoginRequest;
import com.booking.platform.dto.request.RefreshTokenRequest;
import com.booking.platform.dto.request.ResetPasswordRequest;
import com.booking.platform.dto.request.TenantRegisterRequest;
import com.booking.platform.dto.response.LoginResponse;
import com.booking.platform.entity.system.AdminUser;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.repository.AdminUserRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 認證服務
 *
 * <p>處理超級管理員和店家的登入、登出、Token 刷新
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    // ========================================
    // 依賴注入
    // ========================================

    private final AdminUserRepository adminUserRepository;
    private final TenantRepository tenantRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FeatureService featureService;

    // ========================================
    // 設定值
    // ========================================

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    /**
     * 登入失敗最大次數
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // ========================================
    // 超級管理員認證
    // ========================================

    /**
     * 超級管理員登入
     *
     * @param request 登入請求
     * @param ip 登入 IP
     * @return 登入回應
     */
    @Transactional
    public LoginResponse adminLogin(LoginRequest request, String ip) {
        log.info("超級管理員登入，帳號：{}", request.getUsername());

        // ========================================
        // 1. 查詢帳號
        // ========================================

        AdminUser admin = adminUserRepository.findByUsernameOrEmail(
                request.getUsername(), request.getUsername()
        ).orElseThrow(() -> {
            log.warn("登入失敗，帳號不存在：{}", request.getUsername());
            return new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        });

        // ========================================
        // 2. 檢查帳號狀態
        // ========================================

        // 檢查是否停用
        if (!Boolean.TRUE.equals(admin.getIsEnabled())) {
            log.warn("登入失敗，帳號已停用：{}", request.getUsername());
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // 檢查是否鎖定
        if (Boolean.TRUE.equals(admin.getIsLocked())) {
            log.warn("登入失敗，帳號已鎖定：{}", request.getUsername());
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        // ========================================
        // 3. 驗證密碼
        // ========================================

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            // 記錄失敗次數
            admin.recordLoginFailure(MAX_FAILED_ATTEMPTS);
            adminUserRepository.save(admin);

            log.warn("登入失敗，密碼錯誤：{}，失敗次數：{}",
                    request.getUsername(), admin.getFailedAttempts());
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // ========================================
        // 4. 記錄登入成功
        // ========================================

        admin.recordLoginSuccess(ip);
        adminUserRepository.save(admin);

        // ========================================
        // 5. 產生 Token
        // ========================================

        String accessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), admin.getUsername(), "ADMIN", null
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getId(), "ADMIN");

        log.info("超級管理員登入成功：{}", admin.getUsername());

        // ========================================
        // 6. 返回結果
        // ========================================

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .userId(admin.getId())
                .username(admin.getUsername())
                .displayName(admin.getDisplayName())
                .role("ADMIN")
                .build();
    }

    // ========================================
    // 店家認證
    // ========================================

    /**
     * 店家登入
     *
     * <p>目前使用租戶代碼 + 密碼方式登入
     *
     * @param request 登入請求
     * @param ip 登入 IP
     * @return 登入回應
     */
    @Transactional
    public LoginResponse tenantLogin(LoginRequest request, String ip) {
        log.info("店家登入，帳號：{}", request.getUsername());

        // ========================================
        // 1. 查詢租戶
        // ========================================

        // 使用 code 或 email 查詢
        Tenant tenant = tenantRepository.findByCodeAndDeletedAtIsNull(request.getUsername())
                .or(() -> tenantRepository.findAll().stream()
                        .filter(t -> t.getDeletedAt() == null)
                        .filter(t -> request.getUsername().equals(t.getEmail()))
                        .findFirst())
                .orElseThrow(() -> {
                    log.warn("店家登入失敗，帳號不存在：{}", request.getUsername());
                    return new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
                });

        // ========================================
        // 2. 檢查租戶狀態
        // ========================================

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            log.warn("店家登入失敗，租戶狀態異常：{}，狀態：{}",
                    request.getUsername(), tenant.getStatus());

            if (tenant.getStatus() == TenantStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, "店家帳號已停用");
            } else if (tenant.getStatus() == TenantStatus.FROZEN) {
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "店家帳號已凍結");
            } else {
                throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, "店家帳號尚未啟用");
            }
        }

        // ========================================
        // 3. 驗證密碼
        // ========================================

        // 如果租戶有設定密碼，使用加密密碼驗證；否則使用預設密碼
        boolean passwordValid = false;
        if (tenant.getPassword() != null && !tenant.getPassword().isEmpty()) {
            passwordValid = passwordEncoder.matches(request.getPassword(), tenant.getPassword());
        } else {
            // 舊帳號相容：使用預設密碼 "password123"
            passwordValid = "password123".equals(request.getPassword());
        }

        if (!passwordValid) {
            log.warn("店家登入失敗，密碼錯誤：{}", request.getUsername());
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // ========================================
        // 4. 產生 Token
        // ========================================

        String accessToken = jwtTokenProvider.generateAccessToken(
                tenant.getId(), tenant.getCode(), "TENANT", tenant.getId()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(tenant.getId(), "TENANT");

        log.info("店家登入成功：{}，租戶 ID：{}", tenant.getCode(), tenant.getId());

        // ========================================
        // 5. 返回結果
        // ========================================

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .userId(tenant.getId())
                .username(tenant.getCode())
                .displayName(tenant.getName())
                .role("TENANT")
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .build();
    }

    // ========================================
    // Token 管理
    // ========================================

    /**
     * 刷新 Token
     *
     * @param request 刷新請求
     * @return 新的登入回應
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // ========================================
        // 1. 驗證 Refresh Token
        // ========================================

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("刷新 Token 失敗，Token 無效或已過期");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("刷新 Token 失敗，不是 Refresh Token");
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // ========================================
        // 2. 解析 Token 資訊
        // ========================================

        String userId = jwtTokenProvider.getUserId(refreshToken);
        String role = jwtTokenProvider.getRole(refreshToken);

        if (userId == null || role == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // ========================================
        // 3. 根據角色產生新 Token
        // ========================================

        if ("ADMIN".equals(role)) {
            return refreshAdminToken(userId);
        } else if ("TENANT".equals(role)) {
            return refreshTenantToken(userId);
        } else {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    /**
     * 刷新超級管理員 Token
     *
     * @param userId 使用者 ID
     * @return 登入回應
     */
    private LoginResponse refreshAdminToken(String userId) {
        AdminUser admin = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

        if (!admin.isAvailable()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), admin.getUsername(), "ADMIN", null
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getId(), "ADMIN");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .userId(admin.getId())
                .username(admin.getUsername())
                .displayName(admin.getDisplayName())
                .role("ADMIN")
                .build();
    }

    /**
     * 刷新店家 Token
     *
     * @param tenantId 租戶 ID
     * @return 登入回應
     */
    private LoginResponse refreshTenantToken(String tenantId) {
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

        if (!tenant.isAvailable()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                tenant.getId(), tenant.getCode(), "TENANT", tenant.getId()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(tenant.getId(), "TENANT");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .userId(tenant.getId())
                .username(tenant.getCode())
                .displayName(tenant.getName())
                .role("TENANT")
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .build();
    }

    // ========================================
    // 帳號管理
    // ========================================

    /**
     * 初始化預設超級管理員帳號
     *
     * <p>如果沒有任何管理員帳號，建立預設帳號
     */
    @Transactional
    public void initDefaultAdminUser() {
        if (adminUserRepository.count() == 0) {
            log.info("建立預設超級管理員帳號");

            AdminUser admin = AdminUser.builder()
                    .username("admin")
                    .email("admin@booking-platform.com")
                    .password(passwordEncoder.encode("admin123"))
                    .displayName("系統管理員")
                    .isEnabled(true)
                    .isLocked(false)
                    .failedAttempts(0)
                    .build();

            adminUserRepository.save(admin);

            log.info("預設超級管理員帳號建立成功，帳號：admin，密碼：admin123");
        }
    }

    // ========================================
    // 店家註冊
    // ========================================

    /**
     * 店家自助註冊
     *
     * @param request 註冊請求
     * @return 登入回應
     */
    @Transactional
    public LoginResponse tenantRegister(TenantRegisterRequest request) {
        log.info("店家註冊，代碼：{}，名稱：{}", request.getCode(), request.getName());

        // ========================================
        // 1. 驗證密碼確認
        // ========================================

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "密碼與確認密碼不一致");
        }

        // ========================================
        // 2. 檢查店家代碼是否已存在
        // ========================================

        if (tenantRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new BusinessException(ErrorCode.TENANT_CODE_DUPLICATE, "店家代碼已存在，請使用其他代碼");
        }

        // ========================================
        // 3. 檢查 Email 是否已存在
        // ========================================

        if (tenantRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null)
                .anyMatch(t -> request.getEmail().equalsIgnoreCase(t.getEmail()))) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "此電子郵件已被註冊");
        }

        // ========================================
        // 4. 建立租戶
        // ========================================

        String tenantId = UUID.randomUUID().toString();

        Tenant tenant = Tenant.builder()
                .code(request.getCode())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .description(request.getDescription())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(TenantStatus.ACTIVE)  // 直接啟用，可改為 PENDING 需管理員審核
                .isTestAccount(false)
                .pointBalance(BigDecimal.ZERO)
                .maxStaffCount(3)
                .monthlyPushQuota(100)
                .monthlyPushUsed(0)
                .build();

        tenant.setId(tenantId);
        tenant.setTenantId(tenantId);
        tenant.activate();

        tenant = tenantRepository.save(tenant);

        log.info("店家註冊成功，ID：{}，代碼：{}", tenant.getId(), tenant.getCode());

        // ========================================
        // 5. 初始化免費功能
        // ========================================

        featureService.initializeTenantFreeFeatures(tenantId);

        // ========================================
        // 6. 發送歡迎郵件
        // ========================================

        emailService.sendWelcomeEmail(tenant.getEmail(), tenant.getName(), tenant.getCode());

        // ========================================
        // 7. 產生 Token 並返回
        // ========================================

        String accessToken = jwtTokenProvider.generateAccessToken(
                tenant.getId(), tenant.getCode(), "TENANT", tenant.getId()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(tenant.getId(), "TENANT");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .userId(tenant.getId())
                .username(tenant.getCode())
                .displayName(tenant.getName())
                .role("TENANT")
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .build();
    }

    // ========================================
    // 忘記密碼
    // ========================================

    /**
     * 發送密碼重設信件
     *
     * @param request 忘記密碼請求
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("忘記密碼請求，Email/代碼：{}", request.getEmail());

        // ========================================
        // 1. 查詢租戶
        // ========================================

        Tenant tenant = tenantRepository.findByCodeAndDeletedAtIsNull(request.getEmail())
                .or(() -> tenantRepository.findAll().stream()
                        .filter(t -> t.getDeletedAt() == null)
                        .filter(t -> request.getEmail().equalsIgnoreCase(t.getEmail()))
                        .findFirst())
                .orElse(null);

        // 即使找不到也不報錯，避免洩漏帳號是否存在
        if (tenant == null) {
            log.info("忘記密碼：找不到對應帳號，但不報錯");
            return;
        }

        // ========================================
        // 2. 產生重設 Token
        // ========================================

        String resetToken = UUID.randomUUID().toString();
        tenant.setPasswordResetToken(resetToken);
        tenant.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));  // 1 小時內有效

        tenantRepository.save(tenant);

        // ========================================
        // 3. 發送重設郵件
        // ========================================

        emailService.sendPasswordResetEmail(tenant.getEmail(), tenant.getName(), resetToken);

        log.info("密碼重設郵件已發送，租戶：{}，Email：{}", tenant.getCode(), tenant.getEmail());
    }

    /**
     * 重設密碼
     *
     * @param request 重設密碼請求
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("重設密碼請求");

        // ========================================
        // 1. 驗證密碼確認
        // ========================================

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "密碼與確認密碼不一致");
        }

        // ========================================
        // 2. 查詢並驗證 Token
        // ========================================

        Tenant tenant = tenantRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> request.getToken().equals(t.getPasswordResetToken()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "無效的重設連結"));

        // 檢查 Token 是否過期
        if (tenant.getPasswordResetTokenExpiry() == null ||
                tenant.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "重設連結已過期，請重新申請");
        }

        // ========================================
        // 3. 更新密碼
        // ========================================

        tenant.setPassword(passwordEncoder.encode(request.getNewPassword()));
        tenant.setPasswordResetToken(null);
        tenant.setPasswordResetTokenExpiry(null);

        tenantRepository.save(tenant);

        // ========================================
        // 4. 發送變更通知
        // ========================================

        if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
            emailService.sendPasswordChangedEmail(tenant.getEmail(), tenant.getName());
        }

        log.info("密碼重設成功，租戶：{}", tenant.getCode());
    }

    // ========================================
    // 更改密碼
    // ========================================

    /**
     * 更改密碼（需登入）
     *
     * @param tenantId 租戶 ID
     * @param request 更改密碼請求
     */
    @Transactional
    public void changePassword(String tenantId, ChangePasswordRequest request) {
        log.info("更改密碼請求，租戶 ID：{}", tenantId);

        // ========================================
        // 1. 驗證新密碼確認
        // ========================================

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "新密碼與確認密碼不一致");
        }

        // ========================================
        // 2. 查詢租戶
        // ========================================

        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "找不到租戶"));

        // ========================================
        // 3. 驗證目前密碼
        // ========================================

        boolean currentPasswordValid = false;
        if (tenant.getPassword() != null && !tenant.getPassword().isEmpty()) {
            currentPasswordValid = passwordEncoder.matches(request.getCurrentPassword(), tenant.getPassword());
        } else {
            // 舊帳號相容
            currentPasswordValid = "password123".equals(request.getCurrentPassword());
        }

        if (!currentPasswordValid) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, "目前密碼錯誤");
        }

        // ========================================
        // 4. 更新密碼
        // ========================================

        tenant.setPassword(passwordEncoder.encode(request.getNewPassword()));
        tenantRepository.save(tenant);

        // ========================================
        // 5. 發送變更通知
        // ========================================

        if (tenant.getEmail() != null && !tenant.getEmail().isEmpty()) {
            emailService.sendPasswordChangedEmail(tenant.getEmail(), tenant.getName());
        }

        log.info("密碼更改成功，租戶：{}", tenant.getCode());
    }
}
