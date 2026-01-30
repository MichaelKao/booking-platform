package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreateProductRequest;
import com.booking.platform.dto.response.ProductResponse;
import com.booking.platform.entity.product.Product;
import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import com.booking.platform.mapper.ProductMapper;
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
 * 商品服務
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // ========================================
    // 查詢方法
    // ========================================

    public PageResponse<ProductResponse> getList(
            ProductStatus status,
            ProductCategory category,
            String keyword,
            Pageable pageable
    ) {
        String tenantId = TenantContext.getTenantId();

        Page<Product> page = productRepository.findByTenantIdAndFilters(
                tenantId, status, category, keyword, pageable
        );

        List<ProductResponse> content = page.getContent().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public ProductResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        return productMapper.toResponse(entity);
    }

    public List<ProductResponse> getOnSaleProducts() {
        String tenantId = TenantContext.getTenantId();

        return productRepository.findOnSaleByTenantId(tenantId)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getLowStockProducts() {
        String tenantId = TenantContext.getTenantId();

        return productRepository.findLowStockByTenantId(tenantId)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getByCategory(ProductCategory category) {
        String tenantId = TenantContext.getTenantId();

        return productRepository.findByCategoryAndTenantId(tenantId, category)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 寫入方法
    // ========================================

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立商品，租戶：{}，名稱：{}", tenantId, request.getName());

        // 檢查名稱是否重複
        if (productRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.getName())) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_DUPLICATE, "商品名稱已存在");
        }

        // 檢查 SKU 是否重複
        if (request.getSku() != null && !request.getSku().isBlank()) {
            if (productRepository.existsByTenantIdAndSkuAndDeletedAtIsNull(tenantId, request.getSku())) {
                throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATE, "商品編號已存在");
            }
        }

        Product entity = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(ProductStatus.DRAFT)
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .price(request.getPrice())
                .cost(request.getCost())
                .stockQuantity(request.getStockQuantity())
                .safetyStock(request.getSafetyStock())
                .imageUrl(request.getImageUrl())
                .brand(request.getBrand())
                .specification(request.getSpecification())
                .unit(request.getUnit())
                .trackInventory(request.getTrackInventory())
                .isSellable(request.getIsSellable())
                .sortOrder(request.getSortOrder())
                .note(request.getNote())
                .build();

        entity.setTenantId(tenantId);
        entity = productRepository.save(entity);

        log.info("商品建立成功，ID：{}", entity.getId());

        return productMapper.toResponse(entity);
    }

    @Transactional
    public ProductResponse update(String id, CreateProductRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("更新商品，ID：{}", id);

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        // 檢查名稱是否重複
        if (productRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                tenantId, request.getName(), id)) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_DUPLICATE, "商品名稱已存在");
        }

        // 檢查 SKU 是否重複
        if (request.getSku() != null && !request.getSku().isBlank()) {
            if (productRepository.existsByTenantIdAndSkuAndIdNotAndDeletedAtIsNull(
                    tenantId, request.getSku(), id)) {
                throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATE, "商品編號已存在");
            }
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setSku(request.getSku());
        entity.setBarcode(request.getBarcode());
        entity.setPrice(request.getPrice());
        entity.setCost(request.getCost());
        entity.setStockQuantity(request.getStockQuantity());
        entity.setSafetyStock(request.getSafetyStock());
        entity.setImageUrl(request.getImageUrl());
        entity.setBrand(request.getBrand());
        entity.setSpecification(request.getSpecification());
        entity.setUnit(request.getUnit());
        entity.setTrackInventory(request.getTrackInventory());
        entity.setIsSellable(request.getIsSellable());
        entity.setSortOrder(request.getSortOrder());
        entity.setNote(request.getNote());

        entity = productRepository.save(entity);

        log.info("商品更新成功，ID：{}", entity.getId());

        return productMapper.toResponse(entity);
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.getTenantId();

        log.info("刪除商品，ID：{}", id);

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        entity.softDelete();
        productRepository.save(entity);

        log.info("商品刪除成功，ID：{}", id);
    }

    // ========================================
    // 狀態操作
    // ========================================

    @Transactional
    public ProductResponse putOnSale(String id) {
        String tenantId = TenantContext.getTenantId();

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        entity.putOnSale();
        entity = productRepository.save(entity);

        log.info("商品已上架，ID：{}", id);

        return productMapper.toResponse(entity);
    }

    @Transactional
    public ProductResponse takeOffShelf(String id) {
        String tenantId = TenantContext.getTenantId();

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        entity.takeOffShelf();
        entity = productRepository.save(entity);

        log.info("商品已下架，ID：{}", id);

        return productMapper.toResponse(entity);
    }

    // ========================================
    // 庫存操作
    // ========================================

    @Transactional
    public ProductResponse adjustStock(String id, int adjustment, String reason) {
        String tenantId = TenantContext.getTenantId();

        log.info("調整商品庫存，ID：{}，調整量：{}，原因：{}", id, adjustment, reason);

        Product entity = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, "找不到指定的商品"
                ));

        if (adjustment > 0) {
            entity.addStock(adjustment);
        } else if (adjustment < 0) {
            int currentStock = entity.getStockQuantity() != null ? entity.getStockQuantity() : 0;
            if (currentStock + adjustment < 0) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT, "庫存不足");
            }
            entity.setStockQuantity(currentStock + adjustment);
        }

        entity = productRepository.save(entity);

        log.info("商品庫存調整成功，ID：{}，新庫存：{}", id, entity.getStockQuantity());

        return productMapper.toResponse(entity);
    }
}
