package com.booking.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建立員工請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {

    @NotBlank(message = "員工姓名不能為空")
    @Size(max = 50, message = "員工姓名長度不能超過 50 字")
    private String name;

    @Size(max = 50, message = "顯示名稱長度不能超過 50 字")
    private String displayName;

    @Size(max = 500, message = "員工簡介長度不能超過 500 字")
    private String bio;

    @Size(max = 20, message = "手機號碼長度不能超過 20 字")
    private String phone;

    @Email(message = "請輸入有效的電子郵件地址")
    @Size(max = 100, message = "電子郵件長度不能超過 100 字")
    private String email;

    private Boolean isBookable;

    private Boolean isVisible;

    private Integer sortOrder;
}
