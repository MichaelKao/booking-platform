/**
 * 快取模式片段
 * 
 * 使用方式：在 Service 中加入快取
 */

// 讀取時使用快取
public XxxResponse getDetail(String id) {
    String tenantId = TenantContext.getTenantId();
    String cacheKey = "xxx:" + tenantId + ":" + id;
    
    // 嘗試從快取取得
    XxxResponse cached = cacheService.get(cacheKey, XxxResponse.class);
    if (cached != null) {
        return cached;
    }
    
    // 查詢資料庫
    XxxEntity entity = xxxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(...));
    
    XxxResponse response = xxxMapper.toResponse(entity);
    
    // 存入快取（TTL 30 分鐘）
    cacheService.set(cacheKey, response, Duration.ofMinutes(30));
    
    return response;
}

// 寫入時清除快取
@Transactional
public XxxResponse update(String id, UpdateXxxRequest request) {
    String tenantId = TenantContext.getTenantId();
    
    // ... 更新邏輯 ...
    
    // 清除快取
    cacheService.evict("xxx:" + tenantId + ":" + id);
    cacheService.evict("xxx:list:" + tenantId);
    
    return xxxMapper.toResponse(entity);
}