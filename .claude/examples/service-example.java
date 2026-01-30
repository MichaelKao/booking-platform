/**
 * Service 範例
 * 
 * <p>所有 Service 都要遵循此範例的風格
 * 
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExampleService {

    // ========================================
    // 依賴注入
    // ========================================
    
    private final ExampleRepository exampleRepository;
    private final CacheService cacheService;
    private final AuditLogService auditLogService;
    private final ExampleMapper exampleMapper;

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
    public PageResponse<ExampleListItemResponse> getList(
            ExampleStatus status,
            String keyword,
            Pageable pageable
    ) {
        // ========================================
        // 1. 取得當前租戶
        // ========================================
        
        String tenantId = TenantContext.getTenantId();
        
        // ========================================
        // 2. 執行查詢
        // ========================================
        
        Page<ExampleListItemResponse> page = exampleRepository.findListItems(
                tenantId, status, keyword, pageable
        );
        
        // ========================================
        // 3. 返回結果
        // ========================================
        
        return PageResponse.from(page);
    }

    /**
     * 查詢詳情
     * 
     * @param id 資料 ID
     * @return 詳情資料
     */
    public ExampleDetailResponse getDetail(String id) {
        
        // ========================================
        // 1. 取得當前租戶
        // ========================================
        
        String tenantId = TenantContext.getTenantId();
        
        // ========================================
        // 2. 查詢資料
        // ========================================
        
        ExampleEntity entity = exampleRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EXAMPLE_NOT_FOUND, "找不到指定的資料"
                ));
        
        // ========================================
        // 3. 返回結果
        // ========================================
        
        return exampleMapper.toDetailResponse(entity);
    }

    // ========================================
    // 寫入方法
    // ========================================

    /**
     * 建立資料
     * 
     * @param request 建立請求
     * @return 建立結果
     */
    @Transactional
    public ExampleResponse create(CreateExampleRequest request) {
        
        // ========================================
        // 1. 取得當前租戶
        // ========================================
        
        String tenantId = TenantContext.getTenantId();
        
        log.info("建立 Example，租戶：{}，參數：{}", tenantId, request);
        
        // ========================================
        // 2. 驗證業務規則
        // ========================================
        
        // 檢查名稱是否重複
        boolean nameExists = exampleRepository.existsByTenantIdAndNameAndDeletedAtIsNull(
                tenantId, request.getName()
        );
        
        if (nameExists) {
            throw new BusinessException(
                    ErrorCode.EXAMPLE_NAME_DUPLICATE,
                    "名稱已存在，請使用其他名稱"
            );
        }
        
        // ========================================
        // 3. 建立 Entity
        // ========================================
        
        ExampleEntity entity = new ExampleEntity();
        
        // 設定租戶 ID
        entity.setTenantId(tenantId);
        
        // 設定欄位
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(ExampleStatus.ACTIVE);
        entity.setIsActive(true);
        
        // ========================================
        // 4. 儲存到資料庫
        // ========================================
        
        entity = exampleRepository.save(entity);
        
        // ========================================
        // 5. 記錄稽核日誌
        // ========================================
        
        auditLogService.log(AuditAction.CREATE, "Example", entity.getId(), null, entity);
        
        // ========================================
        // 6. 清除快取
        // ========================================
        
        cacheService.evictExampleListCache(tenantId);
        
        // ========================================
        // 7. 返回結果
        // ========================================
        
        log.info("Example 建立成功，ID：{}", entity.getId());
        
        return exampleMapper.toResponse(entity);
    }

    /**
     * 更新資料
     * 
     * @param id 資料 ID
     * @param request 更新請求
     * @return 更新結果
     */
    @Transactional
    public ExampleResponse update(String id, UpdateExampleRequest request) {
        
        // ========================================
        // 1. 取得當前租戶
        // ========================================
        
        String tenantId = TenantContext.getTenantId();
        
        log.info("更新 Example，ID：{}，參數：{}", id, request);
        
        // ========================================
        // 2. 查詢現有資料
        // ========================================
        
        ExampleEntity entity = exampleRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EXAMPLE_NOT_FOUND, "找不到指定的資料"
                ));
        
        // ========================================
        // 3. 更新欄位
        // ========================================
        
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        
        // ========================================
        // 4. 儲存更新
        // ========================================
        
        entity = exampleRepository.save(entity);
        
        // ========================================
        // 5. 記錄稽核日誌
        // ========================================
        
        auditLogService.log(AuditAction.UPDATE, "Example", entity.getId(), null, entity);
        
        // ========================================
        // 6. 清除快取
        // ========================================
        
        cacheService.evictExampleListCache(tenantId);
        
        // ========================================
        // 7. 返回結果
        // ========================================
        
        log.info("Example 更新成功，ID：{}", entity.getId());
        
        return exampleMapper.toResponse(entity);
    }

    /**
     * 刪除資料（軟刪除）
     * 
     * @param id 資料 ID
     */
    @Transactional
    public void delete(String id) {
        
        // ========================================
        // 1. 取得當前租戶
        // ========================================
        
        String tenantId = TenantContext.getTenantId();
        
        log.info("刪除 Example，ID：{}", id);
        
        // ========================================
        // 2. 查詢現有資料
        // ========================================
        
        ExampleEntity entity = exampleRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.EXAMPLE_NOT_FOUND, "找不到指定的資料"
                ));
        
        // ========================================
        // 3. 執行軟刪除
        // ========================================
        
        entity.softDelete();
        exampleRepository.save(entity);
        
        // ========================================
        // 4. 記錄稽核日誌
        // ========================================
        
        auditLogService.log(AuditAction.DELETE, "Example", entity.getId(), entity, null);
        
        // ========================================
        // 5. 清除快取
        // ========================================
        
        cacheService.evictExampleListCache(tenantId);
        
        log.info("Example 刪除成功，ID：{}", id);
    }
}