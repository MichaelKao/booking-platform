package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateServiceItemRequest;
import com.booking.platform.dto.response.ServiceItemResponse;
import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.enums.ServiceStatus;
import com.booking.platform.mapper.ServiceItemMapper;
import com.booking.platform.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 服務項目服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceItemMapper serviceItemMapper;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<ServiceItemResponse> getList(
            ServiceStatus status,
            String categoryId,
            String keyword,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<ServiceItem> page = serviceItemRepository.findByTenantIdAndFilters(
                tenantId, status, categoryId, keyword, pageable
        );

        List<ServiceItemResponse> content = page.getContent().stream()
                .map(serviceItemMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ServiceItemResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public ServiceItemResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        ServiceItem entity = serviceItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                ));

        return serviceItemMapper.toResponse(entity);
    }

    public List<ServiceItemResponse> getBookableServices() {
        String tenantId = TenantContext.getTenantId();

        return serviceItemRepository.findBookableServices(tenantId).stream()
                .map(serviceItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public ServiceItemResponse create(CreateServiceItemRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立服務項目，租戶：{}，參數：{}", tenantId, request);

        // 檢查名稱是否重複
        if (serviceItemRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName())) {
            throw new BusinessException(
                    ErrorCode.SERVICE_NAME_DUPLICATE,
                    "服務名稱已存在"
            );
        }

        ServiceItem entity = ServiceItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .price(request.getPrice())
                .duration(request.getDuration())
                .bufferTime(request.getBufferTime() != null ? request.getBufferTime() : 0)
                .status(ServiceStatus.ACTIVE)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .requiresStaff(request.getRequiresStaff() != null ? request.getRequiresStaff() : true)
                .maxCapacity(request.getMaxCapacity() != null ? request.getMaxCapacity() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        entity.setTenantId(tenantId);
        entity = serviceItemRepository.save(entity);

        log.info("服務項目建立成功，ID：{}", entity.getId());

        return serviceItemMapper.toResponse(entity);
    }

    @Transactional
    public ServiceItemResponse update(String id, CreateServiceItemRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新服務項目，ID：{}，參數：{}", id, request);

        ServiceItem entity = serviceItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                ));

        // 檢查名稱是否重複（排除自己）
        if (serviceItemRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                tenantId, request.getName(), id)) {
            throw new BusinessException(
                    ErrorCode.SERVICE_NAME_DUPLICATE,
                    "服務名稱已存在"
            );
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCategoryId(request.getCategoryId());
        entity.setPrice(request.getPrice());
        entity.setDuration(request.getDuration());

        if (request.getBufferTime() != null) {
            entity.setBufferTime(request.getBufferTime());
        }
        if (request.getIsVisible() != null) {
            entity.setIsVisible(request.getIsVisible());
        }
        if (request.getRequiresStaff() != null) {
            entity.setRequiresStaff(request.getRequiresStaff());
        }
        if (request.getMaxCapacity() != null) {
            entity.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }

        entity = serviceItemRepository.save(entity);

        log.info("服務項目更新成功，ID：{}", entity.getId());

        return serviceItemMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除服務項目，ID：{}", id);

        ServiceItem entity = serviceItemRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SERVICE_NOT_FOUND, "找不到指定的服務項目"
                ));

        entity.softDelete();
        serviceItemRepository.save(entity);

        log.info("服務項目刪除成功，ID：{}", id);
    }
}
