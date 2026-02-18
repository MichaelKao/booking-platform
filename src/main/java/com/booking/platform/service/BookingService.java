package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateBookingRequest;
import com.booking.platform.dto.request.UpdateBookingRequest;
import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.entity.staff.StaffSchedule;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.entity.staff.StaffLeave;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.mapper.BookingMapper;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.StaffLeaveRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.StaffScheduleRepository;
import com.booking.platform.repository.TenantFeatureRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.line.LineNotificationService;
import com.booking.platform.service.notification.SseNotificationService;
import com.booking.platform.enums.FeatureCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffLeaveRepository staffLeaveRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final BookingMapper bookingMapper;
    private final LineNotificationService lineNotificationService;
    private final SseNotificationService sseNotificationService;
    private final CustomerService customerService;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<BookingResponse> getList(
            BookingStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String staffId,
            String customerId,
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
                tenantId, statusStr, startDate, endDate, staffId, customerId, pageable
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
                        request.getServiceItemId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                ));

        if (!service.isAvailable()) {
            throw new BusinessException(
                    ErrorCode.SERVICE_UNAVAILABLE, "該服務目前無法預約"
            );
        }

        // ========================================
        // 2. 驗證預約日期時間
        // ========================================

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 不能預約過去的日期
        if (request.getBookingDate().isBefore(today)) {
            throw new BusinessException(
                    ErrorCode.SYS_PARAM_ERROR, "無法預約過去的日期"
            );
        }

        // 如果是今天，不能預約已過的時段
        if (request.getBookingDate().equals(today) && request.getStartTime().isBefore(now)) {
            throw new BusinessException(
                    ErrorCode.SYS_PARAM_ERROR, "無法預約已過的時段"
            );
        }

        // ========================================
        // 3. 查詢員工（如果有指定）
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

            // 驗證員工排班
            int dayOfWeek = request.getBookingDate().getDayOfWeek().getValue() % 7; // 轉換為 0=週日, 1=週一...
            StaffSchedule schedule = staffScheduleRepository
                    .findByStaffIdAndDayOfWeek(staff.getId(), tenantId, dayOfWeek)
                    .orElse(null);

            // 如果有排班記錄，檢查是否上班日
            if (schedule != null && !schedule.isWorking()) {
                throw new BusinessException(
                        ErrorCode.STAFF_UNAVAILABLE,
                        "該員工在此日期不上班，請選擇其他員工或日期"
                );
            }

            // 如果有排班記錄，檢查是否在工作時間內
            if (schedule != null && schedule.isWorking()) {
                LocalTime bookingStartTime = request.getStartTime();
                if (schedule.getStartTime() != null && bookingStartTime.isBefore(schedule.getStartTime())) {
                    throw new BusinessException(
                            ErrorCode.STAFF_UNAVAILABLE,
                            String.format("該員工 %s 後才開始上班", schedule.getStartTime())
                    );
                }
                if (schedule.getEndTime() != null && bookingStartTime.isAfter(schedule.getEndTime().minusMinutes(30))) {
                    throw new BusinessException(
                            ErrorCode.STAFF_UNAVAILABLE,
                            String.format("該員工 %s 前結束工作", schedule.getEndTime())
                    );
                }
            }
        }

        // ========================================
        // 4. 查詢顧客（如果有指定）
        // ========================================

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.getCustomerId(), tenantId)
                    .orElse(null);
        }

        // ========================================
        // 4. 計算結束時間（含緩衝時間）
        // ========================================

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(service.getTotalDuration());

        // 取得租戶設定的緩衝時間
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId).orElse(null);
        int bufferMinutes = (tenant != null && tenant.getBookingBufferMinutes() != null)
                ? tenant.getBookingBufferMinutes() : 0;

        // 加入緩衝時間（用於衝突檢查）
        LocalTime endTimeWithBuffer = endTime.plusMinutes(bufferMinutes);

        // ========================================
        // 5. 檢查員工請假（含半天假）
        // ========================================

        if (staff != null) {
            // 檢查全天請假
            if (staffLeaveRepository.isStaffOnLeave(staff.getId(), request.getBookingDate())) {
                throw new BusinessException(
                        ErrorCode.STAFF_UNAVAILABLE,
                        "該員工當天請假，請選擇其他員工或日期"
                );
            }

            // 檢查半天假是否涵蓋預約時段
            StaffLeave leave = staffLeaveRepository.findLeaveDetail(staff.getId(), request.getBookingDate())
                    .orElse(null);

            if (leave != null && !leave.getIsFullDay()) {
                // 檢查預約時段是否與半天假時段重疊
                String leaveStartStr = leave.getStartTime();
                String leaveEndStr = leave.getEndTime();

                if (leaveStartStr != null && leaveEndStr != null) {
                    // 解析請假時間字串 (HH:mm 格式)
                    LocalTime leaveStart = LocalTime.parse(leaveStartStr);
                    LocalTime leaveEnd = LocalTime.parse(leaveEndStr);

                    // 預約時段與請假時段重疊檢查
                    if (startTime.isBefore(leaveEnd) && endTime.isAfter(leaveStart)) {
                        throw new BusinessException(
                                ErrorCode.STAFF_UNAVAILABLE,
                                String.format("該員工 %s 至 %s 請假，請選擇其他時段", leaveStartStr, leaveEndStr)
                        );
                    }
                }
            }
        }

        // ========================================
        // 6. 檢查時段衝突 / 自動分配員工
        // ========================================

        if (staff != null) {
            // 有指定員工：檢查衝突
            boolean hasConflict = bookingRepository.existsConflictingBooking(
                    tenantId,
                    staff.getId(),
                    request.getBookingDate(),
                    startTime,
                    endTimeWithBuffer
            );

            if (hasConflict) {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段已被預約，請選擇其他時間"
                );
            }
        } else {
            // 未指定員工：從可用員工中自動分配一位
            staff = findAvailableStaffForSlot(
                    tenantId, request.getBookingDate(), startTime, endTimeWithBuffer
            );
            if (staff == null) {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段所有服務人員皆已被預約，請選擇其他時間"
                );
            }
            log.info("自動分配員工：{}（{}）", staff.getEffectiveDisplayName(), staff.getId());
        }

        // ========================================
        // 7. 建立預約
        // ========================================

        // 產生取消 Token（用於顧客自助取消）
        String cancelToken = UUID.randomUUID().toString();

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
                .cancelToken(cancelToken)
                .build();

        entity.setTenantId(tenantId);
        entity = bookingRepository.save(entity);

        log.info("預約建立成功，ID：{}", entity.getId());

        // ========================================
        // 8. 推送 SSE 通知到後台
        // ========================================
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyNewBooking(tenantId, response);

        return response;
    }

    /**
     * 更新預約
     *
     * @param id      預約 ID
     * @param request 更新請求
     * @return 更新後的預約
     */
    @Transactional
    public BookingResponse update(String id, UpdateBookingRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新預約，ID：{}，參數：{}", id, request);

        // ========================================
        // 1. 查詢預約
        // ========================================

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        // ========================================
        // 2. 檢查是否可修改
        // ========================================

        if (!entity.isModifiable()) {
            throw new BusinessException(
                    ErrorCode.BOOKING_CANNOT_MODIFY,
                    "此預約狀態無法修改"
            );
        }

        // 記錄原始資料（用於通知）
        LocalDate originalDate = entity.getBookingDate();
        LocalTime originalTime = entity.getStartTime();
        String originalStaffName = entity.getStaffName();
        boolean hasTimeChange = false;

        // ========================================
        // 3. 更新服務項目（如有指定）
        // ========================================

        if (request.getServiceItemId() != null) {
            ServiceItem service = serviceItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                            request.getServiceItemId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                    ));

            if (!service.isAvailable()) {
                throw new BusinessException(
                        ErrorCode.SERVICE_UNAVAILABLE, "該服務目前無法預約"
                );
            }

            entity.setServiceId(service.getId());
            entity.setServiceName(service.getName());
            entity.setPrice(service.getPrice());
            entity.setDuration(service.getDuration());
        }

        // ========================================
        // 4. 更新員工（如有指定）
        // ========================================

        if (request.getStaffId() != null) {
            if (request.getStaffId().isEmpty()) {
                // 空字串表示取消指定員工
                entity.setStaffId(null);
                entity.setStaffName(null);
            } else {
                Staff staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                                request.getStaffId(), tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                        ));

                if (!staff.isAvailable()) {
                    throw new BusinessException(
                            ErrorCode.STAFF_UNAVAILABLE, "該員工目前無法預約"
                    );
                }

                entity.setStaffId(staff.getId());
                entity.setStaffName(staff.getEffectiveDisplayName());
            }
        }

        // ========================================
        // 5. 更新自訂服務時長（如有指定）
        // ========================================

        if (request.getDuration() != null && request.getDuration() > 0) {
            entity.setDuration(request.getDuration());
        }

        // ========================================
        // 6. 更新日期時間
        // ========================================

        if (request.getBookingDate() != null) {
            entity.setBookingDate(request.getBookingDate());
            hasTimeChange = true;
        }

        if (request.getStartTime() != null) {
            entity.setStartTime(request.getStartTime());
            hasTimeChange = true;
        }

        // 如果有直接指定結束時間，驗證開始時間必須早於結束時間
        if (request.getEndTime() != null) {
            LocalTime effectiveStart = request.getStartTime() != null ? request.getStartTime() : entity.getStartTime();
            if (!effectiveStart.isBefore(request.getEndTime())) {
                throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                        "預約開始時間必須早於結束時間");
            }
            entity.setEndTime(request.getEndTime());
            hasTimeChange = true;
        }

        // 重新計算結束時間（使用目前的 duration，僅在沒有直接指定 endTime 時）
        if (request.getEndTime() == null && (hasTimeChange || request.getDuration() != null)) {
            entity.setEndTime(entity.getStartTime().plusMinutes(entity.getDuration()));
        }

        // ========================================
        // 6. 檢查員工請假（含半天假）
        // ========================================

        if (entity.getStaffId() != null && hasTimeChange) {
            // 檢查全天請假
            if (staffLeaveRepository.isStaffOnLeave(entity.getStaffId(), entity.getBookingDate())) {
                throw new BusinessException(
                        ErrorCode.STAFF_UNAVAILABLE,
                        "該員工當天請假，請選擇其他員工或日期"
                );
            }

            // 檢查半天假是否涵蓋預約時段
            StaffLeave leave = staffLeaveRepository.findLeaveDetail(entity.getStaffId(), entity.getBookingDate())
                    .orElse(null);

            if (leave != null && !leave.getIsFullDay()) {
                String leaveStartStr = leave.getStartTime();
                String leaveEndStr = leave.getEndTime();

                if (leaveStartStr != null && leaveEndStr != null) {
                    LocalTime leaveStart = LocalTime.parse(leaveStartStr);
                    LocalTime leaveEnd = LocalTime.parse(leaveEndStr);

                    if (entity.getStartTime().isBefore(leaveEnd) && entity.getEndTime().isAfter(leaveStart)) {
                        throw new BusinessException(
                                ErrorCode.STAFF_UNAVAILABLE,
                                String.format("該員工 %s 至 %s 請假，請選擇其他時段", leaveStartStr, leaveEndStr)
                        );
                    }
                }
            }
        }

        // ========================================
        // 7. 檢查時段衝突（含緩衝時間）
        // ========================================

        if (entity.getStaffId() != null && hasTimeChange) {
            // 取得租戶設定的緩衝時間
            Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId).orElse(null);
            int bufferMinutes = (tenant != null && tenant.getBookingBufferMinutes() != null)
                    ? tenant.getBookingBufferMinutes() : 0;

            LocalTime endTimeWithBuffer = entity.getEndTime().plusMinutes(bufferMinutes);

            boolean hasConflict = bookingRepository.existsConflictingBookingExcluding(
                    tenantId,
                    entity.getStaffId(),
                    entity.getBookingDate(),
                    entity.getStartTime(),
                    endTimeWithBuffer,
                    id
            );

            if (hasConflict) {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段已被預約，請選擇其他時間"
                );
            }
        }

        // ========================================
        // 8. 更新備註
        // ========================================

        if (request.getCustomerNote() != null) {
            entity.setCustomerNote(request.getCustomerNote());
        }

        if (request.getInternalNote() != null) {
            entity.setInternalNote(request.getInternalNote());
        }

        if (request.getStoreNoteToCustomer() != null) {
            entity.setStoreNoteToCustomer(request.getStoreNoteToCustomer());
        }

        // ========================================
        // 9. 記錄修改資訊
        // ========================================

        entity.setLastModifiedAt(LocalDateTime.now());
        // 從 Security Context 取得當前用戶 ID
        entity.setLastModifiedBy(getCurrentUserId());

        // ========================================
        // 10. 儲存更新
        // ========================================

        entity = bookingRepository.save(entity);

        log.info("預約更新成功，ID：{}", entity.getId());

        // ========================================
        // 11. 發送 LINE 修改通知
        // ========================================

        // 建構變更描述
        StringBuilder changeDescription = new StringBuilder();
        if (hasTimeChange) {
            changeDescription.append("預約時間已變更\n");
            changeDescription.append("原：").append(originalDate).append(" ").append(originalTime).append("\n");
            changeDescription.append("新：").append(entity.getBookingDate()).append(" ").append(entity.getStartTime());
        }
        if (entity.getStaffName() != null && !entity.getStaffName().equals(originalStaffName)) {
            if (changeDescription.length() > 0) {
                changeDescription.append("\n");
            }
            changeDescription.append("服務人員已變更為：").append(entity.getStaffName());
        }
        if (entity.getStoreNoteToCustomer() != null && !entity.getStoreNoteToCustomer().isEmpty()) {
            if (changeDescription.length() > 0) {
                changeDescription.append("\n");
            }
            changeDescription.append("店家備註：").append(entity.getStoreNoteToCustomer());
        }

        if (changeDescription.length() > 0) {
            lineNotificationService.sendBookingModificationNotification(entity, changeDescription.toString());
        }

        // ========================================
        // 12. 推送 SSE 通知到後台
        // ========================================
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyBookingUpdated(tenantId, response);

        return response;
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

        // 確認時驗證員工可用性（CONFIRMED 才佔用時段，所以確認是真正的驗證關卡）
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId).orElse(null);
        int bufferMinutes = (tenant != null && tenant.getBookingBufferMinutes() != null)
                ? tenant.getBookingBufferMinutes() : 0;
        LocalTime endTimeWithBuffer = entity.getEndTime().plusMinutes(bufferMinutes);

        if (entity.getStaffId() != null) {
            // 已指定員工：檢查 CONFIRMED 衝突
            boolean hasConflict = bookingRepository.existsConflictingBooking(
                    tenantId, entity.getStaffId(), entity.getBookingDate(),
                    entity.getStartTime(), endTimeWithBuffer
            );
            if (hasConflict) {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段員工已有其他已確認的預約，無法確認"
                );
            }
        } else {
            // 未指定員工：自動分配可用員工
            Staff autoAssigned = findAvailableStaffForSlot(
                    tenantId, entity.getBookingDate(), entity.getStartTime(), endTimeWithBuffer
            );
            if (autoAssigned != null) {
                entity.setStaffId(autoAssigned.getId());
                entity.setStaffName(autoAssigned.getEffectiveDisplayName());
                log.info("確認預約時自動分配員工：{}（{}）", autoAssigned.getEffectiveDisplayName(), autoAssigned.getId());
            } else {
                throw new BusinessException(
                        ErrorCode.BOOKING_TIME_CONFLICT,
                        "該時段所有服務人員皆已被預約，無法確認"
                );
            }
        }

        entity.confirm();
        entity = bookingRepository.save(entity);

        log.info("預約確認成功，ID：{}", entity.getId());

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.CONFIRMED, null);

        // 推送 SSE 通知到後台
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyBookingStatusChanged(tenantId, response, "CONFIRMED");

        return response;
    }

    @Transactional
    public BookingResponse complete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("完成預約，ID：{}", id);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        // 只有已確認的預約可以標記為完成
        if (!BookingStatus.CONFIRMED.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR, "只有已確認的預約可以標記為完成");
        }

        entity.complete();
        entity = bookingRepository.save(entity);

        log.info("預約完成，ID：{}", entity.getId());

        // ========================================
        // 更新顧客統計（到訪次數、消費金額）
        // ========================================
        if (entity.getCustomerId() != null && entity.getPrice() != null) {
            try {
                var customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                        entity.getCustomerId(), tenantId
                ).orElse(null);
                if (customer != null) {
                    customer.addVisit(entity.getPrice());
                    customerRepository.save(customer);
                    log.debug("更新顧客統計，顧客 ID：{}，消費：{}", entity.getCustomerId(), entity.getPrice());
                }
            } catch (Exception e) {
                log.warn("更新顧客統計失敗，顧客 ID：{}，錯誤：{}", entity.getCustomerId(), e.getMessage());
            }
        }

        // ========================================
        // 自動集點（POINT_SYSTEM 功能）
        // ========================================
        if (entity.getCustomerId() != null) {
            boolean hasPointSystem = tenantFeatureRepository
                    .findByTenantIdAndFeatureCodeAndDeletedAtIsNull(tenantId, FeatureCode.POINT_SYSTEM)
                    .map(tf -> tf.isEffective())
                    .orElse(false);

            if (hasPointSystem && entity.getPrice() != null) {
                // 取得店家點數累積設定
                var tenantOpt = tenantRepository.findByIdAndDeletedAtIsNull(tenantId);
                if (tenantOpt.isPresent()) {
                    var tenant = tenantOpt.get();

                    // 檢查是否啟用點數累積
                    boolean pointEarnEnabled = tenant.getPointEarnEnabled() != null ? tenant.getPointEarnEnabled() : true;
                    int pointEarnRate = tenant.getPointEarnRate() != null ? tenant.getPointEarnRate() : 10;
                    String pointRoundMode = tenant.getPointRoundMode() != null ? tenant.getPointRoundMode() : "FLOOR";

                    if (pointEarnEnabled && pointEarnRate > 0) {
                        // 根據設定計算點數
                        double rawPoints = entity.getPrice().doubleValue() / pointEarnRate;
                        int earnedPoints;

                        // 根據取整方式計算
                        switch (pointRoundMode) {
                            case "ROUND" -> earnedPoints = (int) Math.round(rawPoints);
                            case "CEIL" -> earnedPoints = (int) Math.ceil(rawPoints);
                            default -> earnedPoints = (int) Math.floor(rawPoints); // FLOOR
                        }

                        if (earnedPoints > 0) {
                            try {
                                customerService.addPoints(
                                        entity.getCustomerId(),
                                        earnedPoints,
                                        "預約完成自動集點 - " + entity.getServiceName()
                                );
                                log.info("自動集點成功，顧客 ID：{}，點數：{}（比例：每 {} 元得 1 點，取整：{}）",
                                        entity.getCustomerId(), earnedPoints, pointEarnRate, pointRoundMode);
                            } catch (Exception e) {
                                log.warn("自動集點失敗，顧客 ID：{}，錯誤：{}", entity.getCustomerId(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.COMPLETED, "感謝您的光臨，期待下次再見！");

        // 推送 SSE 通知到後台
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyBookingStatusChanged(tenantId, response, "COMPLETED");

        return response;
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

        // 推送 SSE 通知到後台
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyBookingCancelled(tenantId, response);

        return response;
    }

    @Transactional
    public BookingResponse markNoShow(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("標記爽約，ID：{}", id);

        Booking entity = bookingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到指定的預約"
                ));

        // 只有已確認的預約可以標記為爽約
        if (!BookingStatus.CONFIRMED.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR, "只有已確認的預約可以標記為爽約");
        }

        entity.markNoShow();
        entity = bookingRepository.save(entity);

        log.info("已標記爽約，ID：{}", entity.getId());

        // 更新顧客爽約次數
        if (entity.getCustomerId() != null) {
            try {
                var customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                        entity.getCustomerId(), tenantId
                ).orElse(null);
                if (customer != null) {
                    int current = customer.getNoShowCount() != null ? customer.getNoShowCount() : 0;
                    customer.setNoShowCount(current + 1);
                    customerRepository.save(customer);
                }
            } catch (Exception e) {
                log.warn("更新顧客爽約次數失敗，顧客 ID：{}，錯誤：{}", entity.getCustomerId(), e.getMessage());
            }
        }

        // 發送 LINE 通知
        lineNotificationService.sendBookingStatusNotification(entity, BookingStatus.NO_SHOW, null);

        // 推送 SSE 通知到後台
        BookingResponse response = bookingMapper.toResponse(entity);
        sseNotificationService.notifyBookingStatusChanged(tenantId, response, "NO_SHOW");

        return response;
    }

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 從 Security Context 取得當前用戶 ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return TenantContext.getTenantId();
    }

    /**
     * 從可用員工中找出該時段沒有衝突的員工（隨機選一位）
     *
     * @param tenantId          租戶 ID
     * @param date              預約日期
     * @param startTime         開始時間
     * @param endTimeWithBuffer 結束時間（含緩衝）
     * @return 可用員工，若全部滿了則返回 null
     */
    private Staff findAvailableStaffForSlot(String tenantId, LocalDate date,
                                            LocalTime startTime, LocalTime endTimeWithBuffer) {
        // 取得所有活躍員工
        List<Staff> allStaff = staffRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        List<Staff> candidates = new ArrayList<>();

        for (Staff s : allStaff) {
            // 檢查排班
            Optional<StaffSchedule> scheduleOpt = staffScheduleRepository
                    .findByStaffIdAndDayOfWeek(s.getId(), tenantId, dayOfWeek);

            boolean isWorkingDay = scheduleOpt.isEmpty() ||
                    Boolean.TRUE.equals(scheduleOpt.get().getIsWorkingDay());
            if (!isWorkingDay) continue;

            // 檢查全天請假
            if (staffLeaveRepository.isStaffOnLeave(s.getId(), date)) continue;

            // 檢查半天假時段重疊
            Optional<StaffLeave> leaveOpt = staffLeaveRepository.findLeaveDetail(s.getId(), date);
            if (leaveOpt.isPresent() && !leaveOpt.get().getIsFullDay()) {
                StaffLeave leave = leaveOpt.get();
                if (leave.getStartTime() != null && leave.getEndTime() != null) {
                    LocalTime leaveStart = LocalTime.parse(leave.getStartTime());
                    LocalTime leaveEnd = LocalTime.parse(leave.getEndTime());
                    if (startTime.isBefore(leaveEnd) && endTimeWithBuffer.isAfter(leaveStart)) {
                        continue;
                    }
                }
            }

            // 檢查預約衝突
            boolean hasConflict = bookingRepository.existsConflictingBooking(
                    tenantId, s.getId(), date, startTime, endTimeWithBuffer
            );
            if (!hasConflict) {
                candidates.add(s);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // 隨機選一位
        Collections.shuffle(candidates);
        return candidates.get(0);
    }
}
