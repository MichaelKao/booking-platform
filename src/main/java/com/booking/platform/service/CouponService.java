package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateCouponRequest;
import com.booking.platform.dto.request.IssueCouponRequest;
import com.booking.platform.dto.response.CouponInstanceResponse;
import com.booking.platform.dto.response.CouponResponse;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.marketing.Coupon;
import com.booking.platform.entity.marketing.CouponInstance;
import com.booking.platform.enums.CouponInstanceStatus;
import com.booking.platform.enums.CouponStatus;
import com.booking.platform.enums.CouponType;
import com.booking.platform.mapper.CouponMapper;
import com.booking.platform.repository.CouponInstanceRepository;
import com.booking.platform.repository.CouponRepository;
import com.booking.platform.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 票券服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponInstanceRepository couponInstanceRepository;
    private final CustomerRepository customerRepository;
    private final CouponMapper couponMapper;

    // ========================================
    // 票券定義查詢
    // ========================================

    public PageResponse<CouponResponse> getList(
            CouponStatus status,
            CouponType type,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<Coupon> page = couponRepository.findByTenantIdAndFilters(tenantId, status, type, pageable);

        List<CouponResponse> content = page.getContent().stream()
                .map(couponMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<CouponResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public CouponResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        return couponMapper.toResponse(entity);
    }

    public List<CouponResponse> getAvailableCoupons() {
        String tenantId = TenantContext.getTenantId();

        return couponRepository.findAvailableByTenantId(tenantId)
                .stream()
                .map(couponMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 票券定義寫入
    // ========================================

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立票券，租戶：{}，名稱：{}", tenantId, request.getName());

        // 檢查名稱是否重複
        if (couponRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName())) {
            throw new BusinessException(ErrorCode.COUPON_NAME_DUPLICATE, "票券名稱已存在");
        }

        Coupon entity = Coupon.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .status(CouponStatus.DRAFT)
                .discountAmount(request.getDiscountAmount())
                .discountPercent(request.getDiscountPercent())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .giftItem(request.getGiftItem())
                .totalQuantity(request.getTotalQuantity())
                .limitPerCustomer(request.getLimitPerCustomer())
                .validDays(request.getValidDays())
                .validStartAt(request.getValidStartAt())
                .validEndAt(request.getValidEndAt())
                .applicableServices(request.getApplicableServices())
                .imageUrl(request.getImageUrl())
                .terms(request.getTerms())
                .note(request.getNote())
                .build();

        entity.setTenantId(tenantId);
        entity = couponRepository.save(entity);

        log.info("票券建立成功，ID：{}", entity.getId());

        return couponMapper.toResponse(entity);
    }

    @Transactional
    public CouponResponse update(String id, CreateCouponRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新票券，ID：{}", id);

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        // 檢查名稱是否重複
        if (couponRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                tenantId, request.getName(), id)) {
            throw new BusinessException(ErrorCode.COUPON_NAME_DUPLICATE, "票券名稱已存在");
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setDiscountAmount(request.getDiscountAmount());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setMinOrderAmount(request.getMinOrderAmount());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setGiftItem(request.getGiftItem());
        entity.setTotalQuantity(request.getTotalQuantity());
        entity.setLimitPerCustomer(request.getLimitPerCustomer());
        entity.setValidDays(request.getValidDays());
        entity.setValidStartAt(request.getValidStartAt());
        entity.setValidEndAt(request.getValidEndAt());
        entity.setApplicableServices(request.getApplicableServices());
        entity.setImageUrl(request.getImageUrl());
        entity.setTerms(request.getTerms());
        entity.setNote(request.getNote());

        entity = couponRepository.save(entity);

        log.info("票券更新成功，ID：{}", entity.getId());

        return couponMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除票券，ID：{}", id);

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        entity.softDelete();
        couponRepository.save(entity);

        log.info("票券刪除成功，ID：{}", id);
    }

    // ========================================
    // 票券狀態操作
    // ========================================

    @Transactional
    public CouponResponse publish(String id) {
        String tenantId = TenantContext.getTenantId();

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        entity.publish();
        entity = couponRepository.save(entity);

        log.info("票券已發布，ID：{}", id);

        return couponMapper.toResponse(entity);
    }

    @Transactional
    public CouponResponse pause(String id) {
        String tenantId = TenantContext.getTenantId();

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        entity.pause();
        entity = couponRepository.save(entity);

        log.info("票券已暫停，ID：{}", id);

        return couponMapper.toResponse(entity);
    }

    @Transactional
    public CouponResponse resume(String id) {
        String tenantId = TenantContext.getTenantId();

        Coupon entity = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        entity.resume();
        entity = couponRepository.save(entity);

        log.info("票券已恢復，ID：{}", id);

        return couponMapper.toResponse(entity);
    }

    // ========================================
    // 票券發放
    // ========================================

    @Transactional
    public CouponInstanceResponse issueCoupon(String couponId, IssueCouponRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("發放票券，票券ID：{}，顧客ID：{}", couponId, request.getCustomerId());

        // 取得票券定義
        Coupon coupon = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(couponId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        // 檢查是否可發放
        if (!coupon.canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_CANNOT_ISSUE, "此票券無法發放");
        }

        // 取得顧客
        Customer customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND, "找不到指定的顧客"
                ));

        // 檢查每人限領數量
        if (coupon.getLimitPerCustomer() != null) {
            long issuedCount = couponInstanceRepository.countByCustomerAndCoupon(
                    tenantId, couponId, request.getCustomerId()
            );
            if (issuedCount >= coupon.getLimitPerCustomer()) {
                throw new BusinessException(ErrorCode.COUPON_LIMIT_EXCEEDED, "已達領取上限");
            }
        }

        // 計算有效期
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime expiresAt = null;

        if (coupon.getValidDays() != null) {
            expiresAt = validFrom.plusDays(coupon.getValidDays());
        } else if (coupon.getValidEndAt() != null) {
            expiresAt = coupon.getValidEndAt();
            if (coupon.getValidStartAt() != null) {
                validFrom = coupon.getValidStartAt();
            }
        }

        // 生成票券代碼
        String code = generateCouponCode(tenantId);

        // 建立票券實例
        CouponInstance instance = CouponInstance.builder()
                .couponId(couponId)
                .customerId(request.getCustomerId())
                .code(code)
                .status(CouponInstanceStatus.UNUSED)
                .source("manual")
                .sourceDescription(request.getSourceDescription())
                .validFrom(validFrom)
                .expiresAt(expiresAt)
                .build();
        instance.setTenantId(tenantId);

        instance = couponInstanceRepository.save(instance);

        // 更新票券已發出數量
        coupon.issue();
        couponRepository.save(coupon);

        log.info("票券發放成功，票券實例ID：{}，代碼：{}", instance.getId(), code);

        return couponMapper.toInstanceResponse(instance, coupon, customer.getDisplayName());
    }

    /**
     * 發放票券給顧客（簡化版，供 LINE Bot 使用）
     *
     * @param couponId   票券 ID
     * @param customerId 顧客 ID
     * @return 票券實例
     */
    @Transactional
    public CouponInstance issueToCustomer(String couponId, String customerId) {
        String tenantId = TenantContext.getTenantId();

        log.info("發放票券給顧客，票券ID：{}，顧客ID：{}", couponId, customerId);

        // 取得票券定義
        Coupon coupon = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(couponId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        // 檢查是否可發放
        if (!coupon.canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_CANNOT_ISSUE, "此票券無法發放");
        }

        // 檢查每人限領數量
        if (coupon.getLimitPerCustomer() != null) {
            long issuedCount = couponInstanceRepository.countByCustomerAndCoupon(
                    tenantId, couponId, customerId
            );
            if (issuedCount >= coupon.getLimitPerCustomer()) {
                throw new BusinessException(ErrorCode.COUPON_LIMIT_EXCEEDED, "已達領取上限");
            }
        }

        // 計算有效期
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime expiresAt = null;

        if (coupon.getValidDays() != null) {
            expiresAt = validFrom.plusDays(coupon.getValidDays());
        } else if (coupon.getValidEndAt() != null) {
            expiresAt = coupon.getValidEndAt();
            if (coupon.getValidStartAt() != null) {
                validFrom = coupon.getValidStartAt();
            }
        }

        // 生成票券代碼
        String code = generateCouponCode(tenantId);

        // 建立票券實例
        CouponInstance instance = CouponInstance.builder()
                .couponId(couponId)
                .customerId(customerId)
                .code(code)
                .status(CouponInstanceStatus.UNUSED)
                .source("LINE")
                .sourceDescription("透過 LINE Bot 領取")
                .validFrom(validFrom)
                .expiresAt(expiresAt)
                .build();
        instance.setTenantId(tenantId);

        instance = couponInstanceRepository.save(instance);

        // 更新票券已發出數量
        coupon.issue();
        couponRepository.save(coupon);

        log.info("票券發放成功，票券實例ID：{}，代碼：{}", instance.getId(), code);

        return instance;
    }

    // ========================================
    // 票券核銷
    // ========================================

    @Transactional
    public CouponInstanceResponse redeemCoupon(String instanceId, String orderId) {
        String tenantId = TenantContext.getTenantId();

        log.info("核銷票券，實例ID：{}，訂單ID：{}", instanceId, orderId);

        CouponInstance instance = couponInstanceRepository.findByIdAndTenantIdAndDeletedAtIsNull(instanceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        if (!instance.canUse()) {
            if (instance.getStatus() == CouponInstanceStatus.USED) {
                throw new BusinessException(ErrorCode.COUPON_USED, "票券已使用");
            } else if (instance.isExpired()) {
                throw new BusinessException(ErrorCode.COUPON_EXPIRED, "票券已過期");
            } else {
                throw new BusinessException(ErrorCode.COUPON_NOT_APPLICABLE, "票券無法使用");
            }
        }

        // 核銷票券
        instance.use(orderId);
        instance = couponInstanceRepository.save(instance);

        // 更新票券已使用數量
        Coupon coupon = couponRepository.findByIdAndTenantIdAndDeletedAtIsNull(instance.getCouponId(), tenantId)
                .orElse(null);
        if (coupon != null) {
            coupon.use();
            couponRepository.save(coupon);
        }

        log.info("票券核銷成功，實例ID：{}", instanceId);

        String customerName = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(instance.getCustomerId(), tenantId)
                .map(Customer::getDisplayName)
                .orElse(null);

        return couponMapper.toInstanceResponse(instance, coupon, customerName);
    }

    @Transactional
    public CouponInstanceResponse redeemByCode(String code, String orderId) {
        String tenantId = TenantContext.getTenantId();

        CouponInstance instance = couponInstanceRepository.findByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.COUPON_NOT_FOUND, "找不到指定的票券"
                ));

        return redeemCoupon(instance.getId(), orderId);
    }

    // ========================================
    // 顧客票券查詢
    // ========================================

    public PageResponse<CouponInstanceResponse> getCustomerCoupons(
            String customerId,
            CouponInstanceStatus status,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<CouponInstance> page = couponInstanceRepository.findByCustomer(
                tenantId, customerId, status, pageable
        );

        List<CouponInstanceResponse> content = page.getContent().stream()
                .map(ci -> {
                    Coupon coupon = couponRepository.findById(ci.getCouponId()).orElse(null);
                    String customerName = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(ci.getCustomerId(), tenantId)
                            .map(Customer::getDisplayName)
                            .orElse(null);
                    return couponMapper.toInstanceResponse(ci, coupon, customerName);
                })
                .collect(Collectors.toList());

        return PageResponse.<CouponInstanceResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public List<CouponInstanceResponse> getCustomerUsableCoupons(String customerId) {
        String tenantId = TenantContext.getTenantId();

        return couponInstanceRepository.findUsableByCustomer(tenantId, customerId, LocalDateTime.now())
                .stream()
                .map(ci -> {
                    Coupon coupon = couponRepository.findById(ci.getCouponId()).orElse(null);
                    return couponMapper.toInstanceResponse(ci, coupon, null);
                })
                .collect(Collectors.toList());
    }

    // ========================================
    // 私有方法
    // ========================================

    private String generateCouponCode(String tenantId) {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (couponInstanceRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code));
        return code;
    }
}
