package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateStaffRequest;
import com.booking.platform.dto.response.StaffResponse;
import com.booking.platform.entity.staff.Staff;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.StaffStatus;
import com.booking.platform.mapper.StaffMapper;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final TenantRepository tenantRepository;
    private final StaffMapper staffMapper;

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
}
