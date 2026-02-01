package com.booking.platform.entity.staff;

import com.booking.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 員工排班
 *
 * <p>資料表：staff_schedules
 *
 * <p>每個員工每週 7 天的工作時間設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "staff_schedules",
        indexes = {
                @Index(name = "idx_staff_schedules_staff_id", columnList = "staff_id"),
                @Index(name = "idx_staff_schedules_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_staff_schedules_deleted", columnList = "deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSchedule extends BaseEntity {

    // ========================================
    // 關聯資料
    // ========================================

    /**
     * 員工 ID
     */
    @Column(name = "staff_id", nullable = false, length = 36)
    private String staffId;

    // ========================================
    // 排班設定
    // ========================================

    /**
     * 星期幾（0=週日, 1=週一, ..., 6=週六）
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    /**
     * 是否工作日
     */
    @Column(name = "is_working_day", nullable = false)
    @Builder.Default
    private Boolean isWorkingDay = true;

    /**
     * 開始工作時間
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * 結束工作時間
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * 休息開始時間
     */
    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    /**
     * 休息結束時間
     */
    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 檢查該日是否有工作
     */
    public boolean isWorking() {
        return Boolean.TRUE.equals(this.isWorkingDay) && !this.isDeleted();
    }

    /**
     * 檢查指定時間是否在工作時段內
     *
     * @param time 要檢查的時間
     * @return true 表示在工作時段內
     */
    public boolean isWithinWorkingHours(LocalTime time) {
        if (!isWorking() || startTime == null || endTime == null) {
            return false;
        }

        // 檢查是否在工作時間內
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return false;
        }

        // 檢查是否在休息時間內
        if (breakStartTime != null && breakEndTime != null) {
            if (!time.isBefore(breakStartTime) && time.isBefore(breakEndTime)) {
                return false;
            }
        }

        return true;
    }
}
