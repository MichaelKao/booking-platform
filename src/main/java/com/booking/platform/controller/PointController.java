package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreatePointTopUpRequest;
import com.booking.platform.dto.response.PointBalanceResponse;
import com.booking.platform.dto.response.PointTopUpResponse;
import com.booking.platform.dto.response.TenantPointTransactionResponse;
import com.booking.platform.enums.TopUpStatus;
import com.booking.platform.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 點數 Controller
 *
 * <p>管理店家的點數餘額、儲值、交易記錄
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PointController {

    // ========================================
    // 依賴注入
    // ========================================

    private final PointService pointService;

    // ========================================
    // 餘額查詢
    // ========================================

    /**
     * 取得點數餘額
     *
     * @return 餘額資料
     */
    @GetMapping("/balance")
    public ApiResponse<PointBalanceResponse> getBalance() {
        log.info("收到取得點數餘額請求");
        return ApiResponse.ok(pointService.getBalance());
    }

    // ========================================
    // 儲值管理
    // ========================================

    /**
     * 申請儲值
     *
     * @param request 儲值請求
     * @return 儲值申請資料
     */
    @PostMapping("/topup")
    public ApiResponse<PointTopUpResponse> createTopUp(
            @Valid @RequestBody CreatePointTopUpRequest request
    ) {
        log.info("收到申請儲值請求，金額：{}", request.getAmount());
        return ApiResponse.ok("儲值申請已提交", pointService.createTopUp(request));
    }

    /**
     * 查詢儲值記錄
     *
     * @param page 頁碼
     * @param size 每頁筆數
     * @param status 狀態篩選
     * @return 分頁結果
     */
    @GetMapping("/topups")
    public ApiResponse<PageResponse<PointTopUpResponse>> getTopUpList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TopUpStatus status
    ) {
        log.info("收到查詢儲值記錄請求");

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ApiResponse.ok(pointService.getTopUpList(status, pageable));
    }

    // ========================================
    // 交易記錄
    // ========================================

    /**
     * 查詢點數交易記錄
     *
     * @param page 頁碼
     * @param size 每頁筆數
     * @return 分頁結果
     */
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<TenantPointTransactionResponse>> getTransactionList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("收到查詢點數交易記錄請求");

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ApiResponse.ok(pointService.getTransactionList(pageable));
    }
}
