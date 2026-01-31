package com.booking.platform.entity.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 超級管理員帳號
 *
 * <p>資料表：admin_users
 *
 * <p>用於超級管理員登入系統
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "admin_users",
        indexes = {
                @Index(name = "idx_admin_users_username", columnList = "username", unique = true),
                @Index(name = "idx_admin_users_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    // ========================================
    // 主鍵
    // ========================================

    /**
     * 主鍵（UUID）
     */
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    // ========================================
    // 帳號資訊
    // ========================================

    /**
     * 使用者名稱
     */
    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    /**
     * 電子郵件
     */
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    /**
     * 密碼（BCrypt 加密）
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 顯示名稱
     */
    @Column(name = "display_name", length = 50)
    private String displayName;

    // ========================================
    // 狀態欄位
    // ========================================

    /**
     * 是否啟用
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 是否鎖定
     */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * 登入失敗次數
     */
    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * 鎖定時間
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    // ========================================
    // 時間欄位
    // ========================================

    /**
     * 最後登入時間
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 最後登入 IP
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 建立時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // 生命週期回調
    // ========================================

    /**
     * 新增前自動設定 ID 和建立時間
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新前自動設定更新時間
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 記錄登入成功
     *
     * @param ip 登入 IP
     */
    public void recordLoginSuccess(String ip) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.failedAttempts = 0;
        this.isLocked = false;
        this.lockedAt = null;
    }

    /**
     * 記錄登入失敗
     *
     * @param maxAttempts 最大允許次數
     */
    public void recordLoginFailure(int maxAttempts) {
        this.failedAttempts = this.failedAttempts + 1;
        if (this.failedAttempts >= maxAttempts) {
            this.isLocked = true;
            this.lockedAt = LocalDateTime.now();
        }
    }

    /**
     * 解鎖帳號
     */
    public void unlock() {
        this.isLocked = false;
        this.lockedAt = null;
        this.failedAttempts = 0;
    }

    /**
     * 檢查帳號是否可用
     *
     * @return true 表示可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(this.isEnabled) && !Boolean.TRUE.equals(this.isLocked);
    }
}
