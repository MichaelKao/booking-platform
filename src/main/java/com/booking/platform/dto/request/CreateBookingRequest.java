package com.booking.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 建立預約請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "預約日期不能為空")
    private LocalDate bookingDate;

    @NotNull(message = "開始時間不能為空")
    private LocalTime startTime;

    private String customerId;

    private String staffId;

    @NotBlank(message = "服務項目 ID 不能為空")
    private String serviceItemId;

    @Size(max = 500, message = "顧客備註長度不能超過 500 字")
    private String customerNote;

    private String source;
}
