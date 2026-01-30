package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateServiceItemRequest;
import com.booking.platform.dto.response.ServiceItemResponse;
import com.booking.platform.enums.ServiceStatus;
import com.booking.platform.service.ServiceItemService;
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
 * 服務項目管理 Controller（店家後台）
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @GetMapping
    public ApiResponse<PageResponse<ServiceItemResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String keyword
    ) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(serviceItemService.getList(status, categoryId, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItemResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(serviceItemService.getDetail(id));
    }

    @GetMapping("/bookable")
    public ApiResponse<List<ServiceItemResponse>> getBookableServices() {
        return ApiResponse.ok(serviceItemService.getBookableServices());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServiceItemResponse> create(@Valid @RequestBody CreateServiceItemRequest request) {
        log.info("收到建立服務項目請求：{}", request);
        return ApiResponse.ok(serviceItemService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceItemResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateServiceItemRequest request
    ) {
        log.info("收到更新服務項目請求，ID：{}", id);
        return ApiResponse.ok(serviceItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("收到刪除服務項目請求，ID：{}", id);
        serviceItemService.delete(id);
        return ApiResponse.ok();
    }
}
