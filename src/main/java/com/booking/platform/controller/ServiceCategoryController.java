package com.booking.platform.controller;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.entity.catalog.ServiceCategory;
import com.booking.platform.repository.ServiceCategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    /**
     * 取得單一分類
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable String id) {
        String tenantId = TenantContext.getTenantId();

        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到此分類"));

        return ApiResponse.ok(Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() != null ? category.getDescription() : "",
                "iconUrl", category.getIconUrl() != null ? category.getIconUrl() : "",
                "isActive", category.getIsActive(),
                "sortOrder", category.getSortOrder()
        ));
    }

    /**
     * 新增服務分類
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CategoryRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("新增服務分類，租戶：{}，名稱：{}", tenantId, request.getName());

        // 檢查名稱是否重複
        if (serviceCategoryRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName().trim())) {
            throw new BusinessException(ErrorCode.SERVICE_NAME_DUPLICATE, "分類名稱已存在");
        }

        // 取得最大排序值
        List<ServiceCategory> existing = serviceCategoryRepository.findByTenantId(tenantId, false);
        int maxSort = existing.stream().mapToInt(ServiceCategory::getSortOrder).max().orElse(0);

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .sortOrder(maxSort + 1)
                .build();
        category.setTenantId(tenantId);

        category = serviceCategoryRepository.save(category);
        log.info("服務分類建立成功，ID：{}", category.getId());

        return ApiResponse.ok("分類建立成功", Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() != null ? category.getDescription() : "",
                "isActive", category.getIsActive(),
                "sortOrder", category.getSortOrder()
        ));
    }

    /**
     * 更新服務分類
     */
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.info("更新服務分類，租戶：{}，ID：{}", tenantId, id);

        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到此分類"));

        // 檢查名稱是否重複（排除自己）
        if (!category.getName().equals(request.getName().trim()) &&
                serviceCategoryRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName().trim())) {
            throw new BusinessException(ErrorCode.SERVICE_NAME_DUPLICATE, "分類名稱已存在");
        }

        category.setName(request.getName().trim());
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        category = serviceCategoryRepository.save(category);
        log.info("服務分類更新成功，ID：{}", category.getId());

        return ApiResponse.ok("分類更新成功", Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() != null ? category.getDescription() : "",
                "isActive", category.getIsActive(),
                "sortOrder", category.getSortOrder()
        ));
    }

    /**
     * 刪除服務分類
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        String tenantId = TenantContext.getTenantId();
        log.info("刪除服務分類，租戶：{}，ID：{}", tenantId, id);

        ServiceCategory category = serviceCategoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到此分類"));

        category.setDeletedAt(LocalDateTime.now());
        serviceCategoryRepository.save(category);
        log.info("服務分類刪除成功，ID：{}", id);

        return ApiResponse.ok("分類刪除成功", null);
    }

    /**
     * 分類請求 DTO
     */
    @Data
    public static class CategoryRequest {
        @NotBlank(message = "分類名稱不能為空")
        @Size(max = 50, message = "分類名稱最多50字")
        private String name;

        @Size(max = 200, message = "描述最多200字")
        private String description;

        @Size(max = 500, message = "圖示URL最多500字")
        private String iconUrl;

        private Boolean isActive;
    }
}
