package com.booking.platform.service;

import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.InventoryLogResponse;
import com.booking.platform.entity.product.InventoryLog;
import com.booking.platform.entity.product.Product;
import com.booking.platform.enums.InventoryActionType;
import com.booking.platform.repository.InventoryLogRepository;
import com.booking.platform.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 庫存異動服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InventoryService {

    private final InventoryLogRepository logRepository;
    private final ProductRepository productRepository;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得所有異動記錄（分頁）
     */
    public PageResponse<InventoryLogResponse> getList(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<InventoryLog> page = logRepository
                .findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, pageable);

        List<InventoryLogResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<InventoryLogResponse>builder()
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
     * 取得指定商品的異動記錄
     */
    public List<InventoryLogResponse> getByProduct(String productId) {
        String tenantId = TenantContext.getTenantId();

        return logRepository.findByTenantIdAndProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, productId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 取得指定商品的異動記錄（分頁）
     */
    public PageResponse<InventoryLogResponse> getByProductPaged(String productId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<InventoryLog> page = logRepository
                .findByTenantIdAndProductIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, productId, pageable);

        List<InventoryLogResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<InventoryLogResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ========================================
    // 記錄異動
    // ========================================

    /**
     * 記錄庫存異動
     */
    @Transactional
    public InventoryLogResponse recordMovement(
            String tenantId,
            String productId,
            String productName,
            InventoryActionType actionType,
            int quantity,
            String reason,
            String orderId,
            String operatorId,
            String operatorName
    ) {
        log.info("記錄庫存異動，租戶：{}，商品：{}，類型：{}，數量：{}",
                tenantId, productId, actionType, quantity);

        // 取得商品當前庫存
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
                .orElse(null);

        int quantityBefore = 0;
        if (product != null && product.getStockQuantity() != null) {
            quantityBefore = product.getStockQuantity();
        }

        // 更新商品庫存
        if (product != null) {
            int newStock = quantityBefore + quantity;
            if (newStock < 0) {
                log.warn("庫存將為負數，已截斷為 0，商品：{}，計算值：{}", productId, newStock);
            }
            product.setStockQuantity(Math.max(0, newStock));
            productRepository.save(product);
        }

        int quantityAfter = product != null && product.getStockQuantity() != null
                ? product.getStockQuantity() : quantityBefore + quantity;

        // 記錄異動
        InventoryLog logEntry = InventoryLog.builder()
                .productId(productId)
                .productName(productName)
                .actionType(actionType)
                .quantity(quantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .reason(reason)
                .orderId(orderId)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build();

        logEntry.setTenantId(tenantId);
        logEntry = logRepository.save(logEntry);

        log.info("庫存異動記錄完成，ID：{}，異動後庫存：{}", logEntry.getId(), quantityAfter);

        return toResponse(logEntry);
    }

    /**
     * 記錄手動調整（簡化版，供 ProductService 呼叫）
     */
    @Transactional
    public void recordManualAdjustment(
            String productId,
            String productName,
            int adjustment,
            int quantityBefore,
            int quantityAfter,
            String reason,
            String operatorName
    ) {
        String tenantId = TenantContext.getTenantId();

        InventoryActionType actionType = determineActionType(reason);

        InventoryLog logEntry = InventoryLog.builder()
                .productId(productId)
                .productName(productName)
                .actionType(actionType)
                .quantity(adjustment)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .reason(reason)
                .operatorName(operatorName != null ? operatorName : "店家")
                .build();

        logEntry.setTenantId(tenantId);
        logRepository.save(logEntry);

        log.info("庫存手動調整記錄完成，商品：{}，調整：{}，原因：{}", productName, adjustment, reason);
    }

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 根據原因判斷異動類型
     */
    private InventoryActionType determineActionType(String reason) {
        if (reason == null || reason.isEmpty()) {
            return InventoryActionType.ADJUSTMENT;
        }

        String lowerReason = reason.toLowerCase();

        if (lowerReason.contains("進貨") || lowerReason.contains("入庫")) {
            return InventoryActionType.STOCK_IN;
        }
        if (lowerReason.contains("銷售") || lowerReason.contains("出貨")) {
            return InventoryActionType.SALE_OUT;
        }
        if (lowerReason.contains("盤點")) {
            return InventoryActionType.INVENTORY_CHECK;
        }
        if (lowerReason.contains("損耗") || lowerReason.contains("報廢")) {
            return InventoryActionType.DAMAGE;
        }
        if (lowerReason.contains("退貨")) {
            return InventoryActionType.RETURN_IN;
        }

        return InventoryActionType.ADJUSTMENT;
    }

    /**
     * 轉換為 Response DTO
     */
    private InventoryLogResponse toResponse(InventoryLog log) {
        return InventoryLogResponse.builder()
                .id(log.getId())
                .productId(log.getProductId())
                .productName(log.getProductName())
                .actionType(log.getActionType())
                .actionTypeDescription(log.getActionType().getDescription())
                .quantity(log.getQuantity())
                .quantityBefore(log.getQuantityBefore())
                .quantityAfter(log.getQuantityAfter())
                .reason(log.getReason())
                .orderId(log.getOrderId())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
