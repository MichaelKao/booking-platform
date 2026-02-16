package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateBookingRequest;
import com.booking.platform.dto.request.UpdateBookingRequest;
import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 預約管理 Controller（店家後台）
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    // ========================================
    // 查詢 API
    // ========================================

    @GetMapping
    public ApiResponse<PageResponse<BookingResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String sort
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // 向後相容：若傳入 date 則視為單日查詢
        if (date != null && startDate == null && endDate == null) {
            startDate = date;
            endDate = date;
        }

        return ApiResponse.ok(bookingService.getList(status, startDate, endDate, staffId, customerId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(bookingService.getDetail(id));
    }

    @GetMapping("/staff/{staffId}/date/{date}")
    public ApiResponse<List<BookingResponse>> getByStaffAndDate(
            @PathVariable String staffId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(bookingService.getByStaffAndDate(staffId, date));
    }

    /**
     * 取得行事曆資料
     */
    @GetMapping("/calendar")
    public ApiResponse<List<BookingResponse>> getCalendarData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ApiResponse.ok(bookingService.getCalendarData(start, end));
    }

    // ========================================
    // 寫入 API
    // ========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        log.info("收到建立預約請求：{}", request);
        return ApiResponse.ok(bookingService.create(request));
    }

    /**
     * 更新預約
     */
    @PutMapping("/{id}")
    public ApiResponse<BookingResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        log.info("收到更新預約請求，ID：{}", id);
        return ApiResponse.ok(bookingService.update(id, request));
    }

    // ========================================
    // 狀態變更 API
    // ========================================

    @PostMapping("/{id}/confirm")
    public ApiResponse<BookingResponse> confirm(@PathVariable String id) {
        log.info("收到確認預約請求，ID：{}", id);
        return ApiResponse.ok(bookingService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<BookingResponse> complete(@PathVariable String id) {
        log.info("收到完成預約請求，ID：{}", id);
        return ApiResponse.ok(bookingService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        log.info("收到取消預約請求，ID：{}，原因：{}", id, reason);
        return ApiResponse.ok(bookingService.cancel(id, reason));
    }

    @PostMapping("/{id}/no-show")
    public ApiResponse<BookingResponse> markNoShow(@PathVariable String id) {
        log.info("收到標記爽約請求，ID：{}", id);
        return ApiResponse.ok(bookingService.markNoShow(id));
    }
}
