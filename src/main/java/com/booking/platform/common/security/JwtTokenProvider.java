package com.booking.platform.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 提供者
 *
 * <p>負責 JWT Token 的產生和驗證
 *
 * <p>支援兩種類型的 Token：
 * <ul>
 *   <li>Access Token - 用於 API 存取，較短有效期</li>
 *   <li>Refresh Token - 用於刷新 Access Token，較長有效期</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@Slf4j
public class JwtTokenProvider {

    // ========================================
    // 設定值
    // ========================================

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * 簽名金鑰
     */
    private SecretKey signingKey;

    // ========================================
    // 初始化
    // ========================================

    /**
     * 初始化簽名金鑰
     */
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ========================================
    // Token 產生
    // ========================================

    /**
     * 產生 Access Token
     *
     * @param userId 使用者 ID
     * @param username 使用者名稱
     * @param role 角色（ADMIN 或 TENANT）
     * @param tenantId 租戶 ID（店家登入時使用）
     * @return Access Token
     */
    public String generateAccessToken(String userId, String username, String role, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");

        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }

        return createToken(claims, userId, expiration);
    }

    /**
     * 產生 Refresh Token
     *
     * @param userId 使用者 ID
     * @param role 角色
     * @return Refresh Token
     */
    public String generateRefreshToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("type", "refresh");

        return createToken(claims, userId, refreshExpiration);
    }

    /**
     * 建立 Token
     *
     * @param claims 自訂資料
     * @param subject 主體（使用者 ID）
     * @param expirationMs 有效期（毫秒）
     * @return JWT Token
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ========================================
    // Token 驗證
    // ========================================

    /**
     * 驗證 Token
     *
     * @param token JWT Token
     * @return true 表示有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已過期：{}", e.getMessage());
        } catch (JwtException e) {
            log.debug("Token 無效：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 檢查是否為 Access Token
     *
     * @param token JWT Token
     * @return true 表示是 Access Token
     */
    public boolean isAccessToken(String token) {
        Claims claims = getClaims(token);
        return claims != null && "access".equals(claims.get("type", String.class));
    }

    /**
     * 檢查是否為 Refresh Token
     *
     * @param token JWT Token
     * @return true 表示是 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = getClaims(token);
        return claims != null && "refresh".equals(claims.get("type", String.class));
    }

    // ========================================
    // Token 解析
    // ========================================

    /**
     * 取得 Claims
     *
     * @param token JWT Token
     * @return Claims，無效時回傳 null
     */
    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("解析 Token 失敗：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 取得使用者 ID
     *
     * @param token JWT Token
     * @return 使用者 ID
     */
    public String getUserId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * 取得使用者名稱
     *
     * @param token JWT Token
     * @return 使用者名稱
     */
    public String getUsername(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 取得角色
     *
     * @param token JWT Token
     * @return 角色
     */
    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * 取得租戶 ID
     *
     * @param token JWT Token
     * @return 租戶 ID，超級管理員回傳 null
     */
    public String getTenantId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("tenantId", String.class) : null;
    }

    /**
     * 取得 Token 剩餘有效期（秒）
     *
     * @param token JWT Token
     * @return 剩餘秒數，已過期回傳 0
     */
    public long getExpirationSeconds(String token) {
        Claims claims = getClaims(token);
        if (claims == null) {
            return 0;
        }

        Date expiration = claims.getExpiration();
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }
}
