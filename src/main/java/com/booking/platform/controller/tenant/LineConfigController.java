package com.booking.platform.controller.tenant;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.line.CreateRichMenuRequest;
import com.booking.platform.dto.line.LineConfigResponse;
import com.booking.platform.dto.line.SaveLineConfigRequest;
import com.booking.platform.service.line.LineConfigService;
import com.booking.platform.service.line.LineRichMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * LINE 設定控制器
 *
 * <p>提供店家管理 LINE Bot 設定的 API
 *
 * <p>API 端點：
 * <ul>
 *   <li>GET /api/settings/line - 取得 LINE 設定</li>
 *   <li>PUT /api/settings/line - 更新 LINE 設定</li>
 *   <li>POST /api/settings/line/activate - 啟用 LINE Bot</li>
 *   <li>POST /api/settings/line/deactivate - 停用 LINE Bot</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/settings/line")
@RequiredArgsConstructor
@Slf4j
public class LineConfigController {

    // ========================================
    // 依賴注入
    // ========================================

    private final LineConfigService lineConfigService;
    private final LineRichMenuService richMenuService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得 LINE 設定
     *
     * @return LINE 設定
     */
    @GetMapping
    public ResponseEntity<ApiResponse<LineConfigResponse>> getConfig() {
        log.debug("取得 LINE 設定");

        LineConfigResponse response = lineConfigService.getConfig();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 更新 LINE 設定
     *
     * @param request 設定請求
     * @return 更新後的 LINE 設定
     */
    @PutMapping
    public ResponseEntity<ApiResponse<LineConfigResponse>> saveConfig(
            @Valid @RequestBody SaveLineConfigRequest request
    ) {
        log.debug("更新 LINE 設定，請求：{}", request);

        LineConfigResponse response = lineConfigService.saveConfig(request);

        return ResponseEntity.ok(ApiResponse.ok("LINE 設定更新成功", response));
    }

    /**
     * 啟用 LINE Bot
     *
     * @return 更新後的 LINE 設定
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<LineConfigResponse>> activate() {
        log.debug("啟用 LINE Bot");

        LineConfigResponse response = lineConfigService.activate();

        return ResponseEntity.ok(ApiResponse.ok("LINE Bot 已啟用", response));
    }

    /**
     * 停用 LINE Bot
     *
     * @return 更新後的 LINE 設定
     */
    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<LineConfigResponse>> deactivate() {
        log.debug("停用 LINE Bot");

        LineConfigResponse response = lineConfigService.deactivate();

        return ResponseEntity.ok(ApiResponse.ok("LINE Bot 已停用", response));
    }

    /**
     * 測試 LINE Bot 連線
     *
     * @return Bot 資訊（包含 basicId, displayName, pictureUrl, qrCodeUrl, addFriendUrl）
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> testConnection() {
        log.debug("測試 LINE Bot 連線");

        java.util.Map<String, Object> botInfo = lineConfigService.testConnection();

        return ResponseEntity.ok(ApiResponse.ok("連線測試成功", botInfo));
    }

    // ========================================
    // Rich Menu API
    // ========================================

    /**
     * 取得 Rich Menu 資訊
     *
     * @return Rich Menu 資訊
     */
    @GetMapping("/rich-menu")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> getRichMenuInfo() {
        log.debug("取得 Rich Menu 資訊");

        String tenantId = TenantContext.getTenantId();
        java.util.Map<String, String> info = richMenuService.getRichMenuInfo(tenantId);

        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    /**
     * 建立 Rich Menu（使用主題配色）
     *
     * @param request 包含主題參數的請求
     * @return Rich Menu ID
     */
    @PostMapping("/rich-menu/create")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> createRichMenu(
            @RequestBody CreateRichMenuRequest request
    ) {
        log.debug("建立 Rich Menu，主題：{}", request.getTheme());

        String tenantId = TenantContext.getTenantId();
        String theme = request.getTheme() != null ? request.getTheme() : "GREEN";
        String richMenuId = richMenuService.createAndSetRichMenu(tenantId, theme);

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("richMenuId", richMenuId);
        result.put("theme", theme);

        return ResponseEntity.ok(ApiResponse.ok("Rich Menu 建立成功", result));
    }

    /**
     * 上傳自訂 Rich Menu 圖片
     *
     * <p>圖片規格：
     * <ul>
     *   <li>尺寸：2500x843 像素</li>
     *   <li>格式：PNG 或 JPG</li>
     *   <li>大小：最大 1MB</li>
     * </ul>
     *
     * @param file 圖片檔案
     * @return Rich Menu ID
     */
    @PostMapping("/rich-menu/upload-image")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadRichMenuImage(
            @RequestParam("file") MultipartFile file
    ) {
        log.debug("上傳自訂 Rich Menu 圖片");

        try {
            String tenantId = TenantContext.getTenantId();
            byte[] imageBytes = file.getBytes();
            String richMenuId = richMenuService.createRichMenuWithCustomImage(tenantId, imageBytes);

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("richMenuId", richMenuId);
            result.put("theme", "CUSTOM");

            return ResponseEntity.ok(ApiResponse.ok("自訂圖片上傳成功", result));
        } catch (java.io.IOException e) {
            log.error("讀取上傳檔案失敗", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FILE_READ_ERROR", "讀取上傳檔案失敗"));
        }
    }

    /**
     * 建立自訂配置 Rich Menu（自訂圖片+佈局+動作）
     *
     * <p>圖片不疊加文字/圖示，直接上傳。由 config JSON 定義佈局和每格動作。
     *
     * @param file 圖片檔案
     * @param config 配置 JSON 字串
     * @return Rich Menu ID
     */
    @PostMapping("/rich-menu/create-custom")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> createCustomRichMenu(
            @RequestParam("file") MultipartFile file,
            @RequestParam("config") String config
    ) {
        log.debug("建立自訂配置 Rich Menu");

        try {
            String tenantId = TenantContext.getTenantId();
            byte[] imageBytes = file.getBytes();
            String richMenuId = richMenuService.createCustomConfigRichMenu(tenantId, imageBytes, config);

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("richMenuId", richMenuId);
            result.put("theme", "CUSTOM");
            result.put("mode", "CUSTOM");

            return ResponseEntity.ok(ApiResponse.ok("自訂選單建立成功", result));
        } catch (java.io.IOException e) {
            log.error("讀取上傳檔案失敗", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FILE_READ_ERROR", "讀取上傳檔案失敗"));
        }
    }

    /**
     * 刪除 Rich Menu
     *
     * @return 操作結果
     */
    @DeleteMapping("/rich-menu")
    public ResponseEntity<ApiResponse<Void>> deleteRichMenu() {
        log.debug("刪除 Rich Menu");

        String tenantId = TenantContext.getTenantId();
        richMenuService.deleteRichMenu(tenantId);

        return ResponseEntity.ok(ApiResponse.ok("Rich Menu 已刪除", null));
    }
}
