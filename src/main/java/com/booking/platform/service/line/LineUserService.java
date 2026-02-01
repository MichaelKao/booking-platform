package com.booking.platform.service.line;

import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.dto.request.CreateCustomerRequest;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.enums.CustomerStatus;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.line.LineUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * LINE 用戶服務
 *
 * <p>管理 LINE 用戶資料和與顧客的關聯
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LineUserService {

    // ========================================
    // 依賴注入
    // ========================================

    private final LineUserRepository lineUserRepository;
    private final CustomerRepository customerRepository;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 根據 LINE User ID 查詢用戶
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return LINE 用戶（可能為空）
     */
    public Optional<LineUser> findByLineUserId(String tenantId, String lineUserId) {
        return lineUserRepository.findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId);
    }

    /**
     * 根據顧客 ID 查詢 LINE 用戶
     *
     * @param tenantId   租戶 ID
     * @param customerId 顧客 ID
     * @return LINE 用戶（可能為空）
     */
    public Optional<LineUser> findByCustomerId(String tenantId, String customerId) {
        return lineUserRepository.findByTenantIdAndCustomerIdAndDeletedAtIsNull(tenantId, customerId);
    }

    /**
     * 取得或建立 LINE 用戶
     *
     * @param tenantId      租戶 ID
     * @param lineUserId    LINE User ID
     * @param displayName   顯示名稱
     * @param pictureUrl    頭像 URL
     * @param statusMessage 狀態訊息
     * @return LINE 用戶
     */
    @Transactional
    public LineUser getOrCreateUser(
            String tenantId,
            String lineUserId,
            String displayName,
            String pictureUrl,
            String statusMessage
    ) {
        // ========================================
        // 1. 查詢現有用戶
        // ========================================

        Optional<LineUser> existingUser = lineUserRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId);

        if (existingUser.isPresent()) {
            // 更新個人資料
            LineUser user = existingUser.get();
            user.updateProfile(displayName, pictureUrl, statusMessage);
            user.incrementInteractionCount();
            return lineUserRepository.save(user);
        }

        // ========================================
        // 2. 建立新用戶
        // ========================================

        log.info("建立新 LINE 用戶，租戶：{}，LINE User ID：{}", tenantId, lineUserId);

        LineUser newUser = LineUser.builder()
                .lineUserId(lineUserId)
                .displayName(displayName)
                .pictureUrl(pictureUrl)
                .statusMessage(statusMessage)
                .isFollowed(true)
                .followedAt(LocalDateTime.now())
                .interactionCount(1)
                .bookingCount(0)
                .lastInteractionAt(LocalDateTime.now())
                .build();

        newUser.setTenantId(tenantId);

        newUser = lineUserRepository.save(newUser);

        // ========================================
        // 3. 自動建立關聯的顧客
        // ========================================

        createCustomerForLineUser(tenantId, newUser);

        return newUser;
    }

    // ========================================
    // 寫入方法
    // ========================================

    /**
     * 處理追蹤事件
     *
     * @param tenantId      租戶 ID
     * @param lineUserId    LINE User ID
     * @param displayName   顯示名稱
     * @param pictureUrl    頭像 URL
     * @param statusMessage 狀態訊息
     * @return LINE 用戶
     */
    @Transactional
    public LineUser handleFollow(
            String tenantId,
            String lineUserId,
            String displayName,
            String pictureUrl,
            String statusMessage
    ) {
        log.info("處理追蹤事件，租戶：{}，LINE User ID：{}", tenantId, lineUserId);

        // 查詢或建立用戶
        LineUser user = getOrCreateUser(tenantId, lineUserId, displayName, pictureUrl, statusMessage);
        user.handleFollow();

        return lineUserRepository.save(user);
    }

    /**
     * 處理取消追蹤事件
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return LINE 用戶
     */
    @Transactional
    public LineUser handleUnfollow(String tenantId, String lineUserId) {
        log.info("處理取消追蹤事件，租戶：{}，LINE User ID：{}", tenantId, lineUserId);

        Optional<LineUser> userOpt = lineUserRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId);

        if (userOpt.isPresent()) {
            LineUser user = userOpt.get();
            user.handleUnfollow();

            // 同時更新顧客的 LINE 封鎖狀態
            if (user.getCustomerId() != null) {
                customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(user.getCustomerId(), tenantId)
                        .ifPresent(customer -> {
                            customer.setIsLineBlocked(true);
                            customerRepository.save(customer);
                        });
            }

            return lineUserRepository.save(user);
        }

        return null;
    }

    /**
     * 增加預約次數
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     */
    @Transactional
    public void incrementBookingCount(String tenantId, String lineUserId) {
        lineUserRepository.findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .ifPresent(user -> {
                    user.incrementBookingCount();
                    lineUserRepository.save(user);
                });
    }

    /**
     * 綁定顧客
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param customerId 顧客 ID
     */
    @Transactional
    public void bindCustomer(String tenantId, String lineUserId, String customerId) {
        LineUser user = lineUserRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.LINE_USER_NOT_FOUND, "找不到 LINE 用戶"
                ));

        user.bindCustomer(customerId);
        lineUserRepository.save(user);

        // 同時更新顧客的 LINE 資訊
        customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(customerId, tenantId)
                .ifPresent(customer -> {
                    customer.setLineUserId(lineUserId);
                    customer.setLineDisplayName(user.getDisplayName());
                    customer.setLinePictureUrl(user.getPictureUrl());
                    customerRepository.save(customer);
                });

        log.info("LINE 用戶綁定顧客成功，租戶：{}，LINE User：{}，顧客：{}",
                tenantId, lineUserId, customerId);
    }

    /**
     * 取得 LINE 用戶的顧客 ID
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 顧客 ID（可能為 null）
     */
    public String getCustomerId(String tenantId, String lineUserId) {
        return lineUserRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .map(LineUser::getCustomerId)
                .orElse(null);
    }

    /**
     * 取得或建立 LINE 用戶的顧客 ID
     *
     * <p>如果 LINE 用戶沒有關聯的顧客，則自動建立一個
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 顧客 ID
     */
    @Transactional
    public String getOrCreateCustomerId(String tenantId, String lineUserId) {
        LineUser lineUser = lineUserRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .orElse(null);

        if (lineUser == null) {
            log.warn("找不到 LINE 用戶，租戶：{}，LINE User：{}", tenantId, lineUserId);
            return null;
        }

        // 如果已有顧客 ID，直接返回
        if (lineUser.getCustomerId() != null) {
            return lineUser.getCustomerId();
        }

        // 否則建立顧客
        createCustomerForLineUser(tenantId, lineUser);
        return lineUser.getCustomerId();
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 為 LINE 用戶建立關聯的顧客
     */
    private void createCustomerForLineUser(String tenantId, LineUser lineUser) {
        // 檢查是否已有綁定的顧客
        if (lineUser.getCustomerId() != null) {
            return;
        }

        // 檢查是否已有同 LINE User ID 的顧客
        Optional<Customer> existingCustomer = customerRepository
                .findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUser.getLineUserId());

        if (existingCustomer.isPresent()) {
            // 綁定現有顧客
            lineUser.setCustomerId(existingCustomer.get().getId());
            lineUserRepository.save(lineUser);
            return;
        }

        // 建立新顧客
        Customer customer = Customer.builder()
                .lineUserId(lineUser.getLineUserId())
                .lineDisplayName(lineUser.getDisplayName())
                .linePictureUrl(lineUser.getPictureUrl())
                .name(lineUser.getDisplayName())
                .status(CustomerStatus.ACTIVE)
                .build();

        customer.setTenantId(tenantId);
        customer = customerRepository.save(customer);

        // 綁定到 LINE 用戶
        lineUser.setCustomerId(customer.getId());
        lineUserRepository.save(lineUser);

        log.info("為 LINE 用戶建立顧客，租戶：{}，顧客 ID：{}", tenantId, customer.getId());
    }
}
