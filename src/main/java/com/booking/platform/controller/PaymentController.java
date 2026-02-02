package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.dto.request.CreatePaymentRequest;
import com.booking.platform.dto.response.EcpayCheckoutResponse;
import com.booking.platform.dto.response.PaymentResponse;
import com.booking.platform.enums.PaymentStatus;
import com.booking.platform.service.payment.EcpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付 API
 *
 * <p>提供 ECPay 金流相關功能
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentController {

    private final EcpayService ecpayService;

    // ========================================
    // 建立付款
    // ========================================

    /**
     * 建立 ECPay 付款
     *
     * @param request 付款請求
     * @return ECPay 付款頁面資訊
     */
    @PostMapping
    public ApiResponse<EcpayCheckoutResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        log.info("建立付款，金額：{}", request.getAmount());

        EcpayCheckoutResponse response = ecpayService.createPayment(request);
        return ApiResponse.ok("付款建立成功", response);
    }

    // ========================================
    // 查詢
    // ========================================

    /**
     * 查詢支付記錄列表
     *
     * @param status 狀態篩選
     * @param page   頁碼
     * @param size   每頁筆數
     * @return 支付記錄列表
     */
    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> getList(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<PaymentResponse> response = ecpayService.getList(status, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 查詢支付記錄詳情
     *
     * @param id 支付記錄 ID
     * @return 支付記錄詳情
     */
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getDetail(@PathVariable String id) {
        PaymentResponse response = ecpayService.getDetail(id);
        return ApiResponse.ok(response);
    }

    /**
     * 依訂單編號查詢
     *
     * @param merchantTradeNo 商店訂單編號
     * @return 支付記錄
     */
    @GetMapping("/order/{merchantTradeNo}")
    public ApiResponse<PaymentResponse> getByOrderNo(@PathVariable String merchantTradeNo) {
        PaymentResponse response = ecpayService.getByMerchantTradeNo(merchantTradeNo);
        return ApiResponse.ok(response);
    }

    // ========================================
    // ECPay 回調
    // ========================================

    /**
     * ECPay 付款結果通知（ReturnURL）
     *
     * <p>ECPay 會以 POST 方式呼叫此 API 通知付款結果
     *
     * @param request HTTP 請求
     * @return 回應字串（1|OK 或 0|錯誤訊息）
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(HttpServletRequest request) {
        log.info("收到 ECPay 付款結果回調");

        // 取得所有參數
        Map<String, String> params = extractParams(request);

        String result = ecpayService.handlePaymentResult(params);
        return ResponseEntity.ok(result);
    }

    /**
     * 從 HTTP 請求中提取參數
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();

        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }

        return params;
    }
}
