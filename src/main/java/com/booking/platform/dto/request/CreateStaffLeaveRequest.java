package com.booking.platform.dto.request;

import com.booking.platform.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 建立員工請假請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
public class CreateStaffLeaveRequest {

    /**
     * 請假日期列表（支援多日請假）
     */
    @NotNull(message = "請選擇請假日期")
    private List<LocalDate> leaveDates;

    /**
     * 請假類型
     */
    private LeaveType leaveType = LeaveType.PERSONAL;

    /**
     * 請假原因
     */
    @Size(max = 200, message = "請假原因不能超過200字")
    private String reason;

    /**
     * 是否為全天請假
     */
    private Boolean isFullDay = true;

    /**
     * 開始時間（半天假使用，格式：HH:mm）
     */
    private String startTime;

    /**
     * 結束時間（半天假使用，格式：HH:mm）
     */
    private String endTime;
}
