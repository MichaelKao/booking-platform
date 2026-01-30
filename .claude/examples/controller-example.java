/**
 * Controller 範例
 * 
 * <p>所有 Controller 都要遵循此範例的風格
 * 
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExampleController {

    // ========================================
    // 依賴注入
    // ========================================
    
    private final ExampleService exampleService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 查詢列表
     * 
     * @param page 頁碼
     * @param size 每頁筆數
     * @param status 狀態篩選
     * @param keyword 關鍵字
     * @return 分頁結果
     */
    @GetMapping
    public ApiResponse<PageResponse<ExampleListItemResponse>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ExampleStatus status,
            @RequestParam(required = false) String keyword
    ) {
        // 限制最大分頁大小
        size = Math.min(size, 100);
        
        // 建立分頁參數
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 查詢並返回
        return ApiResponse.ok(exampleService.getList(status, keyword, pageable));
    }

    /**
     * 查詢詳情
     * 
     * @param id 資料 ID
     * @return 詳情資料
     */
    @GetMapping("/{id}")
    public ApiResponse<ExampleDetailResponse> getDetail(@PathVariable String id) {
        return ApiResponse.ok(exampleService.getDetail(id));
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 建立資料
     * 
     * @param request 建立請求
     * @return 建立結果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExampleResponse> create(@Valid @RequestBody CreateExampleRequest request) {
        log.info("收到建立 Example 請求：{}", request);
        return ApiResponse.ok(exampleService.create(request));
    }

    /**
     * 更新資料
     * 
     * @param id 資料 ID
     * @param request 更新請求
     * @return 更新結果
     */
    @PutMapping("/{id}")
    public ApiResponse<ExampleResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateExampleRequest request
    ) {
        log.info("收到更新 Example 請求，ID：{}", id);
        return ApiResponse.ok(exampleService.update(id, request));
    }

    /**
     * 刪除資料
     * 
     * @param id 資料 ID
     * @return 刪除結果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("收到刪除 Example 請求，ID：{}", id);
        exampleService.delete(id);
        return ApiResponse.ok();
    }
}