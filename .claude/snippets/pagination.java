/**
 * 分頁處理片段
 * 
 * 使用方式：在 Controller 中處理分頁參數
 */

// Controller 中的分頁處理
@GetMapping
public ApiResponse<PageResponse<XxxResponse>> getList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction
) {
    // 限制最大分頁大小
    size = Math.min(size, 100);
    
    // 建立分頁參數
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
    
    // 執行查詢
    return ApiResponse.ok(xxxService.getList(pageable));
}