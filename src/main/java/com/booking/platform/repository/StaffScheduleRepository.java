package com.booking.platform.repository;

import com.booking.platform.entity.staff.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 員工排班 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, String> {

    /**
     * 查詢員工的所有排班
     */
    @Query("""
            SELECT s FROM StaffSchedule s
            WHERE s.staffId = :staffId
            AND s.tenantId = :tenantId
            AND s.deletedAt IS NULL
            ORDER BY s.dayOfWeek ASC
            """)
    List<StaffSchedule> findByStaffIdAndTenantId(
            @Param("staffId") String staffId,
            @Param("tenantId") String tenantId
    );

    /**
     * 查詢員工特定日期的排班
     */
    @Query("""
            SELECT s FROM StaffSchedule s
            WHERE s.staffId = :staffId
            AND s.tenantId = :tenantId
            AND s.dayOfWeek = :dayOfWeek
            AND s.deletedAt IS NULL
            """)
    Optional<StaffSchedule> findByStaffIdAndDayOfWeek(
            @Param("staffId") String staffId,
            @Param("tenantId") String tenantId,
            @Param("dayOfWeek") Integer dayOfWeek
    );

    /**
     * 刪除員工的所有排班（軟刪除前使用硬刪除清空）
     */
    @Modifying
    @Query("""
            DELETE FROM StaffSchedule s
            WHERE s.staffId = :staffId
            AND s.tenantId = :tenantId
            """)
    void deleteByStaffIdAndTenantId(
            @Param("staffId") String staffId,
            @Param("tenantId") String tenantId
    );

    /**
     * 查詢所有可工作員工在特定日期的排班
     */
    @Query("""
            SELECT s FROM StaffSchedule s
            WHERE s.tenantId = :tenantId
            AND s.dayOfWeek = :dayOfWeek
            AND s.isWorkingDay = true
            AND s.deletedAt IS NULL
            """)
    List<StaffSchedule> findWorkingStaffByDayOfWeek(
            @Param("tenantId") String tenantId,
            @Param("dayOfWeek") Integer dayOfWeek
    );

    /**
     * 查詢員工的所有排班（不需 tenantId）
     */
    @Query("""
            SELECT s FROM StaffSchedule s
            WHERE s.staffId = :staffId
            AND s.deletedAt IS NULL
            ORDER BY s.dayOfWeek ASC
            """)
    List<StaffSchedule> findByStaffIdAndDeletedAtIsNull(@Param("staffId") String staffId);
}
