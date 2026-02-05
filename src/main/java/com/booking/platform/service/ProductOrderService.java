package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.ProductOrderResponse;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.line.LineUser;
import com.booking.platform.entity.product.Product;
import com.booking.platform.entity.product.ProductOrder;
import com.booking.platform.enums.InventoryActionType;
import com.booking.platform.enums.ProductOrderStatus;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.line.LineUserRepository;
import com.booking.platform.repository.ProductOrderRepository;
import com.booking.platform.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品訂單服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductOrderService {

    private final ProductOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final LineUserRepository lineUserRepository;
    private final InventoryService inventoryService;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得訂單列表（分頁）
     */
    public PageResponse<ProductOrderResponse> getList(ProductOrderStatus status, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<ProductOrder> page;
        if (status != null) {
            page = orderRepository.findByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    tenantId, status, pageable);
        } else {
            page = orderRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, pageable);
        }

        List<ProductOrderResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductOrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 取得訂單詳情
     */
    public ProductOrderResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        ProductOrder order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SYS_INTERNAL_ERROR, "找不到指定的訂單"
                ));

        return toResponse(order);
    }

    /**
     * 取得待處理訂單數量
     */
    public long getPendingCount() {
        String tenantId = TenantContext.getTenantId();
        return orderRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, ProductOrderStatus.PENDING);
    }

    /**
     * 取得今日訂單統計
     */
    public TodayStats getTodayStats() {
        String tenantId = TenantContext.getTenantId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long count = orderRepository.countTodayOrders(tenantId, startOfDay);
        BigDecimal revenue = orderRepository.sumTodayRevenue(tenantId, startOfDay);

        return new TodayStats(count, revenue);
    }

    // ========================================
    // 建立訂單（由 LINE 購買觸發）
    // ========================================

    /**
     * 建立訂單（LINE 購買）
     */
    @Transactional
    public ProductOrderResponse createFromLine(
            String tenantId,
            String lineUserId,
            String productId,
            int quantity
    ) {
        log.info("建立 LINE 商品訂單，租戶：{}，LINE用戶：{}，商品：{}，數量：{}",
                tenantId, lineUserId, productId, quantity);

        // ========================================
        // 1. 取得商品資訊
        // ========================================
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"));

        // 檢查庫存
        if (product.getTrackInventory() != null && product.getTrackInventory()) {
            if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT, "商品庫存不足");
            }
        }

        // ========================================
        // 2. 取得顧客資訊
        // ========================================
        String customerId = null;
        String customerName = null;

        LineUser lineUser = lineUserRepository.findByTenantIdAndLineUserIdAndDeletedAtIsNull(tenantId, lineUserId)
                .orElse(null);

        if (lineUser != null && lineUser.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                    lineUser.getCustomerId(), tenantId).orElse(null);
            if (customer != null) {
                customerId = customer.getId();
                customerName = customer.getName();
            }
        }

        if (customerName == null && lineUser != null) {
            customerName = lineUser.getDisplayName();
        }

        // ========================================
        // 3. 產生訂單編號
        // ========================================
        String orderNo = generateOrderNo(tenantId);

        // ========================================
        // 4. 建立訂單
        // ========================================
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        ProductOrder order = ProductOrder.builder()
                .orderNo(orderNo)
                .customerId(customerId)
                .customerName(customerName)
                .lineUserId(lineUserId)
                .productId(productId)
                .productName(product.getName())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .status(ProductOrderStatus.PENDING)
                .build();

        order.setTenantId(tenantId);
        order = orderRepository.save(order);

        // ========================================
        // 5. 扣減庫存
        // ========================================
        if (product.getTrackInventory() != null && product.getTrackInventory()) {
            inventoryService.recordMovement(
                    tenantId,
                    productId,
                    product.getName(),
                    InventoryActionType.SALE_OUT,
                    -quantity,
                    "LINE 商品訂單 " + orderNo,
                    order.getId(),
                    null,
                    customerName != null ? customerName : "LINE 顧客"
            );
        }

        log.info("LINE 商品訂單建立成功，訂單編號：{}", orderNo);

        return toResponse(order);
    }

    // ========================================
    // 訂單操作
    // ========================================

    /**
     * 確認訂單
     */
    @Transactional
    public ProductOrderResponse confirm(String id) {
        String tenantId = TenantContext.getTenantId();

        ProductOrder order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SYS_INTERNAL_ERROR, "找不到指定的訂單"
                ));

        if (order.getStatus() != ProductOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "只能確認待處理的訂單");
        }

        order.confirm();
        order = orderRepository.save(order);

        log.info("訂單已確認，訂單編號：{}", order.getOrderNo());

        return toResponse(order);
    }

    /**
     * 完成訂單（取貨）
     */
    @Transactional
    public ProductOrderResponse complete(String id) {
        String tenantId = TenantContext.getTenantId();

        ProductOrder order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SYS_INTERNAL_ERROR, "找不到指定的訂單"
                ));

        if (order.getStatus() != ProductOrderStatus.CONFIRMED && order.getStatus() != ProductOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "只能完成已確認或待處理的訂單");
        }

        order.pickup();
        order = orderRepository.save(order);

        log.info("訂單已完成，訂單編號：{}", order.getOrderNo());

        return toResponse(order);
    }

    /**
     * 取消訂單
     */
    @Transactional
    public ProductOrderResponse cancel(String id, String reason) {
        String tenantId = TenantContext.getTenantId();

        ProductOrder order = orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SYS_INTERNAL_ERROR, "找不到指定的訂單"
                ));

        if (order.getStatus() == ProductOrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "已完成的訂單無法取消");
        }

        if (order.getStatus() == ProductOrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "訂單已被取消");
        }

        order.cancel(reason);
        order = orderRepository.save(order);

        // 回補庫存
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                order.getProductId(), tenantId).orElse(null);

        if (product != null && product.getTrackInventory() != null && product.getTrackInventory()) {
            inventoryService.recordMovement(
                    tenantId,
                    order.getProductId(),
                    order.getProductName(),
                    InventoryActionType.ORDER_CANCEL,
                    order.getQuantity(),
                    "訂單取消回補 " + order.getOrderNo() + (reason != null ? " - " + reason : ""),
                    order.getId(),
                    null,
                    "系統"
            );
        }

        log.info("訂單已取消，訂單編號：{}", order.getOrderNo());

        return toResponse(order);
    }

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 產生訂單編號
     */
    private String generateOrderNo(String tenantId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = orderRepository.countOrdersToday(tenantId, startOfDay);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "P" + dateStr + String.format("%04d", count + 1);
    }

    /**
     * 轉換為 Response DTO
     */
    private ProductOrderResponse toResponse(ProductOrder order) {
        return ProductOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .lineUserId(order.getLineUserId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .unitPrice(order.getUnitPrice())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDescription(order.getStatus().getDescription())
                .note(order.getNote())
                .pickupAt(order.getPickupAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 今日統計
     */
    public record TodayStats(long orderCount, BigDecimal revenue) {}
}
