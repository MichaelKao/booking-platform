package com.booking.platform.repository;

import com.booking.platform.entity.system.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 超級管理員帳號 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    // ========================================
    // 基本查詢
    // ========================================

    /**
     * 依使用者名稱查詢
     *
     * @param username 使用者名稱
     * @return 管理員帳號
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 依電子郵件查詢
     *
     * @param email 電子郵件
     * @return 管理員帳號
     */
    Optional<AdminUser> findByEmail(String email);

    /**
     * 依使用者名稱或電子郵件查詢
     *
     * @param username 使用者名稱
     * @param email 電子郵件
     * @return 管理員帳號
     */
    Optional<AdminUser> findByUsernameOrEmail(String username, String email);

    // ========================================
    // 存在性檢查
    // ========================================

    /**
     * 檢查使用者名稱是否存在
     *
     * @param username 使用者名稱
     * @return true 表示存在
     */
    boolean existsByUsername(String username);

    /**
     * 檢查電子郵件是否存在
     *
     * @param email 電子郵件
     * @return true 表示存在
     */
    boolean existsByEmail(String email);
}
