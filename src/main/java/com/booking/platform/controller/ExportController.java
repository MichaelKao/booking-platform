package com.booking.platform.controller;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.ReportSummaryResponse;
import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.BookingStatus;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.ReportService;
import com.booking.platform.service.export.ExcelExportService;
import com.booking.platform.service.export.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 報表匯出 Controller
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExportController {

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final ReportService reportService;

    // ========================================
    // 預約匯出
    // ========================================

    /**
     * 匯出預約清單 - Excel
     */
    @GetMapping("/bookings/excel")
    public ResponseEntity<byte[]> exportBookingsExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BookingStatus status
    ) throws IOException {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "開始日期不能晚於結束日期");
        }

        String tenantId = TenantContext.getTenantId();
        String tenantName = getTenantName(tenantId);

        List<Booking> bookings = bookingRepository.findForExport(tenantId, startDate, endDate, status);

        byte[] content = excelExportService.exportBookings(bookings, tenantName);

        String filename = URLEncoder.encode("預約清單_" + startDate + "_" + endDate + ".xlsx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    /**
     * 匯出預約清單 - PDF
     */
    @GetMapping("/bookings/pdf")
    public ResponseEntity<byte[]> exportBookingsPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BookingStatus status
    ) throws IOException {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.SYS_PARAM_ERROR, "開始日期不能晚於結束日期");
        }

        String tenantId = TenantContext.getTenantId();
        String tenantName = getTenantName(tenantId);

        List<Booking> bookings = bookingRepository.findForExport(tenantId, startDate, endDate, status);

        byte[] content = pdfExportService.exportBookings(bookings, tenantName);

        String filename = URLEncoder.encode("預約清單_" + startDate + "_" + endDate + ".pdf", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    // ========================================
    // 報表匯出
    // ========================================

    /**
     * 匯出報表 - Excel
     */
    @GetMapping("/reports/excel")
    public ResponseEntity<byte[]> exportReportsExcel(
            @RequestParam(defaultValue = "month") String range
    ) throws IOException {
        String tenantId = TenantContext.getTenantId();
        String tenantName = getTenantName(tenantId);

        ReportSummaryResponse summary = reportService.getMonthlySummary();

        byte[] content = excelExportService.exportReportSummary(summary, tenantName);

        String filename = URLEncoder.encode("營運報表_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    /**
     * 匯出報表 - PDF
     */
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> exportReportsPdf(
            @RequestParam(defaultValue = "month") String range
    ) throws IOException {
        String tenantId = TenantContext.getTenantId();
        String tenantName = getTenantName(tenantId);

        ReportSummaryResponse summary = reportService.getMonthlySummary();

        byte[] content = pdfExportService.exportReportSummary(summary, tenantName);

        String filename = URLEncoder.encode("營運報表_" + LocalDate.now() + ".pdf", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    // ========================================
    // 顧客匯出
    // ========================================

    /**
     * 匯出顧客清單 - Excel
     */
    @GetMapping("/customers/excel")
    public ResponseEntity<byte[]> exportCustomersExcel() throws IOException {
        String tenantId = TenantContext.getTenantId();
        String tenantName = getTenantName(tenantId);

        List<Customer> customers = customerRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        byte[] content = excelExportService.exportCustomers(customers, tenantName);

        String filename = URLEncoder.encode("顧客清單_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    // ========================================
    // 私有方法
    // ========================================

    private String getTenantName(String tenantId) {
        return tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                .map(Tenant::getName)
                .orElse("店家");
    }
}
