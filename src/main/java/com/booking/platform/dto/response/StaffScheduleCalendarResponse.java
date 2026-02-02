package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 員工排班行事曆回應
 *
 * <p>用於 FullCalendar 顯示員工排班
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleCalendarResponse {

    /**
     * 事件 ID
     */
    private String id;

    /**
     * 員工 ID
     */
    private String staffId;

    /**
     * 員工姓名
     */
    private String staffName;

    /**
     * 事件類型：WORK（上班）, LEAVE（請假）, BREAK（休息）
     */
    private String type;

    /**
     * 事件標題
     */
    private String title;

    /**
     * 開始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 開始時間
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 結束時間
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 開始日期時間（ISO 8601 格式，FullCalendar 用）
     */
    private String start;

    /**
     * 結束日期時間（ISO 8601 格式，FullCalendar 用）
     */
    private String end;

    /**
     * 背景顏色
     */
    private String backgroundColor;

    /**
     * 邊框顏色
     */
    private String borderColor;

    /**
     * 文字顏色
     */
    private String textColor;

    /**
     * 是否全天事件
     */
    private Boolean allDay;

    /**
     * 請假類型（僅 LEAVE 類型有值）
     */
    private String leaveType;

    /**
     * 請假原因（僅 LEAVE 類型有值）
     */
    private String leaveReason;

    /**
     * 是否可拖動
     */
    @Builder.Default
    private Boolean editable = false;
}
