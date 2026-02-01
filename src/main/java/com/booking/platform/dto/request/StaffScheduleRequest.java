package com.booking.platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 員工排班請求（批次更新 7 天）
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleRequest {

    /**
     * 排班列表（7 天）
     */
    @Valid
    @NotNull(message = "排班列表不能為空")
    private List<DaySchedule> schedules;

    /**
     * 單日排班
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {

        /**
         * 星期幾（0=週日, 1=週一, ..., 6=週六）
         */
        @NotNull(message = "星期幾不能為空")
        @Min(value = 0, message = "星期幾必須在 0-6 之間")
        @Max(value = 6, message = "星期幾必須在 0-6 之間")
        private Integer dayOfWeek;

        /**
         * 是否工作日
         */
        @NotNull(message = "是否工作日不能為空")
        private Boolean isWorkingDay;

        /**
         * 開始工作時間
         */
        private LocalTime startTime;

        /**
         * 結束工作時間
         */
        private LocalTime endTime;

        /**
         * 休息開始時間
         */
        private LocalTime breakStartTime;

        /**
         * 休息結束時間
         */
        private LocalTime breakEndTime;
    }
}
