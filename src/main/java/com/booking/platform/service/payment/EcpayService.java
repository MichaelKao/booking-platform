package com.booking.platform.service.payment;

import com.booking.platform.common.config.EcpayConfig;
import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.response.PageResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.request.CreatePaymentRequest;
import com.booking.platform.dto.response.EcpayCheckoutResponse;
import com.booking.platform.dto.response.PaymentResponse;
import com.booking.platform.entity.system.Payment;
import com.booking.platform.enums.PaymentStatus;
import com.booking.platform.enums.PaymentType;
import com.booking.platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * ECPay 金流服務
 *
 * <p>整合 ECPay 綠界金流，提供線上支付功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EcpayService {

    private final EcpayConfig ecpayConfig;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // ========================================
    // 建立付款
    // ========================================

    /**
     * 建立 ECPay 付款
     *
     * @param request 付款請求
     * @return ECPay 付款頁面資訊
     */
    @Transactional
    public EcpayCheckoutResponse createPayment(CreatePaymentRequest request) {
        String tenantId = TenantContext.getTenantId();

        log.info("建立 ECPay 付款，租戶：{}，金額：{}", tenantId, request.getAmount());

        // ========================================
        // 1. 產生訂單編號
        // ========================================
        String merchantTradeNo = generateMerchantTradeNo(tenantId);

        // ========================================
        // 2. 建立支付記錄
        // ========================================
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .merchantTradeNo(merchantTradeNo)
                .amount(request.getAmount())
                .description(request.getDescription())
                .bookingId(request.getBookingId())
                .customerId(request.getCustomerId())
                .topupId(request.getTopupId())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // ========================================
        // 3. 建立 ECPay 付款參數
        // ========================================
        Map<String, String> params = buildCheckoutParams(payment, request);

        // ========================================
        // 4. 產生檢查碼
        // ========================================
        String checkMacValue = generateCheckMacValue(params);
        params.put("CheckMacValue", checkMacValue);

        // ========================================
        // 5. 建立付款表單 HTML
        // ========================================
        String checkoutHtml = buildCheckoutHtml(params);

        log.info("ECPay 付款建立成功，訂單編號：{}", merchantTradeNo);

        return EcpayCheckoutResponse.builder()
                .paymentId(payment.getId())
                .merchantTradeNo(merchantTradeNo)
                .checkoutHtml(checkoutHtml)
                .checkoutUrl(ecpayConfig.getEffectiveApiUrl())
                .build();
    }

    // ========================================
    // 處理付款結果
    // ========================================

    /**
     * 處理 ECPay 付款結果通知
     *
     * @param params ECPay 回傳參數
     * @return 處理結果（1|OK 表示成功）
     */
    @Transactional
    public String handlePaymentResult(Map<String, String> params) {
        log.info("收到 ECPay 付款結果通知：{}", params);

        // ========================================
        // 1. 驗證檢查碼
        // ========================================
        String receivedCheckMac = params.get("CheckMacValue");
        Map<String, String> paramsWithoutMac = new TreeMap<>(params);
        paramsWithoutMac.remove("CheckMacValue");

        String calculatedCheckMac = generateCheckMacValue(paramsWithoutMac);

        if (!calculatedCheckMac.equalsIgnoreCase(receivedCheckMac)) {
            log.error("ECPay 檢查碼驗證失敗");
            return "0|CheckMacValue Error";
        }

        // ========================================
        // 2. 查詢支付記錄
        // ========================================
        String merchantTradeNo = params.get("MerchantTradeNo");
        Payment payment = paymentRepository.findByMerchantTradeNoAndDeletedAtIsNull(merchantTradeNo)
                .orElse(null);

        if (payment == null) {
            log.error("找不到支付記錄，訂單編號：{}", merchantTradeNo);
            return "0|Order Not Found";
        }

        // 冪等性檢查：已處理過的付款直接回傳成功
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("付款已處理過，跳過重複回調，訂單編號：{}", merchantTradeNo);
            return "1|OK";
        }

        // ========================================
        // 3. 更新支付狀態
        // ========================================
        String rtnCode = params.get("RtnCode");
        String rtnMsg = params.get("RtnMsg");
        String ecpayTradeNo = params.get("TradeNo");

        if ("1".equals(rtnCode)) {
            // 付款成功
            PaymentType paymentType = parsePaymentType(params.get("PaymentType"));
            payment.markSuccess(ecpayTradeNo, paymentType);

            // 設定額外資訊
            payment.setCardLastFour(params.get("card4no"));
            payment.setPaymentBankCode(params.get("PaymentTypeChargeFee"));

            log.info("付款成功，訂單編號：{}，ECPay 編號：{}", merchantTradeNo, ecpayTradeNo);
        } else {
            // 付款失敗
            payment.markFailed(rtnCode, rtnMsg);
            log.warn("付款失敗，訂單編號：{}，錯誤：{}", merchantTradeNo, rtnMsg);
        }

        payment.setEcpayResponseCode(rtnCode);
        payment.setEcpayResponseMessage(rtnMsg);
        paymentRepository.save(payment);

        return "1|OK";
    }

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 查詢支付記錄列表
     */
    public PageResponse<PaymentResponse> getList(PaymentStatus status, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();

        Page<Payment> page;
        if (status != null) {
            page = paymentRepository.findByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    tenantId, status, pageable);
        } else {
            page = paymentRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                    tenantId, pageable);
        }

        List<PaymentResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PaymentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 查詢支付記錄詳情
     */
    public PaymentResponse getDetail(String id) {
        String tenantId = TenantContext.getTenantId();

        Payment payment = paymentRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到支付記錄"));

        return toResponse(payment);
    }

    /**
     * 依訂單編號查詢
     */
    public PaymentResponse getByMerchantTradeNo(String merchantTradeNo) {
        Payment payment = paymentRepository.findByMerchantTradeNoAndDeletedAtIsNull(merchantTradeNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_RESOURCE_NOT_FOUND, "找不到支付記錄"));

        return toResponse(payment);
    }

    // ========================================
    // 內部方法
    // ========================================

    /**
     * 產生商店訂單編號
     */
    private String generateMerchantTradeNo(String tenantId) {
        // 格式：T + 租戶ID前4碼 + 時間戳 + 隨機數
        String prefix = "T" + tenantId.substring(0, 4).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp.substring(timestamp.length() - 10);
    }

    /**
     * 建立付款參數
     */
    private Map<String, String> buildCheckoutParams(Payment payment, CreatePaymentRequest request) {
        Map<String, String> params = new TreeMap<>();

        params.put("MerchantID", ecpayConfig.getMerchantId());
        params.put("MerchantTradeNo", payment.getMerchantTradeNo());
        params.put("MerchantTradeDate", LocalDateTime.now().format(DATE_FORMAT));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(payment.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValue()));
        params.put("TradeDesc", urlEncode(payment.getDescription() != null ? payment.getDescription() : "線上付款"));
        params.put("ItemName", urlEncode(payment.getDescription() != null ? payment.getDescription() : "商品"));
        params.put("ReturnURL", request.getNotifyUrl() != null ? request.getNotifyUrl() : "");
        params.put("ChoosePayment", "ALL");
        params.put("EncryptType", "1");

        // 可選參數
        if (request.getClientBackUrl() != null) {
            params.put("ClientBackURL", request.getClientBackUrl());
        }
        if (request.getReturnUrl() != null) {
            params.put("OrderResultURL", request.getReturnUrl());
        }

        return params;
    }

    /**
     * 產生 ECPay 檢查碼（SHA256）
     */
    private String generateCheckMacValue(Map<String, String> params) {
        // 1. 依照參數名稱排序
        StringBuilder sb = new StringBuilder();
        sb.append("HashKey=").append(ecpayConfig.getHashKey());

        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }

        sb.append("&HashIV=").append(ecpayConfig.getHashIv());

        // 2. URL Encode
        String encoded = urlEncode(sb.toString()).toLowerCase();

        // 3. SHA256 Hash
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(encoded.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYS_INTERNAL_ERROR, "產生檢查碼失敗");
        }
    }

    /**
     * 建立付款表單 HTML
     */
    private String buildCheckoutHtml(Map<String, String> params) {
        StringBuilder html = new StringBuilder();
        html.append("<form id='ecpayForm' method='post' action='")
                .append(ecpayConfig.getEffectiveApiUrl())
                .append("'>");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type='hidden' name='")
                    .append(entry.getKey())
                    .append("' value='")
                    .append(entry.getValue())
                    .append("'/>");
        }

        html.append("</form>");
        html.append("<script>document.getElementById('ecpayForm').submit();</script>");

        return html.toString();
    }

    /**
     * URL 編碼
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("%2d", "-")
                    .replace("%5f", "_")
                    .replace("%2e", ".")
                    .replace("%21", "!")
                    .replace("%2a", "*")
                    .replace("%28", "(")
                    .replace("%29", ")");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 解析支付方式
     */
    private PaymentType parsePaymentType(String ecpayPaymentType) {
        if (ecpayPaymentType == null) {
            return null;
        }

        return switch (ecpayPaymentType.toUpperCase()) {
            case "CREDIT" -> PaymentType.CREDIT_CARD;
            case "ATM" -> PaymentType.ATM;
            case "CVS" -> PaymentType.CVS;
            case "BARCODE" -> PaymentType.BARCODE;
            case "WEBATM" -> PaymentType.WEB_ATM;
            default -> null;
        };
    }

    /**
     * 轉換為回應 DTO
     */
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .merchantTradeNo(payment.getMerchantTradeNo())
                .amount(payment.getAmount())
                .paymentType(payment.getPaymentType())
                .paymentTypeDescription(payment.getPaymentType() != null
                        ? payment.getPaymentType().getDescription() : null)
                .status(payment.getStatus())
                .statusDescription(payment.getStatus().getDescription())
                .description(payment.getDescription())
                .bookingId(payment.getBookingId())
                .customerId(payment.getCustomerId())
                .ecpayTradeNo(payment.getEcpayTradeNo())
                .cardLastFour(payment.getCardLastFour())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
