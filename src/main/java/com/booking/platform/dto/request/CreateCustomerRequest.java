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
 * 建立顧客請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @Size(max = 50, message = "LINE User ID 長度不能超過 50 字")
    private String lineUserId;

    @Size(max = 50, message = "顧客姓名長度不能超過 50 字")
    private String name;

    @Size(max = 50, message = "暱稱長度不能超過 50 字")
    private String nickname;

    @Size(max = 20, message = "手機號碼長度不能超過 20 字")
    private String phone;

    @Email(message = "請輸入有效的電子郵件地址")
    @Size(max = 100, message = "電子郵件長度不能超過 100 字")
    private String email;

    private Gender gender;

    private LocalDate birthday;

    @Size(max = 200, message = "地址長度不能超過 200 字")
    private String address;

    @Size(max = 1000, message = "備註長度不能超過 1000 字")
    private String note;

    @Size(max = 500, message = "標籤長度不能超過 500 字")
    private String tags;
}
