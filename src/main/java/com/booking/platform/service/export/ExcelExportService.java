package com.booking.platform.service.export;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.customer.Customer;
import com.booking.platform.dto.response.ReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 匯出服務
 *
 * <p>支援匯出預約、顧客、報表等資料為 Excel 格式
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 匯出預約清單
     */
    public byte[] exportBookings(List<Booking> bookings, String tenantName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("預約清單");

            // 建立標題列樣式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 建立標題列
            Row headerRow = sheet.createRow(0);
            String[] headers = {"預約編號", "顧客姓名", "顧客電話", "服務項目", "員工", "預約日期", "開始時間", "結束時間", "狀態", "備註", "建立時間"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填入資料
            int rowNum = 1;
            for (Booking booking : bookings) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(booking.getId());
                row.createCell(1).setCellValue(booking.getCustomerName() != null ? booking.getCustomerName() : "");
                row.createCell(2).setCellValue(booking.getCustomerPhone() != null ? booking.getCustomerPhone() : "");
                row.createCell(3).setCellValue(booking.getServiceName() != null ? booking.getServiceName() : "");
                row.createCell(4).setCellValue(booking.getStaffName() != null ? booking.getStaffName() : "不指定");
                row.createCell(5).setCellValue(booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : "");
                row.createCell(6).setCellValue(booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) : "");
                row.createCell(7).setCellValue(booking.getEndTime() != null ? booking.getEndTime().format(TIME_FORMATTER) : "");
                row.createCell(8).setCellValue(getStatusText(booking.getStatus().name()));
                row.createCell(9).setCellValue(booking.getCustomerNote() != null ? booking.getCustomerNote() : "");
                row.createCell(10).setCellValue(booking.getCreatedAt() != null ? booking.getCreatedAt().format(DATETIME_FORMATTER) : "");
            }

            // 自動調整欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 輸出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 匯出顧客清單
     */
    public byte[] exportCustomers(List<Customer> customers, String tenantName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("顧客清單");

            // 建立標題列樣式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 建立標題列
            Row headerRow = sheet.createRow(0);
            String[] headers = {"顧客編號", "姓名", "電話", "信箱", "性別", "生日", "會員等級", "累積消費", "累積點數", "預約次數", "狀態", "加入時間"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填入資料
            int rowNum = 1;
            for (Customer customer : customers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(customer.getId());
                row.createCell(1).setCellValue(customer.getName() != null ? customer.getName() : "");
                row.createCell(2).setCellValue(customer.getPhone() != null ? customer.getPhone() : "");
                row.createCell(3).setCellValue(customer.getEmail() != null ? customer.getEmail() : "");
                row.createCell(4).setCellValue(getGenderText(customer.getGender() != null ? customer.getGender().name() : null));
                row.createCell(5).setCellValue(customer.getBirthday() != null ? customer.getBirthday().format(DATE_FORMATTER) : "");
                row.createCell(6).setCellValue(customer.getMembershipLevelId() != null ? customer.getMembershipLevelId() : "");
                row.createCell(7).setCellValue(customer.getTotalSpent() != null ? customer.getTotalSpent().doubleValue() : 0);
                row.createCell(8).setCellValue(customer.getPointBalance() != null ? customer.getPointBalance() : 0);
                row.createCell(9).setCellValue(customer.getVisitCount() != null ? customer.getVisitCount() : 0);
                row.createCell(10).setCellValue(getCustomerStatusText(customer.getStatus() != null ? customer.getStatus().name() : null));
                row.createCell(11).setCellValue(customer.getCreatedAt() != null ? customer.getCreatedAt().format(DATETIME_FORMATTER) : "");
            }

            // 自動調整欄寬
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 輸出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 匯出報表摘要
     */
    public byte[] exportReportSummary(ReportSummaryResponse summary, String tenantName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("營運報表");

            // 建立標題列樣式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;

            // 店家名稱
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(tenantName + " - 營運報表");
            titleCell.setCellStyle(headerStyle);
            rowNum++; // 空一行

            // 今日統計
            Row todayHeader = sheet.createRow(rowNum++);
            todayHeader.createCell(0).setCellValue("今日統計");
            todayHeader.getCell(0).setCellStyle(headerStyle);

            createSummaryRow(sheet, rowNum++, "新預約數", summary.getTodayBookings());
            createSummaryRow(sheet, rowNum++, "完成預約", summary.getTodayCompleted());
            createSummaryRow(sheet, rowNum++, "營業額", summary.getTodayRevenue() != null ? summary.getTodayRevenue().doubleValue() : 0);
            createSummaryRow(sheet, rowNum++, "新顧客", summary.getTodayNewCustomers());
            rowNum++; // 空一行

            // 本週統計
            Row weekHeader = sheet.createRow(rowNum++);
            weekHeader.createCell(0).setCellValue("本週統計");
            weekHeader.getCell(0).setCellStyle(headerStyle);

            createSummaryRow(sheet, rowNum++, "預約數", summary.getWeeklyBookings());
            createSummaryRow(sheet, rowNum++, "營業額", summary.getWeeklyRevenue() != null ? summary.getWeeklyRevenue().doubleValue() : 0);
            rowNum++; // 空一行

            // 本月統計
            Row monthHeader = sheet.createRow(rowNum++);
            monthHeader.createCell(0).setCellValue("本月統計");
            monthHeader.getCell(0).setCellStyle(headerStyle);

            createSummaryRow(sheet, rowNum++, "預約數", summary.getMonthlyBookings());
            createSummaryRow(sheet, rowNum++, "營業額", summary.getMonthlyRevenue() != null ? summary.getMonthlyRevenue().doubleValue() : 0);
            createSummaryRow(sheet, rowNum++, "新顧客", summary.getMonthlyNewCustomers());

            // 自動調整欄寬
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // 輸出
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ========================================
    // 私有方法
    // ========================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        if (value instanceof Number) {
            row.createCell(1).setCellValue(((Number) value).doubleValue());
        } else {
            row.createCell(1).setCellValue(value != null ? value.toString() : "0");
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待確認";
            case "CONFIRMED" -> "已確認";
            case "IN_PROGRESS" -> "進行中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "NO_SHOW" -> "未到";
            default -> status;
        };
    }

    private String getGenderText(String gender) {
        if (gender == null) return "";
        return switch (gender) {
            case "MALE" -> "男";
            case "FEMALE" -> "女";
            default -> "未知";
        };
    }

    private String getCustomerStatusText(String status) {
        if (status == null) return "正常";
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "BLOCKED" -> "已封鎖";
            default -> status;
        };
    }
}
