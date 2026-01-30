// ========================================
// Request DTO
// ========================================

/**
 * 建立請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExampleRequest {
    
    /**
     * 名稱（必填）
     */
    @NotBlank(message = "名稱不能為空")
    @Size(max = 100, message = "名稱長度不能超過 100 字")
    private String name;
    
    /**
     * 描述（選填）
     */
    @Size(max = 500, message = "描述長度不能超過 500 字")
    private String description;
    
    /**
     * 金額（選填）
     */
    @DecimalMin(value = "0", message = "金額不能小於 0")
    private BigDecimal amount;
}

/**
 * 更新請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExampleRequest {
    
    @NotBlank(message = "名稱不能為空")
    @Size(max = 100, message = "名稱長度不能超過 100 字")
    private String name;
    
    @Size(max = 500, message = "描述長度不能超過 500 字")
    private String description;
    
    private Boolean isActive;
}

// ========================================
// Response DTO
// ========================================

/**
 * 回應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleResponse {
    
    private String id;
    private String name;
    private String description;
    private ExampleStatus status;
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

/**
 * 列表項目 DTO（用於投影查詢）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExampleListItemResponse {
    
    private String id;
    private String name;
    private ExampleStatus status;
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

/**
 * 詳情 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleDetailResponse {
    
    private String id;
    private String name;
    private String description;
    private ExampleStatus status;
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}