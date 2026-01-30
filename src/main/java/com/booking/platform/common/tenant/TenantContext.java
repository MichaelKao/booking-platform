package com.booking.platform.common.tenant;

/**
 * 租戶上下文
 *
 * <p>使用 ThreadLocal 存儲當前請求的租戶資訊
 *
 * <p>使用範例：
 * <pre>{@code
 * // 取得當前租戶 ID
 * String tenantId = TenantContext.getTenantId();
 *
 * // 設定當前租戶 ID（通常由 Filter 設定）
 * TenantContext.setTenantId("tenant-uuid");
 *
 * // 清除租戶資訊（請求結束時）
 * TenantContext.clear();
 * }</pre>
 *
 * @author Developer
 * @since 1.0.0
 */
public class TenantContext {

    // ========================================
    // ThreadLocal 存儲
    // ========================================

    /**
     * 租戶 ID
     */
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    /**
     * 租戶代碼
     */
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    /**
     * 使用者 ID
     */
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    /**
     * 使用者名稱
     */
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    // ========================================
    // 租戶 ID
    // ========================================

    /**
     * 取得當前租戶 ID
     *
     * @return 租戶 ID，若未設定則返回 null
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 設定當前租戶 ID
     *
     * @param tenantId 租戶 ID
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    // ========================================
    // 租戶代碼
    // ========================================

    /**
     * 取得當前租戶代碼
     *
     * @return 租戶代碼，若未設定則返回 null
     */
    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    /**
     * 設定當前租戶代碼
     *
     * @param tenantCode 租戶代碼
     */
    public static void setTenantCode(String tenantCode) {
        TENANT_CODE.set(tenantCode);
    }

    // ========================================
    // 使用者 ID
    // ========================================

    /**
     * 取得當前使用者 ID
     *
     * @return 使用者 ID，若未設定則返回 null
     */
    public static String getUserId() {
        return USER_ID.get();
    }

    /**
     * 設定當前使用者 ID
     *
     * @param userId 使用者 ID
     */
    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    // ========================================
    // 使用者名稱
    // ========================================

    /**
     * 取得當前使用者名稱
     *
     * @return 使用者名稱，若未設定則返回 null
     */
    public static String getUsername() {
        return USERNAME.get();
    }

    /**
     * 設定當前使用者名稱
     *
     * @param username 使用者名稱
     */
    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    // ========================================
    // 清除
    // ========================================

    /**
     * 清除所有上下文資訊
     *
     * <p>必須在請求結束時呼叫，避免記憶體洩漏
     */
    public static void clear() {
        TENANT_ID.remove();
        TENANT_CODE.remove();
        USER_ID.remove();
        USERNAME.remove();
    }

    // ========================================
    // 工具方法
    // ========================================

    /**
     * 檢查是否有租戶上下文
     *
     * @return true 表示已設定租戶 ID
     */
    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }

    /**
     * 檢查是否有使用者上下文
     *
     * @return true 表示已設定使用者 ID
     */
    public static boolean hasUser() {
        return USER_ID.get() != null;
    }

    // 私有建構子，防止實例化
    private TenantContext() {
    }
}
