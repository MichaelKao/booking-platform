package com.booking.platform.entity.customer;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.CustomerStatus;
import com.booking.platform.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 顧客
 *
 * <p>資料表：customers
 *
 * <p>索引設計：
 * <ul>
 *   <li>idx_customers_tenant_line - LINE ID 查詢</li>
 *   <li>idx_customers_tenant_phone - 手機號碼查詢</li>
 *   <li>idx_customers_tenant_status - 狀態查詢</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_tenant_line", columnList = "tenant_id, line_user_id"),
                @Index(name = "idx_customers_tenant_phone", columnList = "tenant_id, phone"),
                @Index(name = "idx_customers_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_customers_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    // ========================================
    // LINE 資訊
    // ========================================

    /**
     * LINE User ID
     */
    @Column(name = "line_user_id", length = 50)
    private String lineUserId;

    /**
     * LINE 顯示名稱
     */
    @Column(name = "line_display_name", length = 100)
    private String lineDisplayName;

    /**
     * LINE 頭像 URL
     */
    @Column(name = "line_picture_url", length = 500)
    private String linePictureUrl;

    // ========================================
    // 基本資料
    // ========================================

    /**
     * 顧客姓名
     */
    @Column(name = "name", length = 50)
    private String name;

    /**
     * 暱稱
     */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 手機號碼
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 電子郵件
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 性別
     */
    @Column(name = "gender", length = 10)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    /**
     * 生日
     */
    @Column(name = "birthday")
    private LocalDate birthday;

    /**
     * 地址
     */
    @Column(name = "address", length = 200)
    private String address;

    // ========================================
    // 狀態
    // ========================================

    /**
     * 顧客狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * 是否已封鎖 LINE
     */
    @Column(name = "is_line_blocked", nullable = false)
    @Builder.Default
    private Boolean isLineBlocked = false;

    // ========================================
    // 會員資訊
    // ========================================

    /**
     * 會員等級 ID
     */
    @Column(name = "membership_level_id", length = 36)
    private String membershipLevelId;

    /**
     * 累積消費金額
     */
    @Column(name = "total_spent", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /**
     * 累積消費次數
     */
    @Column(name = "visit_count")
    @Builder.Default
    private Integer visitCount = 0;

    /**
     * 會員點數餘額
     */
    @Column(name = "point_balance")
    @Builder.Default
    private Integer pointBalance = 0;

    // ========================================
    // 統計資訊
    // ========================================

    /**
     * 爽約次數
     */
    @Column(name = "no_show_count")
    @Builder.Default
    private Integer noShowCount = 0;

    /**
     * 最後來店日期
     */
    @Column(name = "last_visit_at")
    private LocalDateTime lastVisitAt;

    // ========================================
    // 備註
    // ========================================

    /**
     * 顧客備註（內部）
     */
    @Column(name = "note", length = 1000)
    private String note;

    /**
     * 標籤（逗號分隔）
     */
    @Column(name = "tags", length = 500)
    private String tags;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 取得顯示名稱
     */
    public String getDisplayName() {
        if (this.name != null && !this.name.isEmpty()) {
            return this.name;
        }
        if (this.nickname != null && !this.nickname.isEmpty()) {
            return this.nickname;
        }
        if (this.lineDisplayName != null && !this.lineDisplayName.isEmpty()) {
            return this.lineDisplayName;
        }
        return "未命名顧客";
    }

    /**
     * 檢查是否可預約
     */
    public boolean canBook() {
        return CustomerStatus.ACTIVE.equals(this.status);
    }

    /**
     * 增加消費紀錄
     */
    public void addVisit(BigDecimal amount) {
        this.visitCount++;
        this.totalSpent = this.totalSpent.add(amount);
        this.lastVisitAt = LocalDateTime.now();
    }

    /**
     * 增加爽約紀錄
     */
    public void addNoShow() {
        this.noShowCount++;
    }

    /**
     * 增加點數
     */
    public void addPoints(int points) {
        this.pointBalance += points;
    }

    /**
     * 扣除點數
     */
    public boolean deductPoints(int points) {
        if (this.pointBalance < points) {
            return false;
        }
        this.pointBalance -= points;
        return true;
    }

    /**
     * 檢查是否今天生日
     */
    public boolean isBirthdayToday() {
        if (this.birthday == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return this.birthday.getMonth() == today.getMonth()
                && this.birthday.getDayOfMonth() == today.getDayOfMonth();
    }
}
