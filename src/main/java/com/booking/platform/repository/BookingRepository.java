package com.booking.platform.repository;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
            AND (CAST(:startDate AS DATE) IS NULL OR b.booking_date >= CAST(:startDate AS DATE))
            AND (CAST(:endDate AS DATE) IS NULL OR b.booking_date <= CAST(:endDate AS DATE))
            AND (CAST(:staffId AS VARCHAR) IS NULL OR b.staff_id = CAST(:staffId AS VARCHAR))
            AND (CAST(:customerId AS VARCHAR) IS NULL OR b.customer_id = CAST(:customerId AS VARCHAR))
            ORDER BY b.booking_date DESC, b.start_time ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND (CAST(:status AS VARCHAR) IS NULL OR b.status = CAST(:status AS VARCHAR))
            AND (CAST(:startDate AS DATE) IS NULL OR b.booking_date >= CAST(:startDate AS DATE))
            AND (CAST(:endDate AS DATE) IS NULL OR b.booking_date <= CAST(:endDate AS DATE))
            AND (CAST(:staffId AS VARCHAR) IS NULL OR b.staff_id = CAST(:staffId AS VARCHAR))
            AND (CAST(:customerId AS VARCHAR) IS NULL OR b.customer_id = CAST(:customerId AS VARCHAR))
            """,
            nativeQuery = true)
    Page<Booking> findByTenantIdAndFilters(
            @Param("tenantId") String tenantId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("staffId") String staffId,
            @Param("customerId") String customerId,
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

    /**
     * 查詢顧客的有效預約（待確認 + 已確認）
     * 用於 LINE Bot「我的預約」功能，依日期時間 ASC 排序（最近的排前面）
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.customerId = :customerId
            AND b.deletedAt IS NULL
            AND b.status IN ('PENDING', 'CONFIRMED')
            ORDER BY b.bookingDate ASC, b.startTime ASC
            """)
    List<Booking> findActiveByCustomerId(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId
    );

    // ========================================
    // 時段衝突檢查
    // ========================================

    /**
     * 批次取得指定日期範圍內所有 CONFIRMED 預約（用於日期選單快取）
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.bookingDate BETWEEN :startDate AND :endDate
            AND b.deletedAt IS NULL
            AND b.status = 'CONFIRMED'
            """)
    List<Booking> findConfirmedBookingsInDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 檢查時段是否有衝突（僅檢查已確認的預約）
     * PENDING 不佔用時段，只有 CONFIRMED 才算衝突
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.bookingDate = :date
            AND b.deletedAt IS NULL
            AND b.status = 'CONFIRMED'
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
     * 檢查時段是否有衝突（排除指定預約，僅檢查已確認的預約）
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.bookingDate = :date
            AND b.id != :excludeId
            AND b.deletedAt IS NULL
            AND b.status = 'CONFIRMED'
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
     * 統計日期區間內的預約數（依預約日期）
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 統計日期區間內特定狀態的預約數（依預約日期）
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.status = :status
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countByTenantIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
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
            AND b.service_id IS NOT NULL
            AND b.status = 'COMPLETED'
            AND b.booking_date BETWEEN :startDate AND :endDate
            GROUP BY b.service_id, s.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopServicesByTenantId(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
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
            AND b.staff_id IS NOT NULL
            AND b.status = 'COMPLETED'
            AND b.booking_date BETWEEN :startDate AND :endDate
            GROUP BY b.staff_id, st.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopStaffByTenantId(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
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

    // ========================================
    // 進階報表查詢
    // ========================================

    /**
     * 統計不重複顧客數（進階報表用，依預約日期）
     */
    @Query("""
            SELECT COUNT(DISTINCT b.customerId) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.customerId IS NOT NULL
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countDistinctCustomersByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 統計指定時段的預約數（尖峰時段分析用，依預約日期）
     */
    @Query(value = """
            SELECT COUNT(*) FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND b.status = 'COMPLETED'
            AND EXTRACT(HOUR FROM b.start_time) = :hour
            AND b.booking_date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    long countByTenantIdAndHour(
            @Param("tenantId") String tenantId,
            @Param("hour") int hour,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========================================
    // 營收統計查詢
    // ========================================

    /**
     * 計算指定狀態的預約總營收（只計算已完成的預約）
     */
    @Query("""
            SELECT COALESCE(SUM(b.price), 0) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.status = :status
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumRevenueByTenantIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 計算指定日期已完成預約的營收
     */
    @Query("""
            SELECT COALESCE(SUM(b.price), 0) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.status = 'COMPLETED'
            AND b.bookingDate = :date
            """)
    BigDecimal sumRevenueByTenantIdAndDate(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date
    );

    /**
     * 統計回客數（在指定時間範圍內有預約，且在此之前也有過預約的顧客數）
     */
    @Query(value = """
            SELECT COUNT(DISTINCT b.customer_id)
            FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.deleted_at IS NULL
            AND b.customer_id IS NOT NULL
            AND b.booking_date BETWEEN :startDate AND :endDate
            AND b.customer_id IN (
                SELECT DISTINCT b2.customer_id
                FROM bookings b2
                WHERE b2.tenant_id = :tenantId
                AND b2.deleted_at IS NULL
                AND b2.customer_id IS NOT NULL
                AND b2.booking_date < :startDate
            )
            """, nativeQuery = true)
    long countReturningCustomersByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 計算服務營收（熱門服務報表用）
     */
    @Query(value = """
            SELECT COALESCE(SUM(b.price), 0)
            FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.service_id = :serviceId
            AND b.deleted_at IS NULL
            AND b.status = 'COMPLETED'
            AND b.booking_date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    BigDecimal sumRevenueByTenantIdAndServiceIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("serviceId") String serviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 計算員工營收（員工業績報表用）
     */
    @Query(value = """
            SELECT COALESCE(SUM(b.price), 0)
            FROM bookings b
            WHERE b.tenant_id = :tenantId
            AND b.staff_id = :staffId
            AND b.deleted_at IS NULL
            AND b.status = 'COMPLETED'
            AND b.booking_date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    BigDecimal sumRevenueByTenantIdAndStaffIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("staffId") String staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 統計特定狀態的預約數（不限日期）
     */
    long countByTenantIdAndStatusAndDeletedAtIsNull(String tenantId, BookingStatus status);

    /**
     * 統計特定狀態的不重複顧客數（依預約日期）
     */
    @Query("""
            SELECT COUNT(DISTINCT b.customerId) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.customerId IS NOT NULL
            AND b.status = :status
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countDistinctCustomersByTenantIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 計算平均來客週期（天）：有多次預約的顧客，平均兩次預約之間的天數
     */
    @Query(value = """
            SELECT AVG(avg_interval) FROM (
                SELECT customer_id,
                       AVG(next_date - booking_date) as avg_interval
                FROM (
                    SELECT customer_id, booking_date,
                           LEAD(booking_date) OVER (PARTITION BY customer_id ORDER BY booking_date) as next_date
                    FROM bookings
                    WHERE tenant_id = :tenantId
                    AND deleted_at IS NULL
                    AND status = 'COMPLETED'
                    AND customer_id IS NOT NULL
                    AND booking_date BETWEEN :startDate AND :endDate
                ) sub
                WHERE next_date IS NOT NULL
                GROUP BY customer_id
            ) avg_sub
            """, nativeQuery = true)
    BigDecimal avgVisitIntervalByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 統計特定服務在指定狀態和日期範圍內的預約數（服務趨勢用）
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND b.serviceId = :serviceId
            AND b.status = :status
            AND b.bookingDate BETWEEN :startDate AND :endDate
            """)
    long countByTenantIdAndServiceIdAndStatusAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("serviceId") String serviceId,
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========================================
    // 存在性檢查
    // ========================================

    boolean existsByTenantIdAndDeletedAtIsNull(String tenantId);
}
