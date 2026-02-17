package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreateTenantRequest;
import com.booking.platform.dto.request.UpdateTenantRequest;
import com.booking.platform.dto.response.TenantDetailResponse;
import com.booking.platform.dto.response.TenantListItemResponse;
import com.booking.platform.dto.response.TenantResponse;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.mapper.TenantMapper;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.StaffRepository;
import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 租戶服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TenantService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantRepository tenantRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final TenantMapper tenantMapper;
    private final FeatureService featureService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 分頁查詢列表
     *
     * @param status 狀態篩選（可選）
     * @param keyword 關鍵字（可選）
     * @param pageable 分頁參數
     * @return 分頁結果
     */
    public PageResponse<TenantListItemResponse> getList(
            TenantStatus status,
            String keyword,
            Pageable pageable
    ) {
        // ========================================
        // 1. 執行查詢
        // ========================================

        Page<TenantListItemResponse> page = tenantRepository.findListItems(
                status, keyword, pageable
        );

        // ========================================
        // 2. 返回結果
        // ========================================

        return PageResponse.from(page);
    }

    /**
     * 查詢詳情
     *
     * @param id 租戶 ID
     * @return 詳情資料
     */
    public TenantDetailResponse getDetail(String id) {
        // ========================================
        // 1. 查詢資料
        // ========================================

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        // ========================================
        // 2. 轉換為回應物件
        // ========================================

        TenantDetailResponse response = tenantMapper.toDetailResponse(entity);

        // ========================================
        // 3. 查詢統計資料
        // ========================================

        long staffCount = staffRepository.countByTenantIdAndDeletedAtIsNull(id);
        long customerCount = customerRepository.countByTenantIdAndDeletedAtIsNull(id);

        response.setStaffCount(staffCount);
        response.setCustomerCount(customerCount);

        // ========================================
        // 4. 返回結果
        // ========================================

        return response;
    }

    /**
     * 依代碼查詢
     *
     * @param code 租戶代碼
     * @return 租戶資料
     */
    public TenantResponse getByCode(String code) {
        Tenant entity = tenantRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        return tenantMapper.toResponse(entity);
    }

    // ========================================
    // 寫入方法
    // ========================================

    /**
     * 建立租戶
     *
     * @param request 建立請求
     * @return 建立結果
     */
    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        log.info("建立租戶，參數：{}", request);

        // ========================================
        // 1. 驗證業務規則
        // ========================================

        // 檢查代碼是否重複
        if (tenantRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new BusinessException(
                    ErrorCode.TENANT_CODE_DUPLICATE,
                    "租戶代碼已存在，請使用其他代碼"
            );
        }

        // ========================================
        // 2. 建立 Entity
        // ========================================

        Tenant entity = Tenant.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .status(TenantStatus.PENDING)
                .isTestAccount(Boolean.TRUE.equals(request.getIsTestAccount()))
                .pointBalance(BigDecimal.ZERO)
                .maxStaffCount(3)
                .monthlyPushQuota(100)
                .monthlyPushUsed(0)
                .build();

        // 先產生 ID（Tenant 的 tenantId 就是自己的 ID）
        String tenantId = java.util.UUID.randomUUID().toString();
        entity.setId(tenantId);
        entity.setTenantId(tenantId);

        // ========================================
        // 3. 儲存到資料庫
        // ========================================

        entity = tenantRepository.save(entity);

        // ========================================
        // 4. 初始化免費功能
        // ========================================

        featureService.initializeTenantFreeFeatures(tenantId);

        // ========================================
        // 5. 返回結果
        // ========================================

        log.info("租戶建立成功，ID：{}，代碼：{}", entity.getId(), entity.getCode());

        return tenantMapper.toResponse(entity);
    }

    /**
     * 更新租戶
     *
     * @param id 租戶 ID
     * @param request 更新請求
     * @return 更新結果
     */
    @Transactional
    public TenantResponse update(String id, UpdateTenantRequest request) {
        log.info("更新租戶，ID：{}，參數：{}", id, request);

        // ========================================
        // 1. 查詢現有資料
        // ========================================

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        // ========================================
        // 2. 更新欄位
        // ========================================

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setLogoUrl(request.getLogoUrl());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setAddress(request.getAddress());

        // 超管重設密碼
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("超管重設租戶密碼，租戶 ID：{}", id);
        }

        // ========================================
        // 3. 儲存更新
        // ========================================

        entity = tenantRepository.save(entity);

        // ========================================
        // 4. 返回結果
        // ========================================

        log.info("租戶更新成功，ID：{}", entity.getId());

        return tenantMapper.toResponse(entity);
    }

    /**
     * 啟用租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @Transactional
    public TenantResponse activate(String id) {
        log.info("啟用租戶，ID：{}", id);

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        entity.activate();
        entity = tenantRepository.save(entity);

        log.info("租戶啟用成功，ID：{}", entity.getId());

        return tenantMapper.toResponse(entity);
    }

    /**
     * 停用租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @Transactional
    public TenantResponse suspend(String id) {
        log.info("停用租戶，ID：{}", id);

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        entity.suspend();
        entity = tenantRepository.save(entity);

        log.info("租戶停用成功，ID：{}", entity.getId());

        return tenantMapper.toResponse(entity);
    }

    /**
     * 凍結租戶
     *
     * @param id 租戶 ID
     * @return 更新結果
     */
    @Transactional
    public TenantResponse freeze(String id) {
        log.info("凍結租戶，ID：{}", id);

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        entity.freeze();
        entity = tenantRepository.save(entity);

        log.info("租戶凍結成功，ID：{}", entity.getId());

        return tenantMapper.toResponse(entity);
    }

    /**
     * 刪除租戶（軟刪除）
     *
     * @param id 租戶 ID
     */
    @Transactional
    public void delete(String id) {
        log.info("刪除租戶，ID：{}", id);

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        entity.softDelete();
        tenantRepository.save(entity);

        log.info("租戶刪除成功，ID：{}", id);
    }

    // ========================================
    // 點數相關
    // ========================================

    /**
     * 增加點數
     *
     * @param id 租戶 ID
     * @param amount 增加金額
     * @return 更新結果
     */
    @Transactional
    public TenantResponse addPoints(String id, BigDecimal amount) {
        log.info("增加租戶點數，ID：{}，金額：{}", id, amount);

        Tenant entity = tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TENANT_NOT_FOUND, "找不到指定的租戶"
                ));

        entity.addPoints(amount);
        entity = tenantRepository.save(entity);

        log.info("租戶點數增加成功，ID：{}，新餘額：{}", entity.getId(), entity.getPointBalance());

        return tenantMapper.toResponse(entity);
    }
}
