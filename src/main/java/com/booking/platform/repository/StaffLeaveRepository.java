package com.booking.platform.repository;

import com.booking.platform.entity.staff.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 員工請假 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, String> {

    /**
     * 查詢員工特定日期的請假記錄
     */
    Optional<StaffLeave> findByStaffIdAndLeaveDateAndDeletedAtIsNull(String staffId, LocalDate leaveDate);

    /**
     * 查詢員工日期範圍內的請假記錄
     */
    @Query("SELECT sl FROM StaffLeave sl WHERE sl.staffId = :staffId " +
           "AND sl.leaveDate BETWEEN :startDate AND :endDate " +
           "AND sl.deletedAt IS NULL ORDER BY sl.leaveDate")
    List<StaffLeave> findByStaffIdAndDateRange(
            @Param("staffId") String staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查詢租戶下所有員工的請假記錄（日期範圍）
     */
    @Query("SELECT sl FROM StaffLeave sl WHERE sl.tenantId = :tenantId " +
           "AND sl.leaveDate BETWEEN :startDate AND :endDate " +
           "AND sl.deletedAt IS NULL ORDER BY sl.leaveDate")
    List<StaffLeave> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查詢員工所有未來的請假記錄
     */
    @Query("SELECT sl FROM StaffLeave sl WHERE sl.staffId = :staffId " +
           "AND sl.leaveDate >= :today " +
           "AND sl.deletedAt IS NULL ORDER BY sl.leaveDate")
    List<StaffLeave> findFutureLeavesByStaffId(
            @Param("staffId") String staffId,
            @Param("today") LocalDate today);

    /**
     * 檢查員工特定日期是否請假
     */
    @Query("SELECT COUNT(sl) > 0 FROM StaffLeave sl WHERE sl.staffId = :staffId " +
           "AND sl.leaveDate = :date AND sl.isFullDay = true AND sl.deletedAt IS NULL")
    boolean isStaffOnLeave(@Param("staffId") String staffId, @Param("date") LocalDate date);

    /**
     * 刪除員工的請假記錄
     */
    void deleteByStaffIdAndLeaveDateAndDeletedAtIsNull(String staffId, LocalDate leaveDate);

    /**
     * 檢查員工在特定時間是否請假（含半天假）
     *
     * <p>若為全天假，直接返回 true
     * <p>若為半天假，檢查指定時間是否在請假時段內
     * <p>注意：startTime/endTime 為 String 格式 (HH:mm)
     */
    @Query("""
            SELECT COUNT(sl) > 0 FROM StaffLeave sl
            WHERE sl.staffId = :staffId
            AND sl.leaveDate = :date
            AND sl.deletedAt IS NULL
            AND (
                sl.isFullDay = true
                OR (
                    sl.isFullDay = false
                    AND sl.startTime <= :time
                    AND sl.endTime > :time
                )
            )
            """)
    boolean isStaffOnLeaveAtTime(
            @Param("staffId") String staffId,
            @Param("date") LocalDate date,
            @Param("time") String time);

    /**
     * 取得員工特定日期的請假記錄（含詳細資訊）
     */
    @Query("""
            SELECT sl FROM StaffLeave sl
            WHERE sl.staffId = :staffId
            AND sl.leaveDate = :date
            AND sl.deletedAt IS NULL
            """)
    Optional<StaffLeave> findLeaveDetail(
            @Param("staffId") String staffId,
            @Param("date") LocalDate date);
}
