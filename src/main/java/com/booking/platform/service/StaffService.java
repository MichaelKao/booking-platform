package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateStaffLeaveRequest;
import com.booking.platform.dto.request.CreateStaffRequest;
import com.booking.platform.dto.request.StaffScheduleRequest;
import com.booking.platform.dto.response.StaffLeaveResponse;
import com.booking.platform.dto.response.StaffResponse;
import com.booking.platform.dto.response.StaffScheduleCalendarResponse;
import com.booking.platform.dto.response.StaffScheduleResponse;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.entity.staff.StaffLeave;
import com.booking.platform.entity.staff.StaffSchedule;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.mapper.StaffMapper;
import com.booking.platform.repository.StaffLeaveRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.StaffScheduleRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 員工服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffLeaveRepository staffLeaveRepository;
    private final TenantRepository tenantRepository;
    private final StaffMapper staffMapper;

    private static final String[] DAY_OF_WEEK_NAMES = {
            "週日", "週一", "週二", "週三", "週四", "週五", "週六"
    };

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<StaffResponse> getList(StaffStatus status, String keyword, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<Staff> page = staffRepository.findByTenantIdAndFilters(tenantId, status, keyword, pageable);

        List<StaffResponse> content = page.getContent().stream()
                .map(staffMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<StaffResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public StaffResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Staff entity = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        return staffMapper.toResponse(entity);
    }

    public List<StaffResponse> getBookableStaffs() {
        String tenantId = TenantContext.getTenantId();

        return staffRepository.findBookableStaffs(tenantId).stream()
                .map(staffMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public StaffResponse create(CreateStaffRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立員工，租戶：{}，參數：{}", tenantId, request);

        // 檢查員工數量限制
        Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到租戶"
                ));

        long currentCount = staffRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        if (tenant.getMaxStaffCount() > 0 && currentCount >= tenant.getMaxStaffCount()) {
            throw new BusinessException(
                    ErrorCode.STAFF_LIMIT_EXCEEDED,
                    "員工數量已達上限（" + tenant.getMaxStaffCount() + " 位）"
            );
        }

        // 建立員工
        Staff entity = Staff.builder()
                .name(request.getName())
                .displayName(request.getDisplayName())
                .bio(request.getBio())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(StaffStatus.ACTIVE)
                .isBookable(request.getIsBookable() != null ? request.getIsBookable() : true)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        entity.setTenantId(tenantId);
        entity = staffRepository.save(entity);

        log.info("員工建立成功，ID：{}", entity.getId());

        return staffMapper.toResponse(entity);
    }

    @Transactional
    public StaffResponse update(String id, CreateStaffRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新員工，ID：{}，參數：{}", id, request);

        Staff entity = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        entity.setName(request.getName());
        entity.setDisplayName(request.getDisplayName());
        entity.setBio(request.getBio());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());

        if (request.getIsBookable() != null) {
            entity.setIsBookable(request.getIsBookable());
        }
        if (request.getIsVisible() != null) {
            entity.setIsVisible(request.getIsVisible());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }

        entity = staffRepository.save(entity);

        log.info("員工更新成功，ID：{}", entity.getId());

        return staffMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除員工，ID：{}", id);

        Staff entity = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        entity.softDelete();
        staffRepository.save(entity);

        log.info("員工刪除成功，ID：{}", id);
    }

    // ========================================
    // 排班方法
    // ========================================

    /**
     * 取得員工排班
     *
     * @param staffId 員工 ID
     * @return 排班資料
     */
    public StaffScheduleResponse getSchedule(String staffId) {
        String tenantId = TenantContext.getTenantId();

        log.debug("取得員工排班，員工 ID：{}", staffId);

        // 驗證員工存在
        Staff staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 查詢排班
        List<StaffSchedule> schedules = staffScheduleRepository
                .findByStaffIdAndTenantId(staffId, tenantId);

        // 轉換為 Map 方便查找
        Map<Integer, StaffSchedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(StaffSchedule::getDayOfWeek, s -> s));

        // 建立 7 天的排班回應
        List<StaffScheduleResponse.DayScheduleResponse> daySchedules = new ArrayList<>();
        for (int day = 0; day <= 6; day++) {
            StaffSchedule schedule = scheduleMap.get(day);

            StaffScheduleResponse.DayScheduleResponse daySchedule;
            if (schedule != null) {
                daySchedule = StaffScheduleResponse.DayScheduleResponse.builder()
                        .id(schedule.getId())
                        .dayOfWeek(day)
                        .dayOfWeekName(DAY_OF_WEEK_NAMES[day])
                        .isWorkingDay(schedule.getIsWorkingDay())
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .breakStartTime(schedule.getBreakStartTime())
                        .breakEndTime(schedule.getBreakEndTime())
                        .build();
            } else {
                // 預設排班（週一至週五上班，含午休 12:00-13:00）
                boolean isWorkingDay = day >= 1 && day <= 5;
                daySchedule = StaffScheduleResponse.DayScheduleResponse.builder()
                        .dayOfWeek(day)
                        .dayOfWeekName(DAY_OF_WEEK_NAMES[day])
                        .isWorkingDay(isWorkingDay)
                        .startTime(isWorkingDay ? LocalTime.of(9, 0) : null)
                        .endTime(isWorkingDay ? LocalTime.of(18, 0) : null)
                        .breakStartTime(isWorkingDay ? LocalTime.of(12, 0) : null)
                        .breakEndTime(isWorkingDay ? LocalTime.of(13, 0) : null)
                        .build();
            }
            daySchedules.add(daySchedule);
        }

        return StaffScheduleResponse.builder()
                .staffId(staffId)
                .staffName(staff.getEffectiveDisplayName())
                .schedules(daySchedules)
                .build();
    }

    /**
     * 更新員工排班
     *
     * @param staffId 員工 ID
     * @param request 排班請求
     * @return 更新後的排班資料
     */
    @Transactional
    public StaffScheduleResponse updateSchedule(String staffId, StaffScheduleRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新員工排班，員工 ID：{}", staffId);

        // 驗證員工存在
        Staff staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 查詢現有排班
        List<StaffSchedule> existingSchedules = staffScheduleRepository
                .findByStaffIdAndTenantId(staffId, tenantId);
        Map<Integer, StaffSchedule> existingMap = existingSchedules.stream()
                .collect(Collectors.toMap(StaffSchedule::getDayOfWeek, s -> s));

        // 驗證時間邏輯
        for (StaffScheduleRequest.DaySchedule daySchedule : request.getSchedules()) {
            if (Boolean.TRUE.equals(daySchedule.getIsWorkingDay())) {
                // 上班時間驗證：開始時間不能晚於結束時間
                if (daySchedule.getStartTime() != null && daySchedule.getEndTime() != null
                        && !daySchedule.getStartTime().isBefore(daySchedule.getEndTime())) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            DAY_OF_WEEK_NAMES[daySchedule.getDayOfWeek()] + " 的上班開始時間必須早於結束時間");
                }
                // 休息時間必須成對設定
                if ((daySchedule.getBreakStartTime() != null) != (daySchedule.getBreakEndTime() != null)) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            DAY_OF_WEEK_NAMES[daySchedule.getDayOfWeek()] + " 的休息時間必須同時設定開始和結束時間");
                }
                // 休息時間驗證：開始時間不能晚於結束時間
                if (daySchedule.getBreakStartTime() != null && daySchedule.getBreakEndTime() != null
                        && !daySchedule.getBreakStartTime().isBefore(daySchedule.getBreakEndTime())) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            DAY_OF_WEEK_NAMES[daySchedule.getDayOfWeek()] + " 的休息開始時間必須早於結束時間");
                }
                // 休息時間必須在上班時間範圍內
                if (daySchedule.getBreakStartTime() != null && daySchedule.getStartTime() != null
                        && daySchedule.getBreakStartTime().isBefore(daySchedule.getStartTime())) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            DAY_OF_WEEK_NAMES[daySchedule.getDayOfWeek()] + " 的休息時間不能早於上班時間");
                }
                if (daySchedule.getBreakEndTime() != null && daySchedule.getEndTime() != null
                        && daySchedule.getBreakEndTime().isAfter(daySchedule.getEndTime())) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            DAY_OF_WEEK_NAMES[daySchedule.getDayOfWeek()] + " 的休息時間不能晚於下班時間");
                }
            }
        }

        // 更新或建立排班
        List<StaffSchedule> toSave = new ArrayList<>();
        for (StaffScheduleRequest.DaySchedule daySchedule : request.getSchedules()) {
            StaffSchedule schedule = existingMap.get(daySchedule.getDayOfWeek());

            if (schedule == null) {
                // 建立新排班
                schedule = StaffSchedule.builder()
                        .staffId(staffId)
                        .dayOfWeek(daySchedule.getDayOfWeek())
                        .isWorkingDay(daySchedule.getIsWorkingDay())
                        .startTime(daySchedule.getStartTime())
                        .endTime(daySchedule.getEndTime())
                        .breakStartTime(daySchedule.getBreakStartTime())
                        .breakEndTime(daySchedule.getBreakEndTime())
                        .build();
                schedule.setTenantId(tenantId);
            } else {
                // 更新現有排班
                schedule.setIsWorkingDay(daySchedule.getIsWorkingDay());
                schedule.setStartTime(daySchedule.getStartTime());
                schedule.setEndTime(daySchedule.getEndTime());
                schedule.setBreakStartTime(daySchedule.getBreakStartTime());
                schedule.setBreakEndTime(daySchedule.getBreakEndTime());
            }

            toSave.add(schedule);
        }

        staffScheduleRepository.saveAll(toSave);

        log.info("員工排班更新成功，員工 ID：{}", staffId);

        return getSchedule(staffId);
    }

    /**
     * 取得員工特定日期的排班
     *
     * @param staffId   員工 ID
     * @param dayOfWeek 星期幾（0-6）
     * @return 排班資料（可能為空）
     */
    public StaffScheduleResponse.DayScheduleResponse getScheduleForDay(String staffId, int dayOfWeek) {
        String tenantId = TenantContext.getTenantId();

        return staffScheduleRepository.findByStaffIdAndDayOfWeek(staffId, tenantId, dayOfWeek)
                .map(schedule -> StaffScheduleResponse.DayScheduleResponse.builder()
                        .id(schedule.getId())
                        .dayOfWeek(dayOfWeek)
                        .dayOfWeekName(DAY_OF_WEEK_NAMES[dayOfWeek])
                        .isWorkingDay(schedule.getIsWorkingDay())
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .breakStartTime(schedule.getBreakStartTime())
                        .breakEndTime(schedule.getBreakEndTime())
                        .build())
                .orElse(null);
    }

    // ========================================
    // 請假管理
    // ========================================

    /**
     * 取得員工請假記錄
     *
     * @param staffId   員工 ID
     * @param startDate 開始日期（可選）
     * @param endDate   結束日期（可選）
     * @return 請假記錄列表
     */
    public List<StaffLeaveResponse> getLeaves(String staffId, LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();

        // 驗證員工存在
        Staff staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 預設查詢未來 60 天
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusDays(60);
        }

        List<StaffLeave> leaves = staffLeaveRepository.findByStaffIdAndDateRange(
                staffId, startDate, endDate);

        return leaves.stream()
                .map(leave -> toLeaveResponse(leave, staff.getEffectiveDisplayName()))
                .collect(Collectors.toList());
    }

    /**
     * 新增員工請假
     *
     * @param staffId 員工 ID
     * @param request 請假請求
     * @return 建立的請假記錄
     */
    @Transactional
    public List<StaffLeaveResponse> createLeaves(String staffId, CreateStaffLeaveRequest request) {
        String tenantId = TenantContext.getTenantId();

        // 驗證員工存在
        Staff staff = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 半天假時間驗證：開始時間不能晚於結束時間
        if (Boolean.FALSE.equals(request.getIsFullDay())) {
            if (request.getStartTime() != null && request.getEndTime() != null) {
                LocalTime leaveStart = LocalTime.parse(request.getStartTime());
                LocalTime leaveEnd = LocalTime.parse(request.getEndTime());
                if (!leaveStart.isBefore(leaveEnd)) {
                    throw new BusinessException(ErrorCode.SYS_PARAM_ERROR,
                            "請假開始時間必須早於結束時間");
                }
            }
        }

        List<StaffLeave> createdLeaves = new ArrayList<>();

        for (LocalDate leaveDate : request.getLeaveDates()) {
            // 檢查日期不能是過去
            if (leaveDate.isBefore(LocalDate.now())) {
                throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "請假日期不能是過去的日期");
            }

            // 檢查是否已有請假記錄
            if (staffLeaveRepository.findByStaffIdAndLeaveDateAndDeletedAtIsNull(staffId, leaveDate).isPresent()) {
                log.warn("員工 {} 在 {} 已有請假記錄，跳過", staffId, leaveDate);
                continue;
            }

            StaffLeave leave = StaffLeave.builder()
                    .staffId(staffId)
                    .leaveDate(leaveDate)
                    .leaveType(request.getLeaveType())
                    .reason(request.getReason())
                    .isFullDay(request.getIsFullDay() != null ? request.getIsFullDay() : true)
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .build();
            leave.setTenantId(tenantId);

            createdLeaves.add(staffLeaveRepository.save(leave));
        }

        log.info("成功新增 {} 筆員工請假記錄，員工 ID：{}", createdLeaves.size(), staffId);

        return createdLeaves.stream()
                .map(leave -> toLeaveResponse(leave, staff.getEffectiveDisplayName()))
                .collect(Collectors.toList());
    }

    /**
     * 刪除員工請假
     *
     * @param staffId 員工 ID
     * @param leaveId 請假 ID
     */
    @Transactional
    public void deleteLeave(String staffId, String leaveId) {
        String tenantId = TenantContext.getTenantId();

        // 驗證員工存在
        staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 查找請假記錄
        StaffLeave leave = staffLeaveRepository.findById(leaveId)
                .filter(l -> l.getStaffId().equals(staffId) && l.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SYS_NOT_FOUND, "找不到指定的請假記錄"
                ));

        // 軟刪除
        leave.setDeletedAt(LocalDateTime.now());
        staffLeaveRepository.save(leave);

        log.info("刪除員工請假記錄，員工 ID：{}，請假 ID：{}", staffId, leaveId);
    }

    /**
     * 按日期刪除員工請假
     *
     * @param staffId 員工 ID
     * @param date    請假日期
     */
    @Transactional
    public void deleteLeaveByDate(String staffId, LocalDate date) {
        String tenantId = TenantContext.getTenantId();

        // 驗證員工存在
        staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND, "找不到指定的員工"
                ));

        // 查找並刪除
        staffLeaveRepository.findByStaffIdAndLeaveDateAndDeletedAtIsNull(staffId, date)
                .ifPresent(leave -> {
                    leave.setDeletedAt(LocalDateTime.now());
                    staffLeaveRepository.save(leave);
                    log.info("刪除員工請假記錄，員工 ID：{}，日期：{}", staffId, date);
                });
    }

    /**
     * 檢查員工是否在特定日期請假
     *
     * @param staffId 員工 ID
     * @param date    日期
     * @return 是否請假
     */
    public boolean isStaffOnLeave(String staffId, LocalDate date) {
        return staffLeaveRepository.isStaffOnLeave(staffId, date);
    }

    /**
     * 轉換為請假回應
     */
    private StaffLeaveResponse toLeaveResponse(StaffLeave leave, String staffName) {
        return StaffLeaveResponse.builder()
                .id(leave.getId())
                .staffId(leave.getStaffId())
                .staffName(staffName)
                .leaveDate(leave.getLeaveDate())
                .leaveType(leave.getLeaveType())
                .leaveTypeDescription(leave.getLeaveType().getDescription())
                .reason(leave.getReason())
                .isFullDay(leave.getIsFullDay())
                .startTime(leave.getStartTime())
                .endTime(leave.getEndTime())
                .createdAt(leave.getCreatedAt())
                .build();
    }

    // ========================================
    // 行事曆
    // ========================================

    /**
     * 取得員工排班行事曆事件
     *
     * @param startDate 開始日期
     * @param endDate   結束日期
     * @param staffId   員工 ID（可選）
     * @return 行事曆事件列表
     */
    public List<StaffScheduleCalendarResponse> getCalendarEvents(LocalDate startDate, LocalDate endDate, String staffId) {
        String tenantId = TenantContext.getTenantId();

        List<StaffScheduleCalendarResponse> events = new ArrayList<>();

        // 取得員工列表
        List<Staff> staffList;
        if (staffId != null && !staffId.isEmpty()) {
            staffList = staffRepository.findByIdAndTenantIdAndDeletedAtIsNull(staffId, tenantId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            staffList = staffRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, StaffStatus.ACTIVE);
        }

        // 為每個員工產生事件
        for (Staff staff : staffList) {
            // 取得排班
            List<StaffSchedule> schedules = staffScheduleRepository.findByStaffIdAndDeletedAtIsNull(staff.getId());
            Map<Integer, StaffSchedule> scheduleMap = schedules.stream()
                    .collect(Collectors.toMap(StaffSchedule::getDayOfWeek, s -> s));

            // 取得請假
            List<StaffLeave> leaves = staffLeaveRepository.findByStaffIdAndDateRange(
                    staff.getId(), startDate, endDate);
            Map<LocalDate, StaffLeave> leaveMap = leaves.stream()
                    .collect(Collectors.toMap(StaffLeave::getLeaveDate, l -> l, (a, b) -> a));

            // 產生每天的事件
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7; // 轉換為 0=週日

                // 檢查是否請假
                StaffLeave leave = leaveMap.get(currentDate);
                if (leave != null) {
                    // 請假事件
                    events.add(buildLeaveEvent(staff, leave, currentDate));

                    // 如果是全天假，跳過上班事件
                    if (Boolean.TRUE.equals(leave.getIsFullDay())) {
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
                }

                // 上班事件
                StaffSchedule schedule = scheduleMap.get(dayOfWeek);
                if (schedule != null && Boolean.TRUE.equals(schedule.getIsWorkingDay())) {
                    events.add(buildWorkEvent(staff, schedule, currentDate, leave));
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        return events;
    }

    /**
     * 建構上班事件
     */
    private StaffScheduleCalendarResponse buildWorkEvent(Staff staff, StaffSchedule schedule, LocalDate date, StaffLeave leave) {
        LocalTime startTime = schedule.getStartTime();
        LocalTime endTime = schedule.getEndTime();

        // 如果有半天假，調整時間
        if (leave != null && !Boolean.TRUE.equals(leave.getIsFullDay())) {
            String leaveStartStr = leave.getStartTime();
            String leaveEndStr = leave.getEndTime();
            if (leaveStartStr != null && leaveEndStr != null) {
                LocalTime leaveStart = parseTime(leaveStartStr);
                LocalTime leaveEnd = parseTime(leaveEndStr);
                // 調整上班時間（避開請假時段）
                if (startTime.isBefore(leaveStart)) {
                    endTime = leaveStart;
                } else if (endTime.isAfter(leaveEnd)) {
                    startTime = leaveEnd;
                }
            }
        }

        String startDateTime = date + "T" + startTime;
        String endDateTime = date + "T" + endTime;

        return StaffScheduleCalendarResponse.builder()
                .id("work_" + staff.getId() + "_" + date)
                .staffId(staff.getId())
                .staffName(staff.getEffectiveDisplayName())
                .type("WORK")
                .title(staff.getEffectiveDisplayName() + " 上班")
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .start(startDateTime)
                .end(endDateTime)
                .backgroundColor("#1cc88a") // 綠色
                .borderColor("#1cc88a")
                .textColor("#ffffff")
                .allDay(false)
                .editable(false)
                .build();
    }

    /**
     * 建構請假事件
     */
    private StaffScheduleCalendarResponse buildLeaveEvent(Staff staff, StaffLeave leave, LocalDate date) {
        boolean isFullDay = Boolean.TRUE.equals(leave.getIsFullDay());
        LocalTime startTime = isFullDay ? LocalTime.of(0, 0) : parseTime(leave.getStartTime());
        LocalTime endTime = isFullDay ? LocalTime.of(23, 59) : parseTime(leave.getEndTime());

        String startDateTime = isFullDay ? date.toString() : date + "T" + startTime;
        String endDateTime = isFullDay ? date.plusDays(1).toString() : date + "T" + endTime;

        String leaveTypeText = leave.getLeaveType() != null ? leave.getLeaveType().getDescription() : "請假";
        String title = staff.getEffectiveDisplayName() + " " + leaveTypeText;

        return StaffScheduleCalendarResponse.builder()
                .id("leave_" + leave.getId())
                .staffId(staff.getId())
                .staffName(staff.getEffectiveDisplayName())
                .type("LEAVE")
                .title(title)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .start(startDateTime)
                .end(endDateTime)
                .backgroundColor("#e74a3b") // 紅色
                .borderColor("#e74a3b")
                .textColor("#ffffff")
                .allDay(isFullDay)
                .leaveType(leave.getLeaveType() != null ? leave.getLeaveType().name() : null)
                .leaveReason(leave.getReason())
                .editable(false)
                .build();
    }

    /**
     * 解析時間字串
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return LocalTime.of(9, 0); // 預設
        }
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return LocalTime.of(9, 0);
        }
    }
}
