package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 員工排班回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleResponse {

    /**
     * 員工 ID
     */
    private String staffId;

    /**
     * 員工姓名
     */
    private String staffName;

    /**
     * 排班列表（7 天）
     */
    private List<DayScheduleResponse> schedules;

    /**
     * 單日排班回應
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayScheduleResponse {

        /**
         * 排班 ID
         */
        private String id;

        /**
         * 星期幾（0=週日, 1=週一, ..., 6=週六）
         */
        private Integer dayOfWeek;

        /**
         * 星期幾名稱
         */
        private String dayOfWeekName;

        /**
         * 是否工作日
         */
        private Boolean isWorkingDay;

        /**
         * 開始工作時間
         */
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        /**
         * 結束工作時間
         */
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        /**
         * 休息開始時間
         */
        @JsonFormat(pattern = "HH:mm")
        private LocalTime breakStartTime;

        /**
         * 休息結束時間
         */
        @JsonFormat(pattern = "HH:mm")
        private LocalTime breakEndTime;
    }
}
