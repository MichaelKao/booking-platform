package com.booking.platform.dto.request;

import com.booking.platform.enums.Gender;
import com.booking.platform.enums.StaffStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新員工請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRequest {

    /**
     * 員工姓名
     */
    @Size(max = 50, message = "姓名長度不能超過 50 字")
    private String name;

    /**
     * 手機號碼
     */
    @Size(max = 20, message = "手機號碼長度不能超過 20 字")
    private String phone;

    /**
     * 電子郵件
     */
    @Email(message = "電子郵件格式不正確")
    @Size(max = 100, message = "電子郵件長度不能超過 100 字")
    private String email;

    /**
     * 性別
     */
    private Gender gender;

    /**
     * 職稱
     */
    @Size(max = 50, message = "職稱長度不能超過 50 字")
    private String title;

    /**
     * 個人簡介
     */
    @Size(max = 500, message = "個人簡介長度不能超過 500 字")
    private String bio;

    /**
     * 大頭照 URL
     */
    @Size(max = 500, message = "大頭照 URL 長度不能超過 500 字")
    private String avatarUrl;

    /**
     * 狀態
     */
    private StaffStatus status;

    /**
     * 排序順序
     */
    @Min(value = 0, message = "排序順序不能為負數")
    @Max(value = 9999, message = "排序順序不能超過 9999")
    private Integer sortOrder;

    /**
     * 是否接受預約
     */
    private Boolean isBookable;

    /**
     * 是否顯示於前台
     */
    private Boolean isVisible;

    /** 同一時段最大同時預約數 */
    @Min(value = 1, message = "最大同時預約數至少 1")
    private Integer maxConcurrentBookings;
}
