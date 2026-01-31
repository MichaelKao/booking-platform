package com.booking.platform.common.config;

import com.booking.platform.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 *
 * <p>配置認證和授權規則
 *
 * <p>使用 JWT 進行無狀態認證
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ========================================
    // 依賴注入
    // ========================================

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ========================================
    // 白名單路徑
    // ========================================

    /**
     * 不需要認證的路徑
     */
    private static final String[] PUBLIC_PATHS = {
            // 健康檢查
            "/actuator/**",
            "/health",

            // LINE Webhook（使用簽名驗證）
            "/api/line/webhook/**",

            // 公開 API
            "/api/public/**",

            // 認證 API
            "/api/auth/**",

            // Swagger（開發環境）
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // 靜態資源
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",

            // 頁面（由頁面 Controller 處理認證）
            "/admin/**",
            "/tenant/**",

            // 錯誤頁面
            "/error/**"
    };

    /**
     * 開發階段暫時開放的路徑
     *
     * <p>注意：需要 TenantContext 的 API 不能放在這裡
     * <p>正式環境應該移除這些設定
     */
    private static final String[] DEV_OPEN_PATHS = {
            // 注意：以下 API 需要認證以取得 TenantContext，已移除：
            // "/api/points/**", "/api/feature-store/**"
    };

    // ========================================
    // Bean 配置
    // ========================================

    /**
     * 密碼編碼器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允許的來源
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允許的方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 允許的 Header
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-Tenant-Id"
        ));

        // 暴露的 Header
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count"
        ));

        // 允許攜帶 Cookie
        configuration.setAllowCredentials(true);

        // 預檢請求的快取時間
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 安全過濾鏈
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 停用 CSRF（使用 JWT）
                .csrf(AbstractHttpConfigurer::disable)

                // 啟用 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 停用 Session（使用 JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 授權規則
                .authorizeHttpRequests(auth -> auth
                        // 白名單路徑
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // 開發階段暫時開放
                        .requestMatchers(DEV_OPEN_PATHS).permitAll()

                        // 超級管理後台 API 需要 ADMIN 角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 店家後台 API 需要 TENANT 角色
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "TENANT")

                        // 其他請求需要認證
                        .anyRequest().authenticated()
                )

                // 加入 JWT 過濾器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
