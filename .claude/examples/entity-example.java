/**
 * Entity 範例
 * 
 * <p>所有 Entity 都要遵循此範例的風格
 * 
 * <p>資料表：examples
 * 
 * <p>索引設計：
 * <ul>
 *   <li>idx_examples_tenant_status - 列表查詢用</li>
 *   <li>idx_examples_tenant_deleted - 軟刪除過濾用</li>
 * </ul>
 * 
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "examples",
        indexes = {
                // 列表查詢用索引
                @Index(name = "idx_examples_tenant_status", columnList = "tenant_id, status, created_at"),
                // 軟刪除過濾用索引
                @Index(name = "idx_examples_tenant_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExampleEntity extends BaseEntity {
    
    // ========================================
    // 基本資料欄位
    // ========================================
    
    /**
     * 名稱
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /**
     * 描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    // ========================================
    // 狀態欄位
    // ========================================
    
    /**
     * 狀態
     * 
     * @see ExampleStatus
     */
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ExampleStatus status;
    
    /**
     * 是否啟用
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    // ========================================
    // 關聯欄位
    // ========================================
    
    /**
     * 分類 ID
     */
    @Column(name = "category_id", length = 36)
    private String categoryId;
    
    // ========================================
    // 數值欄位
    // ========================================
    
    /**
     * 金額（精度：10 位整數，2 位小數）
     */
    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * 排序權重（數字越小排越前面）
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    // ========================================
    // 時間欄位
    // ========================================
    
    /**
     * 開始時間
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    /**
     * 結束時間
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    // ========================================
    // 業務方法
    // ========================================
    
    /**
     * 檢查是否可用
     * 
     * @return true 表示可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(this.isActive) && !this.isDeleted();
    }
}