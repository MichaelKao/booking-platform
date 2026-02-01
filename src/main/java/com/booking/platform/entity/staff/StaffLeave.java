package com.booking.platform.entity.staff;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.LeaveType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 員工請假記錄
 *
 * <p>資料表：staff_leaves
 *
 * <p>用於記錄員工的特定日期請假（如：下週二休假、2/15-2/17 請假）
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "staff_leaves",
        indexes = {
                @Index(name = "idx_staff_leaves_staff_date", columnList = "staff_id, leave_date"),
                @Index(name = "idx_staff_leaves_tenant_date", columnList = "tenant_id, leave_date"),
                @Index(name = "idx_staff_leaves_deleted", columnList = "deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLeave extends BaseEntity {

    /**
     * 員工 ID
     */
    @Column(name = "staff_id", nullable = false, length = 36)
    private String staffId;

    /**
     * 請假日期
     */
    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    /**
     * 請假類型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    @Builder.Default
    private LeaveType leaveType = LeaveType.PERSONAL;

    /**
     * 請假原因/備註
     */
    @Column(name = "reason", length = 200)
    private String reason;

    /**
     * 是否為全天請假
     */
    @Column(name = "is_full_day", nullable = false)
    @Builder.Default
    private Boolean isFullDay = true;

    /**
     * 開始時間（半天假時使用）
     */
    @Column(name = "start_time", length = 5)
    private String startTime;

    /**
     * 結束時間（半天假時使用）
     */
    @Column(name = "end_time", length = 5)
    private String endTime;
}
