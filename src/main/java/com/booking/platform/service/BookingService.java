package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateBookingRequest;
import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.mapper.BookingMapper;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.service.line.LineNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 預約服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final BookingMapper bookingMapper;
    private final LineNotificationService lineNotificationService;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<BookingResponse> getList(
            BookingStatus status,
            LocalDate date,
            String staffId,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        // 如果沒有租戶上下文，返回空結果
        if (tenantId == null) {
            log.warn("查詢預約列表時沒有租戶上下文");
            return PageResponse.<BookingResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(pageable.getPageSize())
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        // 將 status 轉為字串供原生查詢使用
        String statusStr = status != null ? status.name() : null;

        Page<Booking> page = bookingRepository.findByTenantIdAndFilters(
                tenantId, statusStr, date, staffId, pageable
        );

        List<BookingResponse> content = page.getContent().stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookingResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public BookingResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        return bookingMapper.toResponse(entity);
    }

    public List<BookingResponse> getByStaffAndDate(String staffId, LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        return bookingRepository.findByStaffAndDate(tenantId, staffId, date).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得行事曆資料（日期區間）
     */
    public List<BookingResponse> getCalendarData(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        return bookingRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立預約，租戶：{}，參數：{}", tenantId, request);

        // ========================================
        // 1. 查詢服務項目
        // ========================================

        ServiceItem service = serviceItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                        request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                ));

        if (!service.isAvailable()) {
            throw new BusinessException(
                    ErrorCode.SERVICE_UNAVAILABLE, "該服務目前無法預約"
            );
        }

        // ========================================
        // 2. 查詢員工（如果有指定）
        // ========================================

        Staff staff = null;
        if (request.getStaffId() != null) {
            staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.getStaffId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                    ));

            if (!staff.isAvailable()) {
                throw new BusinessException(
                        ErrorCode.STAFF_UNAVAILABLE, "該員工目前無法預約"
                );
            }
        }

        // ========================================
        // 3. 查詢顧客（如果有指定）
        // ========================================

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.getCustomerId(), tenantId)
                    .orElse(null);
        }

        // ========================================
        // 4. 計算結束時間
        // ========================================

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(service.getTotalDuration());

        // ========================================
        // 5. 檢查時段衝突
        // ========================================

        if (staff != null) {
            boolean hasConflict = bookingRepository.existsConflictingBooking(
                    tenantId,
                    staff.getId(),
                    request.getBookingDate(),
                    startTime,
                    endTime
            );

            if (hasConflict) {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段已被預約，請選擇其他時間"
                );
            }
        }

        // ========================================
        // 6. 建立預約
        // ========================================

        Booking entity = Booking.builder()
                .bookingDate(request.getBookingDate())
                .startTime(startTime)
                .endTime(endTime)
                .customerId(request.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .staffId(staff != null ? staff.getId() : null)
                .staffName(staff != null ? staff.getEffectiveDisplayName() : null)
                .serviceId(service.getId())
                .serviceName(service.getName())
                .price(service.getPrice())
                .duration(service.getDuration())
                .status(BookingStatus.PENDING)
                .customerNote(request.getCustomerNote())
                .source(request.getSource() != null ? request.getSource() : "LINE")
                .build();

        entity.setTenantId(tenantId);
        entity = bookingRepository.save(entity);

        log.info("預約建立成功，ID：{}", entity.getId());

        return bookingMapper.toResponse(entity);
    }

    // ========================================
    // 狀態變更
    // ========================================

    @Transactional
    public BookingResponse confirm(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("確認預約，ID：{}", id);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        if (!BookingStatus.PENDING.equals(entity.getStatus())) {
            throw new BusinessException(
                    ErrorCode.BOOKING_STATUS_ERROR,
                    "只有待確認的預約可以確認"
            );
        }

        entity.confirm();
        entity = bookingRepository.save(entity);

        log.info("預約確認成功，ID：{}", entity.getId());

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.CONFIRMED, null);

        return bookingMapper.toResponse(entity);
    }

    @Transactional
    public BookingResponse complete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("完成預約，ID：{}", id);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        entity.complete();
        entity = bookingRepository.save(entity);

        log.info("預約完成，ID：{}", entity.getId());

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.COMPLETED, "感謝您的光臨，期待下次再見！");

        return bookingMapper.toResponse(entity);
    }

    @Transactional
    public BookingResponse cancel(String id, String reason) {
        String tenantId = TenantContext.getTenantId();

        log.info("取消預約，ID：{}，原因：{}", id, reason);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        if (!entity.isCancellable()) {
            throw new BusinessException(
                    ErrorCode.BOOKING_CANNOT_CANCEL,
                    "此預約狀態無法取消"
            );
        }

        entity.cancel(reason);
        entity = bookingRepository.save(entity);

        log.info("預約取消成功，ID：{}", entity.getId());

        // 發送 LINE 通知
        String message = reason != null ? "取消原因：" + reason : null;
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.CANCELLED, message);

        return bookingMapper.toResponse(entity);
    }

    @Transactional
    public BookingResponse markNoShow(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("標記爽約，ID：{}", id);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        entity.markNoShow();
        entity = bookingRepository.save(entity);

        log.info("已標記爽約，ID：{}", entity.getId());

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.NO_SHOW, null);

        return bookingMapper.toResponse(entity);
    }
}
