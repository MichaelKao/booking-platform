package com.booking.platform.service.export;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.dto.response.ReportSummaryResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF 匯出服務
 *
 * <p>支援匯出預約、報表等資料為 PDF 格式
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 使用系統字體或預設字體
    private static final String FONT_PATH = "STSong-Light";
    private static final String ENCODING = "UniGB-UCS2-H";

    /**
     * 匯出預約清單
     */
    public byte[] exportBookings(List<Booking> bookings, String tenantName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // 橫向

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // 取得中文字體
            BaseFont baseFont = getChineseFont();
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);
            Font cellFont = new Font(baseFont, 9, Font.NORMAL);

            // 標題
            Paragraph title = new Paragraph(tenantName + " - 預約清單", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // 建立表格
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.5f, 1.5f, 2f, 1.5f, 1.5f, 1.5f, 1f});

            // 表頭
            String[] headers = {"顧客姓名", "電話", "服務項目", "員工", "日期", "時間", "狀態"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(220, 220, 220));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // 資料列
            for (Booking booking : bookings) {
                addCell(table, booking.getCustomerName() != null ? booking.getCustomerName() : "", cellFont);
                addCell(table, booking.getCustomerPhone() != null ? booking.getCustomerPhone() : "", cellFont);
                addCell(table, booking.getServiceName() != null ? booking.getServiceName() : "", cellFont);
                addCell(table, booking.getStaffName() != null ? booking.getStaffName() : "不指定", cellFont);
                addCell(table, booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : "", cellFont);
                addCell(table, (booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) : "") +
                              (booking.getEndTime() != null ? "-" + booking.getEndTime().format(TIME_FORMATTER) : ""), cellFont);
                addCell(table, getStatusText(booking.getStatus().name()), cellFont);
            }

            document.add(table);

            // 頁尾 - 統計資訊
            document.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("共 " + bookings.size() + " 筆預約", cellFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

        } catch (DocumentException e) {
            log.error("PDF 匯出失敗", e);
            throw new IOException("PDF 匯出失敗: " + e.getMessage(), e);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    /**
     * 匯出報表摘要
     */
    public byte[] exportReportSummary(ReportSummaryResponse summary, String tenantName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // 取得中文字體
            BaseFont baseFont = getChineseFont();
            Font titleFont = new Font(baseFont, 20, Font.BOLD);
            Font sectionFont = new Font(baseFont, 14, Font.BOLD);
            Font labelFont = new Font(baseFont, 11, Font.NORMAL);
            Font valueFont = new Font(baseFont, 11, Font.BOLD);

            // 標題
            Paragraph title = new Paragraph(tenantName + " - 營運報表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30f);
            document.add(title);

            // 今日統計
            addSectionTitle(document, "今日統計", sectionFont);
            addStatRow(document, "新預約數", String.valueOf(summary.getTodayBookings()), labelFont, valueFont);
            addStatRow(document, "完成預約", String.valueOf(summary.getTodayCompleted()), labelFont, valueFont);
            addStatRow(document, "營業額", "NT$ " + (summary.getTodayRevenue() != null ? summary.getTodayRevenue().toString() : "0"), labelFont, valueFont);
            addStatRow(document, "新顧客", String.valueOf(summary.getTodayNewCustomers()), labelFont, valueFont);
            document.add(new Paragraph("\n"));

            // 本週統計
            addSectionTitle(document, "本週統計", sectionFont);
            addStatRow(document, "預約數", String.valueOf(summary.getWeeklyBookings()), labelFont, valueFont);
            addStatRow(document, "營業額", "NT$ " + (summary.getWeeklyRevenue() != null ? summary.getWeeklyRevenue().toString() : "0"), labelFont, valueFont);
            document.add(new Paragraph("\n"));

            // 本月統計
            addSectionTitle(document, "本月統計", sectionFont);
            addStatRow(document, "預約數", String.valueOf(summary.getMonthlyBookings()), labelFont, valueFont);
            addStatRow(document, "營業額", "NT$ " + (summary.getMonthlyRevenue() != null ? summary.getMonthlyRevenue().toString() : "0"), labelFont, valueFont);
            addStatRow(document, "新顧客", String.valueOf(summary.getMonthlyNewCustomers()), labelFont, valueFont);

        } catch (DocumentException e) {
            log.error("PDF 匯出失敗", e);
            throw new IOException("PDF 匯出失敗: " + e.getMessage(), e);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    // ========================================
    // 私有方法
    // ========================================

    private BaseFont getChineseFont() {
        try {
            // 嘗試使用內建中文字體
            return BaseFont.createFont(FONT_PATH, ENCODING, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            try {
                // 如果失敗，使用 Helvetica
                log.warn("無法載入中文字體，使用預設字體");
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new RuntimeException("無法建立字體", ex);
            }
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph section = new Paragraph(title, font);
        section.setSpacingBefore(10f);
        section.setSpacingAfter(10f);
        document.add(section);
    }

    private void addStatRow(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingLeft(20f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);

        document.add(table);
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
}
