package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日報表回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 預約數
     */
    private Long bookingCount;

    /**
     * 完成數
     */
    private Long completedCount;

    /**
     * 新顧客數
     */
    private Long newCustomerCount;

    /**
     * 營收
     */
    private BigDecimal revenue;
}
