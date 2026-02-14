package com.booking.platform.controller.tenant;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.line.CreateRichMenuRequest;
import com.booking.platform.dto.line.LineConfigResponse;
import com.booking.platform.dto.line.SaveLineConfigRequest;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.FeatureService;
import com.booking.platform.service.line.LineConfigService;
import com.booking.platform.service.line.LineRichMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    private final TenantLineConfigRepository lineConfigRepository;
    private final FeatureService featureService;

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
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "textColor", required = false) String textColor,
            @RequestParam(value = "noOverlay", required = false, defaultValue = "false") boolean noOverlay
    ) {
        log.debug("上傳自訂 Rich Menu 圖片，文字顏色：{}，不疊加：{}", textColor, noOverlay);

        try {
            String tenantId = TenantContext.getTenantId();
            // 自訂選單需訂閱 CUSTOM_RICH_MENU
            featureService.checkFeatureEnabled(tenantId, FeatureCode.CUSTOM_RICH_MENU);
            byte[] imageBytes = file.getBytes();
            String richMenuId = richMenuService.createRichMenuWithCustomImage(tenantId, imageBytes, textColor, noOverlay);

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
            // 自訂選單需訂閱 CUSTOM_RICH_MENU
            featureService.checkFeatureEnabled(tenantId, FeatureCode.CUSTOM_RICH_MENU);
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

    // ========================================
    // 進階自訂 Rich Menu API（付費功能：CUSTOM_RICH_MENU）
    // ========================================

    /**
     * 建立進階自訂 Rich Menu（需訂閱 CUSTOM_RICH_MENU）
     *
     * <p>支援：上傳背景圖、每格獨立圖示、自訂文字標籤、大尺寸佈局
     *
     * @param backgroundImage 背景圖片（可選）
     * @param config 配置 JSON 字串
     * @return Rich Menu ID
     */
    @PostMapping("/rich-menu/create-advanced")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> createAdvancedRichMenu(
            @RequestParam(value = "backgroundImage", required = false) MultipartFile backgroundImage,
            @RequestParam("config") String config,
            @RequestParam(value = "cellIcon_0", required = false) MultipartFile cellIcon0,
            @RequestParam(value = "cellIcon_1", required = false) MultipartFile cellIcon1,
            @RequestParam(value = "cellIcon_2", required = false) MultipartFile cellIcon2,
            @RequestParam(value = "cellIcon_3", required = false) MultipartFile cellIcon3,
            @RequestParam(value = "cellIcon_4", required = false) MultipartFile cellIcon4,
            @RequestParam(value = "cellIcon_5", required = false) MultipartFile cellIcon5,
            @RequestParam(value = "cellIcon_6", required = false) MultipartFile cellIcon6,
            @RequestParam(value = "cellIcon_7", required = false) MultipartFile cellIcon7,
            @RequestParam(value = "cellIcon_8", required = false) MultipartFile cellIcon8,
            @RequestParam(value = "cellIcon_9", required = false) MultipartFile cellIcon9,
            @RequestParam(value = "cellIcon_10", required = false) MultipartFile cellIcon10,
            @RequestParam(value = "cellIcon_11", required = false) MultipartFile cellIcon11
    ) {
        log.debug("建立進階自訂 Rich Menu");

        try {
            String tenantId = TenantContext.getTenantId();

            // 需訂閱 CUSTOM_RICH_MENU 才能發布
            featureService.checkFeatureEnabled(tenantId, FeatureCode.CUSTOM_RICH_MENU);

            // 背景圖片
            byte[] bgBytes = backgroundImage != null && !backgroundImage.isEmpty()
                    ? backgroundImage.getBytes() : null;

            // 收集每格圖示
            MultipartFile[] iconFiles = {
                    cellIcon0, cellIcon1, cellIcon2, cellIcon3,
                    cellIcon4, cellIcon5, cellIcon6, cellIcon7,
                    cellIcon8, cellIcon9, cellIcon10, cellIcon11
            };
            java.util.Map<Integer, byte[]> cellIcons = new java.util.HashMap<>();
            for (int i = 0; i < iconFiles.length; i++) {
                if (iconFiles[i] != null && !iconFiles[i].isEmpty()) {
                    cellIcons.put(i, iconFiles[i].getBytes());
                }
            }

            String richMenuId = richMenuService.createAdvancedRichMenu(
                    tenantId, bgBytes, cellIcons, config
            );

            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("richMenuId", richMenuId);
            result.put("mode", "ADVANCED");

            return ResponseEntity.ok(ApiResponse.ok("進階自訂選單建立成功", result));

        } catch (java.io.IOException e) {
            log.error("讀取上傳檔案失敗", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FILE_READ_ERROR", "讀取上傳檔案失敗"));
        }
    }

    /**
     * 產生進階 Rich Menu 預覽圖（不需訂閱，免費可用）
     *
     * @return 預覽圖片（image/png）
     */
    @PostMapping("/rich-menu/preview-advanced")
    public ResponseEntity<byte[]> previewAdvancedRichMenu(
            @RequestParam(value = "backgroundImage", required = false) MultipartFile backgroundImage,
            @RequestParam("config") String config,
            @RequestParam(value = "cellIcon_0", required = false) MultipartFile cellIcon0,
            @RequestParam(value = "cellIcon_1", required = false) MultipartFile cellIcon1,
            @RequestParam(value = "cellIcon_2", required = false) MultipartFile cellIcon2,
            @RequestParam(value = "cellIcon_3", required = false) MultipartFile cellIcon3,
            @RequestParam(value = "cellIcon_4", required = false) MultipartFile cellIcon4,
            @RequestParam(value = "cellIcon_5", required = false) MultipartFile cellIcon5,
            @RequestParam(value = "cellIcon_6", required = false) MultipartFile cellIcon6,
            @RequestParam(value = "cellIcon_7", required = false) MultipartFile cellIcon7,
            @RequestParam(value = "cellIcon_8", required = false) MultipartFile cellIcon8,
            @RequestParam(value = "cellIcon_9", required = false) MultipartFile cellIcon9,
            @RequestParam(value = "cellIcon_10", required = false) MultipartFile cellIcon10,
            @RequestParam(value = "cellIcon_11", required = false) MultipartFile cellIcon11
    ) {
        log.debug("產生進階 Rich Menu 預覽圖");

        try {
            // 背景圖片
            byte[] bgBytes = backgroundImage != null && !backgroundImage.isEmpty()
                    ? backgroundImage.getBytes() : null;

            // 收集每格圖示
            MultipartFile[] iconFiles = {
                    cellIcon0, cellIcon1, cellIcon2, cellIcon3,
                    cellIcon4, cellIcon5, cellIcon6, cellIcon7,
                    cellIcon8, cellIcon9, cellIcon10, cellIcon11
            };
            java.util.Map<Integer, byte[]> cellIcons = new java.util.HashMap<>();
            for (int i = 0; i < iconFiles.length; i++) {
                if (iconFiles[i] != null && !iconFiles[i].isEmpty()) {
                    cellIcons.put(i, iconFiles[i].getBytes());
                }
            }

            byte[] previewImage = richMenuService.composeAdvancedRichMenuImage(
                    bgBytes, cellIcons, config
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(previewImage);

        } catch (java.io.IOException e) {
            log.error("產生預覽圖失敗", e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"預覽圖生成失敗：" + e.getMessage() + "\"}").getBytes());
        } catch (Exception e) {
            log.error("產生預覽圖失敗", e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"預覽圖生成失敗：" + e.getMessage() + "\"}").getBytes());
        }
    }

    /**
     * 取得進階 Rich Menu 配置
     *
     * @return 配置 JSON
     */
    @GetMapping("/rich-menu/advanced-config")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAdvancedRichMenuConfig() {
        log.debug("取得進階 Rich Menu 配置");

        String tenantId = TenantContext.getTenantId();
        String configJson = richMenuService.getAdvancedConfig(tenantId);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (configJson != null && !configJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                result = mapper.readValue(configJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析進階 Rich Menu 配置失敗，租戶：{}", tenantId);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 儲存進階 Rich Menu 配置草稿（不發布到 LINE，不需訂閱）
     *
     * @param configMap 配置 JSON
     * @return 儲存結果
     */
    @PutMapping("/rich-menu/advanced-config")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> saveAdvancedRichMenuConfig(
            @RequestBody java.util.Map<String, Object> configMap
    ) {
        log.debug("儲存進階 Rich Menu 配置草稿");

        String tenantId = TenantContext.getTenantId();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String configJson = mapper.writeValueAsString(configMap);
            richMenuService.saveAdvancedConfig(tenantId, configJson);
        } catch (Exception e) {
            log.error("儲存進階 Rich Menu 配置失敗，租戶：{}", tenantId, e);
        }

        return ResponseEntity.ok(ApiResponse.ok("配置已儲存", configMap));
    }

    // ========================================
    // Flex Menu 自訂 API
    // ========================================

    /**
     * 取得 Flex Menu 主選單配置
     *
     * @return Flex Menu 配置 JSON
     */
    @GetMapping("/flex-menu")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getFlexMenuConfig() {
        log.debug("取得 Flex Menu 配置");

        String tenantId = TenantContext.getTenantId();
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            String flexConfig = config.getFlexMenuConfig();
            if (flexConfig != null && !flexConfig.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    result.putAll(mapper.readValue(flexConfig, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}));
                } catch (Exception e) {
                    log.warn("解析 flexMenuConfig 失敗，租戶：{}", tenantId);
                }
            }
        });

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 更新 Flex Menu 主選單配置
     *
     * @param configMap 配置 JSON
     * @return 更新結果
     */
    @PutMapping("/flex-menu")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> updateFlexMenuConfig(
            @RequestBody java.util.Map<String, Object> configMap
    ) {
        log.debug("更新 Flex Menu 配置");

        String tenantId = TenantContext.getTenantId();

        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                config.setFlexMenuConfig(mapper.writeValueAsString(configMap));
                lineConfigRepository.save(config);
            } catch (Exception e) {
                log.error("儲存 flexMenuConfig 失敗，租戶：{}", tenantId, e);
            }
        });

        return ResponseEntity.ok(ApiResponse.ok("主選單樣式已儲存", configMap));
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
