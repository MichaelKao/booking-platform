/**
 * 租戶檢查片段
 * 
 * 使用方式：確保資料屬於當前租戶
 */

// Service 中檢查資料歸屬
public XxxResponse getDetail(String id) {
    // 取得當前租戶
    String tenantId = TenantContext.getTenantId();
    
    // 查詢時加上租戶條件
    XxxEntity entity = xxxRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.XXX_NOT_FOUND,
                    "找不到指定的資料"
            ));
    
    return xxxMapper.toResponse(entity);
}

// Repository 中的查詢方法
Optional<XxxEntity> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);