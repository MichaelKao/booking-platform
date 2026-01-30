package com.booking.platform.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分頁回應格式
 *
 * <p>用於包裝分頁查詢結果
 *
 * <p>範例：
 * <pre>{@code
 * {
 *     "content": [...],
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 100,
 *     "totalPages": 5,
 *     "first": true,
 *     "last": false
 * }
 * }</pre>
 *
 * @param <T> 資料類型
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    // ========================================
    // 欄位
    // ========================================

    /**
     * 資料內容
     */
    private List<T> content;

    /**
     * 當前頁碼（從 0 開始）
     */
    private int page;

    /**
     * 每頁筆數
     */
    private int size;

    /**
     * 總筆數
     */
    private long totalElements;

    /**
     * 總頁數
     */
    private int totalPages;

    /**
     * 是否為第一頁
     */
    private boolean first;

    /**
     * 是否為最後一頁
     */
    private boolean last;

    // ========================================
    // 工廠方法
    // ========================================

    /**
     * 從 Spring Data Page 建立 PageResponse
     *
     * @param page Spring Data Page 物件
     * @return PageResponse 實例
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 建立空的分頁回應
     *
     * @param page 頁碼
     * @param size 每頁筆數
     * @return 空的 PageResponse 實例
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();
    }
}
