package com.booking.platform.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 更新預約請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequest {

    /**
     * 服務項目 ID
     */
    private String serviceItemId;

    /**
     * 員工 ID
     */
    private String staffId;

    /**
     * 預約日期
     */
    @Future(message = "預約日期必須是未來的日期")
    private LocalDate bookingDate;

    /**
     * 開始時間
     */
    private LocalTime startTime;

    /**
     * 結束時間
     */
    private LocalTime endTime;

    /**
     * 顧客備註
     */
    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String customerNote;

    /**
     * 店家備註（內部使用）
     */
    @Size(max = 500, message = "店家備註長度不能超過 500 字")
    private String staffNote;
}
