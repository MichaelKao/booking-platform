package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateCustomerRequest;
import com.booking.platform.dto.response.CustomerResponse;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.customer.MembershipLevel;
import com.booking.platform.entity.customer.PointTransaction;
import com.booking.platform.enums.CustomerStatus;
import com.booking.platform.mapper.CustomerMapper;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.MembershipLevelRepository;
import com.booking.platform.repository.PointTransactionRepository;
import com.booking.platform.service.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 顧客服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final CustomerMapper customerMapper;
    private final SseNotificationService sseNotificationService;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<CustomerResponse> getList(
            CustomerStatus status,
            String keyword,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<Customer> page = customerRepository.findByTenantIdAndFilters(
                tenantId, status, keyword, pageable
        );

        // 批次查詢所有會員等級（避免 N+1）
        Map<String, String> levelNameMap = membershipLevelRepository
                .findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId)
                .stream()
                .collect(Collectors.toMap(MembershipLevel::getId, MembershipLevel::getName));

        List<CustomerResponse> content = page.getContent().stream()
                .map(c -> {
                    String levelName = c.getMembershipLevelId() != null
                            ? levelNameMap.get(c.getMembershipLevelId())
                            : null;
                    return customerMapper.toResponse(c, levelName);
                })
                .collect(Collectors.toList());

        return PageResponse.<CustomerResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public CustomerResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        String levelName = null;
        if (entity.getMembershipLevelId() != null) {
            levelName = membershipLevelRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(entity.getMembershipLevelId(), tenantId)
                    .map(MembershipLevel::getName)
                    .orElse(null);
        }

        return customerMapper.toResponse(entity, levelName);
    }

    public CustomerResponse getByLineUserId(String lineUserId) {
        String tenantId = TenantContext.getTenantId();

        Customer entity = customerRepository.findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .orElse(null);

        return entity != null ? customerMapper.toResponse(entity) : null;
    }

    public List<CustomerResponse> getBirthdayCustomers() {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now();

        return customerRepository.findBirthdayCustomers(
                        tenantId, today.getMonthValue(), today.getDayOfMonth()
                ).stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立顧客，租戶：{}，參數：{}", tenantId, request);

        // 檢查 LINE User ID 是否重複
        if (request.getLineUserId() != null && !request.getLineUserId().isEmpty()) {
            if (customerRepository.existsByTenantIdAndLineUserIdAndDeletedAtIsNull(
                    tenantId, request.getLineUserId())) {
                throw new BusinessException(
                        ErrorCode.CUSTOMER_LINE_ID_DUPLICATE,
                        "此 LINE 帳號已綁定其他顧客"
                );
            }
        }

        // 檢查手機號碼是否重複
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (customerRepository.existsByTenantIdAndPhoneAndDeletedAtIsNull(
                    tenantId, request.getPhone())) {
                throw new BusinessException(
                        ErrorCode.CUSTOMER_PHONE_DUPLICATE,
                        "此手機號碼已存在"
                );
            }
        }

        // 取得預設會員等級
        String defaultLevelId = membershipLevelRepository
                .findByTenantIdAndIsDefaultTrueAndDeletedAtIsNull(tenantId)
                .map(MembershipLevel::getId)
                .orElse(null);

        Customer entity = Customer.builder()
                .lineUserId(request.getLineUserId())
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .address(request.getAddress())
                .status(CustomerStatus.ACTIVE)
                .membershipLevelId(defaultLevelId)
                .totalSpent(BigDecimal.ZERO)
                .visitCount(0)
                .pointBalance(0)
                .noShowCount(0)
                .note(request.getNote())
                .tags(request.getTags())
                .build();

        entity.setTenantId(tenantId);
        entity = customerRepository.save(entity);

        log.info("顧客建立成功，ID：{}", entity.getId());

        // 推送 SSE 通知
        try {
            sseNotificationService.notifyNewCustomer(tenantId, Map.of(
                    "name", entity.getName() != null ? entity.getName() : "新顧客",
                    "phone", entity.getPhone() != null ? entity.getPhone() : ""
            ));
        } catch (Exception e) {
            log.warn("推送新顧客通知失敗：{}", e.getMessage());
        }

        return customerMapper.toResponse(entity);
    }

    @Transactional
    public CustomerResponse update(String id, CreateCustomerRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新顧客，ID：{}，參數：{}", id, request);

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        // 檢查手機號碼是否重複（排除自己）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (customerRepository.existsByTenantIdAndPhoneAndIdNotAndDeletedAtIsNull(
                    tenantId, request.getPhone(), id)) {
                throw new BusinessException(
                        ErrorCode.CUSTOMER_PHONE_DUPLICATE,
                        "此手機號碼已存在"
                );
            }
        }

        entity.setName(request.getName());
        entity.setNickname(request.getNickname());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setGender(request.getGender());
        entity.setBirthday(request.getBirthday());
        entity.setAddress(request.getAddress());
        entity.setNote(request.getNote());
        entity.setTags(request.getTags());

        entity = customerRepository.save(entity);

        log.info("顧客更新成功，ID：{}", entity.getId());

        return customerMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除顧客，ID：{}", id);

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        entity.softDelete();
        customerRepository.save(entity);

        log.info("顧客刪除成功，ID：{}", id);
    }

    // ========================================
    // 點數操作
    // ========================================

    private static final int MAX_RETRY = 3;

    @Transactional
    public CustomerResponse addPoints(String id, int points, String description) {
        String tenantId = TenantContext.getTenantId();

        log.info("增加顧客點數，ID：{}，點數：{}", id, points);

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                        ));

                entity.addPoints(points);
                entity = customerRepository.save(entity);

                // 記錄點數交易
                PointTransaction transaction = PointTransaction.builder()
                        .customerId(id)
                        .type("EARN")
                        .points(points)
                        .balanceAfter(entity.getPointBalance())
                        .description(description)
                        .build();
                transaction.setTenantId(tenantId);
                pointTransactionRepository.save(transaction);

                log.info("顧客點數增加成功，ID：{}，新餘額：{}", id, entity.getPointBalance());

                return customerMapper.toResponse(entity);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("點數增加樂觀鎖衝突，ID：{}，第 {} 次重試", id, attempt + 1);
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "系統繁忙，請稍後再試");
                }
            }
        }
        throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "系統繁忙，請稍後再試");
    }

    @Transactional
    public CustomerResponse deductPoints(String id, int points, String description) {
        String tenantId = TenantContext.getTenantId();

        log.info("扣除顧客點數，ID：{}，點數：{}", id, points);

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                        ));

                if (!entity.deductPoints(points)) {
                    throw new BusinessException(
                            ErrorCode.POINT_INSUFFICIENT,
                            "點數不足"
                    );
                }

                entity = customerRepository.save(entity);

                // 記錄點數交易
                PointTransaction transaction = PointTransaction.builder()
                        .customerId(id)
                        .type("REDEEM")
                        .points(-points)
                        .balanceAfter(entity.getPointBalance())
                        .description(description)
                        .build();
                transaction.setTenantId(tenantId);
                pointTransactionRepository.save(transaction);

                log.info("顧客點數扣除成功，ID：{}，新餘額：{}", id, entity.getPointBalance());

                return customerMapper.toResponse(entity);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("點數扣除樂觀鎖衝突，ID：{}，第 {} 次重試", id, attempt + 1);
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "系統繁忙，請稍後再試");
                }
            }
        }
        throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "系統繁忙，請稍後再試");
    }

    // ========================================
    // 狀態操作
    // ========================================

    @Transactional
    public CustomerResponse blockCustomer(String id) {
        String tenantId = TenantContext.getTenantId();

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        entity.setStatus(CustomerStatus.BLOCKED);
        entity = customerRepository.save(entity);

        log.info("顧客已封鎖，ID：{}", id);

        return customerMapper.toResponse(entity);
    }

    @Transactional
    public CustomerResponse unblockCustomer(String id) {
        String tenantId = TenantContext.getTenantId();

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        entity.setStatus(CustomerStatus.ACTIVE);
        entity = customerRepository.save(entity);

        log.info("顧客已解除封鎖，ID：{}", id);

        return customerMapper.toResponse(entity);
    }

    // ========================================
    // 標籤操作（ADVANCED_CUSTOMER 功能）
    // ========================================

    /**
     * 更新顧客標籤
     */
    @Transactional
    public CustomerResponse updateTags(String id, List<String> tags) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新顧客標籤，ID：{}，標籤：{}", id, tags);

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        if (tags == null || tags.isEmpty()) {
            entity.setTags(null);
        } else {
            // 清理並去重標籤
            Set<String> uniqueTags = new HashSet<>();
            for (String tag : tags) {
                String cleaned = tag.trim();
                if (!cleaned.isEmpty()) {
                    uniqueTags.add(cleaned);
                }
            }
            entity.setTags(String.join(",", uniqueTags));
        }

        entity = customerRepository.save(entity);
        log.info("顧客標籤更新成功，ID：{}", id);

        return customerMapper.toResponse(entity);
    }

    /**
     * 新增顧客標籤
     */
    @Transactional
    public CustomerResponse addTag(String id, String tag) {
        String tenantId = TenantContext.getTenantId();

        log.info("新增顧客標籤，ID：{}，標籤：{}", id, tag);

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        String cleaned = tag.trim();
        if (cleaned.isEmpty()) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "標籤不能為空");
        }

        Set<String> tags = new HashSet<>();
        if (entity.getTags() != null && !entity.getTags().isEmpty()) {
            tags.addAll(Arrays.asList(entity.getTags().split(",")));
        }
        tags.add(cleaned);
        entity.setTags(String.join(",", tags));

        entity = customerRepository.save(entity);
        log.info("顧客標籤新增成功，ID：{}", id);

        return customerMapper.toResponse(entity);
    }

    /**
     * 移除顧客標籤
     */
    @Transactional
    public CustomerResponse removeTag(String id, String tag) {
        String tenantId = TenantContext.getTenantId();

        log.info("移除顧客標籤，ID：{}，標籤：{}", id, tag);

        Customer entity = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        if (entity.getTags() == null || entity.getTags().isEmpty()) {
            return customerMapper.toResponse(entity);
        }

        Set<String> tags = new HashSet<>(Arrays.asList(entity.getTags().split(",")));
        tags.remove(tag.trim());

        if (tags.isEmpty()) {
            entity.setTags(null);
        } else {
            entity.setTags(String.join(",", tags));
        }

        entity = customerRepository.save(entity);
        log.info("顧客標籤移除成功，ID：{}", id);

        return customerMapper.toResponse(entity);
    }

    /**
     * 依標籤搜尋顧客
     */
    public List<CustomerResponse> getCustomersByTag(String tag) {
        String tenantId = TenantContext.getTenantId();

        log.debug("依標籤搜尋顧客，標籤：{}", tag);

        // 使用 LIKE 搜尋包含該標籤的顧客
        List<Customer> customers = customerRepository.findByTenantIdAndTagContaining(tenantId, tag.trim());

        return customers.stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得所有使用中的標籤
     */
    public List<String> getAllTags() {
        String tenantId = TenantContext.getTenantId();

        log.debug("取得所有標籤");

        List<Customer> customers = customerRepository.findByTenantIdAndTagsNotNull(tenantId);

        Set<String> allTags = new HashSet<>();
        for (Customer customer : customers) {
            if (customer.getTags() != null && !customer.getTags().isEmpty()) {
                allTags.addAll(Arrays.asList(customer.getTags().split(",")));
            }
        }

        return new ArrayList<>(allTags).stream()
                .sorted()
                .collect(Collectors.toList());
    }

    // ========================================
    // 點數交易記錄
    // ========================================

    /**
     * 取得顧客的點數交易記錄
     */
    public PageResponse<com.booking.platform.dto.response.PointTransactionResponse> getPointTransactions(
            String customerId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        log.debug("取得顧客點數交易記錄，顧客 ID：{}", customerId);

        // 驗證顧客存在
        customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        Page<PointTransaction> page = pointTransactionRepository.findByCustomerId(
                tenantId, customerId, pageable
        );

        List<com.booking.platform.dto.response.PointTransactionResponse> responses = page.getContent()
                .stream()
                .map(t -> com.booking.platform.dto.response.PointTransactionResponse.builder()
                        .id(t.getId())
                        .customerId(t.getCustomerId())
                        .type(t.getType())
                        .points(t.getPoints())
                        .balance(t.getBalanceAfter())
                        .description(t.getDescription())
                        .orderId(t.getOrderId())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<com.booking.platform.dto.response.PointTransactionResponse>builder()
                .content(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
