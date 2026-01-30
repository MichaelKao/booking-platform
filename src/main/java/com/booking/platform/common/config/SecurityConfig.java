package com.booking.platform.common.config;

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

/**
 * Spring Security 配置
 *
 * <p>配置認證和授權規則
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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

            // 登入
            "/api/auth/login",
            "/api/auth/refresh",

            // Swagger（開發環境）
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // TODO: 開發階段暫時開放，正式環境要移除
            "/api/admin/**",
            "/api/staffs/**",
            "/api/services/**",
            "/api/bookings/**",
            "/api/customers/**",
            "/api/membership-levels/**",
            "/api/coupons/**",
            "/api/campaigns/**",
            "/api/products/**",
            "/api/reports/**"
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
     * 安全過濾鏈
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 停用 CSRF（使用 JWT）
                .csrf(AbstractHttpConfigurer::disable)

                // 停用 Session（使用 JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 授權規則
                .authorizeHttpRequests(auth -> auth
                        // 白名單路徑
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // 超級管理後台需要 ADMIN 角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 其他請求需要認證
                        .anyRequest().authenticated()
                );

        // TODO: 加入 JWT 過濾器
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
