package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateMembershipLevelRequest;
import com.booking.platform.dto.response.MembershipLevelResponse;
import com.booking.platform.entity.customer.MembershipLevel;
import com.booking.platform.mapper.MembershipLevelMapper;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.MembershipLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 會員等級服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MembershipLevelService {

    private final MembershipLevelRepository membershipLevelRepository;
    private final CustomerRepository customerRepository;
    private final MembershipLevelMapper membershipLevelMapper;

    // ========================================
    // 查詢方法
    // ========================================

    public List<MembershipLevelResponse> getList() {
        String tenantId = TenantContext.getTenantId();

        return membershipLevelRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId)
                .stream()
                .map(level -> {
                    Long memberCount = customerRepository.countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(
                            tenantId, level.getId()
                    );
                    return membershipLevelMapper.toResponse(level, memberCount);
                })
                .collect(Collectors.toList());
    }

    public MembershipLevelResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        MembershipLevel entity = membershipLevelRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.MEMBERSHIP_LEVEL_NOT_FOUND, "找不到指定的會員等級"
                ));

        Long memberCount = customerRepository.countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(
                tenantId, entity.getId()
        );

        return membershipLevelMapper.toResponse(entity, memberCount);
    }

    public MembershipLevelResponse getDefaultLevel() {
        String tenantId = TenantContext.getTenantId();

        return membershipLevelRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                .map(membershipLevelMapper::toResponse)
                .orElse(null);
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public MembershipLevelResponse create(CreateMembershipLevelRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立會員等級，租戶：{}，參數：{}", tenantId, request);

        // 檢查名稱是否重複
        if (membershipLevelRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName())) {
            throw new BusinessException(
                    ErrorCode.MEMBERSHIP_LEVEL_NAME_DUPLICATE,
                    "此會員等級名稱已存在"
            );
        }

        // 如果設為預設，取消其他預設
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            membershipLevelRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        membershipLevelRepository.save(existingDefault);
                    });
        }

        // 計算排序順序
        Integer maxSortOrder = membershipLevelRepository.findMaxSortOrderByTenantId(tenantId);
        int sortOrder = (maxSortOrder != null ? maxSortOrder : 0) + 1;

        MembershipLevel entity = MembershipLevel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .badgeColor(request.getBadgeColor())
                .upgradeThreshold(request.getUpgradeThreshold())
                .discountRate(request.getDiscountRate())
                .pointRate(request.getPointRate())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isActive(true)
                .sortOrder(sortOrder)
                .build();

        entity.setTenantId(tenantId);
        entity = membershipLevelRepository.save(entity);

        log.info("會員等級建立成功，ID：{}", entity.getId());

        return membershipLevelMapper.toResponse(entity, 0L);
    }

    @Transactional
    public MembershipLevelResponse update(String id, CreateMembershipLevelRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新會員等級，ID：{}，參數：{}", id, request);

        MembershipLevel entity = membershipLevelRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.MEMBERSHIP_LEVEL_NOT_FOUND, "找不到指定的會員等級"
                ));

        // 檢查名稱是否重複（排除自己）
        if (membershipLevelRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                tenantId, request.getName(), id)) {
            throw new BusinessException(
                    ErrorCode.MEMBERSHIP_LEVEL_NAME_DUPLICATE,
                    "此會員等級名稱已存在"
            );
        }

        // 如果設為預設，取消其他預設
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            membershipLevelRepository.findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        membershipLevelRepository.save(existingDefault);
                    });
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setBadgeColor(request.getBadgeColor());
        entity.setUpgradeThreshold(request.getUpgradeThreshold());
        entity.setDiscountRate(request.getDiscountRate());
        entity.setPointRate(request.getPointRate());
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }

        entity = membershipLevelRepository.save(entity);

        Long memberCount = customerRepository.countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(
                tenantId, entity.getId()
        );

        log.info("會員等級更新成功，ID：{}", entity.getId());

        return membershipLevelMapper.toResponse(entity, memberCount);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除會員等級，ID：{}", id);

        MembershipLevel entity = membershipLevelRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.MEMBERSHIP_LEVEL_NOT_FOUND, "找不到指定的會員等級"
                ));

        // 檢查是否為預設等級
        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            throw new BusinessException(
                    ErrorCode.MEMBERSHIP_LEVEL_DEFAULT_CANNOT_DELETE,
                    "無法刪除預設會員等級"
            );
        }

        // 檢查是否有會員使用此等級
        Long memberCount = customerRepository.countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(
                tenantId, id
        );
        if (memberCount > 0) {
            throw new BusinessException(
                    ErrorCode.MEMBERSHIP_LEVEL_HAS_MEMBERS,
                    "此會員等級下尚有 " + memberCount + " 位會員，無法刪除"
            );
        }

        entity.softDelete();
        membershipLevelRepository.save(entity);

        log.info("會員等級刪除成功，ID：{}", id);
    }

    // ========================================
    // 狀態操作
    // ========================================

    @Transactional
    public MembershipLevelResponse toggleActive(String id) {
        String tenantId = TenantContext.getTenantId();

        MembershipLevel entity = membershipLevelRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.MEMBERSHIP_LEVEL_NOT_FOUND, "找不到指定的會員等級"
                ));

        entity.setIsActive(!entity.getIsActive());
        entity = membershipLevelRepository.save(entity);

        log.info("會員等級狀態切換，ID：{}，新狀態：{}", id, entity.getIsActive());

        Long memberCount = customerRepository.countByTenantIdAndMembershipLevelIdAndDeletedAtIsNull(
                tenantId, entity.getId()
        );

        return membershipLevelMapper.toResponse(entity, memberCount);
    }

    @Transactional
    public void updateSortOrder(List<String> ids) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新會員等級排序，租戶：{}", tenantId);

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            membershipLevelRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                    .ifPresent(entity -> {
                        entity.setSortOrder(ids.indexOf(id) + 1);
                        membershipLevelRepository.save(entity);
                    });
        }

        log.info("會員等級排序更新成功");
    }
}
