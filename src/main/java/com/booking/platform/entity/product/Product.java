package com.booking.platform.entity.product;

import com.booking.platform.common.entity.BaseEntity;
import com.booking.platform.enums.ProductCategory;
import com.booking.platform.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 商品 Entity
 *
 * <p>記錄店家販售的商品
 *
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_tenant", columnList = "tenant_id, deleted_at"),
                @Index(name = "idx_products_status", columnList = "tenant_id, status, deleted_at"),
                @Index(name = "idx_products_category", columnList = "tenant_id, category, deleted_at"),
                @Index(name = "idx_products_sku", columnList = "tenant_id, sku", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Version
    private Long version;

    /**
     * 商品名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 商品描述
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 商品分類
     */
    @Column(name = "category", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    /**
     * 商品狀態
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    /**
     * 商品編號 (SKU)
     */
    @Column(name = "sku", length = 50)
    private String sku;

    /**
     * 條碼
     */
    @Column(name = "barcode", length = 50)
    private String barcode;

    /**
     * 售價
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 成本價
     */
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    /**
     * 庫存數量
     */
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    /**
     * 安全庫存量（低於此數量會提醒）
     */
    @Column(name = "safety_stock")
    private Integer safetyStock;

    /**
     * 商品圖片 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 品牌
     */
    @Column(name = "brand", length = 100)
    private String brand;

    /**
     * 規格
     */
    @Column(name = "specification", length = 200)
    private String specification;

    /**
     * 單位
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 是否追蹤庫存
     */
    @Column(name = "track_inventory")
    private Boolean trackInventory;

    /**
     * 是否可單獨販售
     */
    @Column(name = "is_sellable")
    private Boolean isSellable;

    /**
     * 排序順序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 銷售數量
     */
    @Column(name = "sold_quantity")
    private Integer soldQuantity;

    /**
     * 備註
     */
    @Column(name = "note", length = 500)
    private String note;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = ProductStatus.DRAFT;
        }
        if (this.stockQuantity == null) {
            this.stockQuantity = 0;
        }
        if (this.soldQuantity == null) {
            this.soldQuantity = 0;
        }
        if (this.trackInventory == null) {
            this.trackInventory = true;
        }
        if (this.isSellable == null) {
            this.isSellable = true;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 上架商品
     */
    public void putOnSale() {
        this.status = ProductStatus.ON_SALE;
    }

    /**
     * 下架商品
     */
    public void takeOffShelf() {
        this.status = ProductStatus.OFF_SHELF;
    }

    /**
     * 標記缺貨
     */
    public void markOutOfStock() {
        this.status = ProductStatus.OUT_OF_STOCK;
    }

    /**
     * 增加庫存
     */
    public void addStock(int quantity) {
        this.stockQuantity = (this.stockQuantity != null ? this.stockQuantity : 0) + quantity;
        // 如果之前缺貨，自動改為上架
        if (this.status == ProductStatus.OUT_OF_STOCK && this.stockQuantity > 0) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    /**
     * 減少庫存（銷售）
     */
    public boolean reduceStock(int quantity) {
        if (!this.trackInventory) {
            this.soldQuantity = (this.soldQuantity != null ? this.soldQuantity : 0) + quantity;
            return true;
        }
        if (this.stockQuantity == null || this.stockQuantity < quantity) {
            return false;
        }
        this.stockQuantity -= quantity;
        this.soldQuantity = (this.soldQuantity != null ? this.soldQuantity : 0) + quantity;
        // 庫存歸零自動標記缺貨
        if (this.stockQuantity <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
        return true;
    }

    /**
     * 檢查是否低於安全庫存
     */
    public boolean isLowStock() {
        if (!this.trackInventory || this.safetyStock == null) {
            return false;
        }
        return this.stockQuantity != null && this.stockQuantity <= this.safetyStock;
    }

    /**
     * 檢查是否可販售
     */
    public boolean canSell() {
        if (this.status != ProductStatus.ON_SALE) {
            return false;
        }
        if (!this.isSellable) {
            return false;
        }
        if (this.trackInventory && (this.stockQuantity == null || this.stockQuantity <= 0)) {
            return false;
        }
        return true;
    }
}
