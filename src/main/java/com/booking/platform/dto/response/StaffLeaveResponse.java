package com.booking.platform.dto.response;

import com.booking.platform.enums.LeaveType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 員工請假回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
public class StaffLeaveResponse {

    private String id;
    private String staffId;
    private String staffName;
    private LocalDate leaveDate;
    private LeaveType leaveType;
    private String leaveTypeDescription;
    private String reason;
    private Boolean isFullDay;
    private String startTime;
    private String endTime;
    private LocalDateTime createdAt;
}
