package com.booking.platform.controller;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 公開預約操作 Controller
 *
 * <p>提供顧客自助取消預約等公開功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
@Slf4j
public class PublicBookingController {

    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final BookingService bookingService;

    /**
     * 取消預約頁面
     *
     * <p>透過取消連結進入此頁面
     */
    @GetMapping("/cancel/{token}")
    public String cancelPage(@PathVariable String token, Model model) {
        log.info("顧客進入取消預約頁面，token：{}", token);

        try {
            // 查詢預約
            Booking booking = findBookingByToken(token);
            Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(booking.getTenantId())
                    .orElse(null);

            // 檢查是否可取消
            boolean canCancel = booking.isCancellable();

            model.addAttribute("booking", booking);
            model.addAttribute("tenant", tenant);
            model.addAttribute("token", token);
            model.addAttribute("canCancel", canCancel);
            model.addAttribute("statusText", getStatusText(booking.getStatus()));

            return "public/cancel-booking";

        } catch (ResourceNotFoundException e) {
            model.addAttribute("error", "找不到此預約，連結可能已失效");
            return "public/cancel-booking";
        } catch (Exception e) {
            log.error("載入取消預約頁面失敗", e);
            model.addAttribute("error", "系統錯誤，請稍後再試");
            return "public/cancel-booking";
        }
    }

    /**
     * 執行取消預約
     */
    @PostMapping("/cancel/{token}")
    @ResponseBody
    public ApiResponse<Void> cancelBooking(
            @PathVariable String token,
            @RequestParam(required = false) String reason
    ) {
        log.info("顧客執行取消預約，token：{}", token);

        try {
            // 查詢預約
            Booking booking = findBookingByToken(token);

            // 檢查是否可取消
            if (!booking.isCancellable()) {
                throw new BusinessException(ErrorCode.SYS_INVALID_OPERATION,
                        "此預約無法取消（目前狀態：" + getStatusText(booking.getStatus()) + "）");
            }

            // 執行取消
            String cancelReason = (reason != null && !reason.isEmpty()) ? reason : "顧客自助取消";
            booking.cancel(cancelReason);
            bookingRepository.save(booking);

            log.info("顧客自助取消預約成功，預約 ID：{}", booking.getId());

            return ApiResponse.ok("預約已成功取消", null);

        } catch (ResourceNotFoundException e) {
            return ApiResponse.error(ErrorCode.SYS_RESOURCE_NOT_FOUND.name(), "找不到此預約");
        } catch (BusinessException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("取消預約失敗", e);
            return ApiResponse.error("ERROR", "系統錯誤，請稍後再試");
        }
    }

    /**
     * 根據 token 查詢預約
     */
    private Booking findBookingByToken(String token) {
        return bookingRepository.findByCancelTokenAndDeletedAtIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BOOKING_NOT_FOUND, "找不到預約"
                ));
    }

    /**
     * 取得狀態文字
     */
    private String getStatusText(BookingStatus status) {
        if (status == null) return "未知";
        return switch (status) {
            case PENDING -> "待確認";
            case CONFIRMED -> "已確認";
            case IN_PROGRESS -> "進行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case NO_SHOW -> "未到";
        };
    }
}
