package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateStaffRequest;
import com.booking.platform.dto.request.StaffScheduleRequest;
import com.booking.platform.dto.response.StaffResponse;
import com.booking.platform.dto.response.StaffScheduleResponse;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 員工管理 Controller（店家後台）
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Validated
@Slf4j
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ApiResponse<PageResponse<StaffResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) StaffStatus status,
            @RequestParam(required = false) String keyword
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(staffService.getList(status, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<StaffResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(staffService.getDetail(id));
    }

    @GetMapping("/bookable")
    public ApiResponse<List<StaffResponse>> getBookableStaffs() {
        return ApiResponse.ok(staffService.getBookableStaffs());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        log.info("收到建立員工請求：{}", request);
        return ApiResponse.ok(staffService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StaffResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        log.info("收到更新員工請求，ID：{}", id);
        return ApiResponse.ok(staffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("收到刪除員工請求，ID：{}", id);
        staffService.delete(id);
        return ApiResponse.ok();
    }

    // ========================================
    // 排班 API
    // ========================================

    /**
     * 取得員工排班
     */
    @GetMapping("/{id}/schedule")
    public ApiResponse<StaffScheduleResponse> getSchedule(@PathVariable String id) {
        log.info("收到取得員工排班請求，ID：{}", id);
        return ApiResponse.ok(staffService.getSchedule(id));
    }

    /**
     * 更新員工排班（批次 7 天）
     */
    @PutMapping("/{id}/schedule")
    public ApiResponse<StaffScheduleResponse> updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody StaffScheduleRequest request
    ) {
        log.info("收到更新員工排班請求，ID：{}", id);
        return ApiResponse.ok(staffService.updateSchedule(id, request));
    }
}
