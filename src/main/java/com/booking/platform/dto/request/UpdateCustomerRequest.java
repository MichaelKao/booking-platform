package com.booking.platform.dto.request;

import com.booking.platform.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 更新顧客請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    /**
     * 顧客姓名
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
     * 生日
     */
    private LocalDate birthday;

    /**
     * 地址
     */
    @Size(max = 200, message = "地址長度不能超過 200 字")
    private String address;

    /**
     * 備註
     */
    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;

    /**
     * 會員等級 ID
     */
    private String membershipLevelId;

    /**
     * 標籤（逗號分隔）
     */
    @Size(max = 200, message = "標籤長度不能超過 200 字")
    private String tags;
}
