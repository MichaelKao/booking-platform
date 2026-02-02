package com.booking.platform.repository;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 預約 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // ========================================
    // 基本查詢
    // ========================================

    Optional<Booking> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);

    // ========================================
    // 列表查詢
    // ========================================

    @Query(value = """
            SELECT * FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND (CAST(:status AS VARCHAR) IS NULL OR b.status = CAST(:status AS VARCHAR))
            AND (CAST(:date AS DATE) IS NULL OR b.booking_date = CAST(:date AS DATE))
            AND (CAST(:staffId AS VARCHAR) IS NULL OR b.staff_id = CAST(:staffId AS VARCHAR))
            ORDER BY b.booking_date DESC, b.start_time ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND (CAST(:status AS VARCHAR) IS NULL OR b.status = CAST(:status AS VARCHAR))
            AND (CAST(:date AS DATE) IS NULL OR b.booking_date = CAST(:date AS DATE))
            AND (CAST(:staffId AS VARCHAR) IS NULL OR b.staff_id = CAST(:staffId AS VARCHAR))
            """,
            nativeQuery = true)
    Page<Booking> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("staffId") String staffId,
            Pageable pageable
    );

    /**
     * 查詢某天某員工的所有預約
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.bookingDate = :date
            AND b.deletedAt IS NULL
            AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
            ORDER BY b.startTime ASC
            """)
    List<Booking> findByStaffAndDate(
            @Param("tenantId") String tenantId,
            @Param("staffId") String staffId,
            @Param("date") LocalDate date
    );

    /**
     * 查詢顧客的預約記錄
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.customerId = :customerId
            AND b.deletedAt IS NULL
            ORDER BY b.bookingDate DESC, b.startTime DESC
            """)
    Page<Booking> findByCustomerId(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            Pageable pageable
    );

    // ========================================
    // 時段衝突檢查
    // ========================================

    /**
     * 檢查時段是否有衝突
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.bookingDate = :date
            AND b.deletedAt IS NULL
            AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
            AND (
                (b.startTime < :endTime AND b.endTime > :startTime)
            )
            """)
    boolean existsConflictingBooking(
            @Param("tenantId") String tenantId,
            @Param("staffId") String staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * 檢查時段是否有衝突（排除指定預約）
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.bookingDate = :date
            AND b.id != :excludeId
            AND b.deletedAt IS NULL
            AND b.status NOT IN ('CANCELLED', 'NO_SHOW')
            AND (
                (b.startTime < :endTime AND b.endTime > :startTime)
            )
            """)
    boolean existsConflictingBookingExcluding(
            @Param("tenantId") String tenantId,
            @Param("staffId") String staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") String excludeId
    );

    // ========================================
    // 統計查詢
    // ========================================

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.bookingDate = :date
            AND b.deletedAt IS NULL
            AND (:status IS NULL OR b.status = :status)
            """)
    long countByDateAndStatus(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatus status
    );

    /**
     * 統計顧客的預約次數
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.customerId = :customerId
            AND b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            """)
    long countByCustomerIdAndTenantId(
            @Param("customerId") String customerId,
            @Param("tenantId") String tenantId
    );

    // ========================================
    // 報表查詢
    // ========================================

    /**
     * 統計日期區間內的預約數
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 統計日期區間內特定狀態的預約數
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.status = :status
            AND b.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    long countByTenantIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 查詢熱門服務
     */
    @Query(value = """
            SELECT b.service_id, s.name, COUNT(b.id) as cnt
            FROM bookings b
            LEFT JOIN service_items s ON b.service_id = s.id
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND b.status = 'COMPLETED'
            AND b.created_at BETWEEN :startDateTime AND :endDateTime
            GROUP BY b.service_id, s.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopServicesByTenantId(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("limit") int limit
    );

    /**
     * 查詢熱門員工
     */
    @Query(value = """
            SELECT b.staff_id, st.name, COUNT(b.id) as cnt
            FROM bookings b
            LEFT JOIN staffs st ON b.staff_id = st.id
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND b.status = 'COMPLETED'
            AND b.created_at BETWEEN :startDateTime AND :endDateTime
            GROUP BY b.staff_id, st.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopStaffByTenantId(
            @Param("tenantId") String tenantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("limit") int limit
    );

    // ========================================
    // 行事曆查詢
    // ========================================

    /**
     * 查詢日期區間內的預約（行事曆用）
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.bookingDate BETWEEN :startDate AND :endDate
            ORDER BY b.bookingDate ASC, b.startTime ASC
            """)
    List<Booking> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========================================
    // 全平台統計（超級管理員用）
    // ========================================

    /**
     * 統計日期區間內全平台的預約數
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.deletedAt IS NULL
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countByBookingDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========================================
    // 預約提醒查詢
    // ========================================

    /**
     * 查詢需要發送提醒的預約
     *
     * <p>查詢條件：
     * <ul>
     *   <li>預約日期和時間在指定範圍內</li>
     *   <li>狀態為已確認</li>
     *   <li>尚未發送提醒</li>
     * </ul>
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.deletedAt IS NULL
            AND b.status = 'CONFIRMED'
            AND b.reminderSent = false
            AND b.bookingDate = :date
            AND b.startTime BETWEEN :startTime AND :endTime
            """)
    List<Booking> findUpcomingBookingsForReminder(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * 查詢指定租戶需要發送提醒的預約
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.status = 'CONFIRMED'
            AND b.reminderSent = false
            AND b.bookingDate = :date
            AND b.startTime BETWEEN :startTime AND :endTime
            """)
    List<Booking> findUpcomingBookingsForReminderByTenant(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    // ========================================
    // 報表匯出查詢
    // ========================================

    /**
     * 查詢指定條件的預約（報表匯出用）
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND (:status IS NULL OR b.status = :status)
            AND b.bookingDate BETWEEN :startDate AND :endDate
            ORDER BY b.bookingDate ASC, b.startTime ASC
            """)
    List<Booking> findForExport(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") BookingStatus status
    );

    // ========================================
    // 自助取消查詢
    // ========================================

    /**
     * 依取消 Token 查詢預約
     */
    Optional<Booking> findByCancelTokenAndDeletedAtIsNull(String cancelToken);
}
