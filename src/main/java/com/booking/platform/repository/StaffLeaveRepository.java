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
}
