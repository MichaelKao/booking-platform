package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.entity.catalog.ServiceCategory;
import com.booking.platform.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服務分類 Controller
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/service-categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ServiceCategoryController {

    private final ServiceCategoryRepository serviceCategoryRepository;

    /**
     * 取得服務分類列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getList(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        String tenantId = TenantContext.getTenantId();

        List<ServiceCategory> categories = serviceCategoryRepository.findByTenantId(tenantId, activeOnly);

        // 轉換為簡單的 Map 格式
        List<Map<String, Object>> result = categories.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "description", c.getDescription() != null ? c.getDescription() : "",
                        "iconUrl", c.getIconUrl() != null ? c.getIconUrl() : "",
                        "isActive", c.getIsActive(),
                        "sortOrder", c.getSortOrder()
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }
}
