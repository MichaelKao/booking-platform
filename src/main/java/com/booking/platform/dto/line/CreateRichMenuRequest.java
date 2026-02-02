package com.booking.platform.dto.line;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建立 Rich Menu 請求 DTO
 *
 * <p>用於建立自訂主題的 Rich Menu
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRichMenuRequest {

    // ========================================
    // 主題設定
    // ========================================

    /**
     * 主題配色
     * <p>可選值：GREEN, BLUE, PURPLE, ORANGE, DARK
     */
    @Pattern(regexp = "^(GREEN|BLUE|PURPLE|ORANGE|DARK)$", message = "主題必須為 GREEN, BLUE, PURPLE, ORANGE 或 DARK")
    private String theme;

    // ========================================
    // 輔助方法
    // ========================================

    /**
     * 取得主題的十六進位色碼
     *
     * @return 色碼（不含 #）
     */
    public String getThemeColorHex() {
        if (theme == null) {
            return "1DB446"; // 預設 LINE 綠色
        }
        return switch (theme.toUpperCase()) {
            case "GREEN" -> "1DB446";   // LINE Green
            case "BLUE" -> "2196F3";    // Ocean Blue
            case "PURPLE" -> "9C27B0";  // Royal Purple
            case "ORANGE" -> "FF5722";  // Sunset Orange
            case "DARK" -> "263238";    // Dark Mode
            default -> "1DB446";
        };
    }

    /**
     * 取得主題的中文名稱
     *
     * @return 中文名稱
     */
    public String getThemeDisplayName() {
        if (theme == null) {
            return "LINE 綠";
        }
        return switch (theme.toUpperCase()) {
            case "GREEN" -> "LINE 綠";
            case "BLUE" -> "海洋藍";
            case "PURPLE" -> "皇家紫";
            case "ORANGE" -> "日落橘";
            case "DARK" -> "暗黑模式";
            default -> "LINE 綠";
        };
    }
}
