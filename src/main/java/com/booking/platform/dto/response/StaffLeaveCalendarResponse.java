package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 員工請假行事曆回應
 *
 * <p>用於 FullCalendar 顯示員工請假
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveCalendarResponse {

    /**
     * 請假 ID
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
     * 請假日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate leaveDate;

    /**
     * 請假類型：PERSONAL（事假）, SICK（病假）, VACATION（休假）, ANNUAL（特休）, OTHER（其他）
     */
    private String leaveType;

    /**
     * 請假類型顯示文字
     */
    private String leaveTypeText;

    /**
     * 請假原因
     */
    private String reason;

    /**
     * 是否全天
     */
    private Boolean isFullDay;

    /**
     * 開始時間（半天假時有值）
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 結束時間（半天假時有值）
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * FullCalendar 事件標題
     */
    private String title;

    /**
     * FullCalendar 開始日期時間
     */
    private String start;

    /**
     * FullCalendar 結束日期時間
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
     * 是否全天事件
     */
    private Boolean allDay;
}
