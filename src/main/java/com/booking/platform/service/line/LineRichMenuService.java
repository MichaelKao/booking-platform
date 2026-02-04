package com.booking.platform.service.line;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ErrorCode;
import com.booking.platform.entity.line.TenantLineConfig;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.booking.platform.service.common.EncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * LINE Rich Menu 服務
 *
 * <p>管理 LINE Bot 的 Rich Menu（底部固定選單）
 *
 * <p>Rich Menu 規格：
 * <ul>
 *   <li>尺寸：2500x1686 或 2500x843（全尺寸或半尺寸）</li>
 *   <li>格式：JPEG 或 PNG</li>
 *   <li>檔案大小：最大 1MB</li>
 *   <li>區域：最多 20 個可點擊區域</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 * @see <a href="https://developers.line.biz/en/docs/messaging-api/using-rich-menus/">LINE Rich Menu</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineRichMenuService {

    // ========================================
    // 依賴注入
    // ========================================

    private final TenantLineConfigRepository lineConfigRepository;
    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // ========================================
    // 配置
    // ========================================

    @Value("${line.bot.api-endpoint:https://api.line.me}")
    private String apiEndpoint;

    /**
     * LINE Data API 端點（用於上傳檔案）
     */
    private static final String DATA_API_ENDPOINT = "https://api-data.line.me";

    // ========================================
    // Rich Menu 規格
    // ========================================

    /**
     * Rich Menu 圖片寬度
     */
    private static final int MENU_WIDTH = 2500;

    /**
     * Rich Menu 圖片高度（半尺寸）
     */
    private static final int MENU_HEIGHT = 843;

    /**
     * 選單格數（2行 x 3列）
     */
    private static final int COLS = 3;
    private static final int ROWS = 2;

    /**
     * 每格寬度
     */
    private static final int CELL_WIDTH = MENU_WIDTH / COLS;

    /**
     * 每格高度
     */
    private static final int CELL_HEIGHT = MENU_HEIGHT / ROWS;

    // ========================================
    // API 端點
    // ========================================

    private static final String CREATE_RICH_MENU_API = "/v2/bot/richmenu";
    private static final String UPLOAD_IMAGE_API = "/v2/bot/richmenu/%s/content";
    private static final String SET_DEFAULT_API = "/v2/bot/user/all/richmenu/%s";
    private static final String DELETE_RICH_MENU_API = "/v2/bot/richmenu/%s";
    private static final String GET_DEFAULT_API = "/v2/bot/user/all/richmenu";
    private static final String CANCEL_DEFAULT_API = "/v2/bot/user/all/richmenu";

    // ========================================
    // 顏色設定
    // ========================================

    private static final Color BACKGROUND_COLOR = new Color(0x1D, 0xB4, 0x46);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ICON_BG_COLOR = new Color(255, 255, 255, 50);

    // ========================================
    // 主題配色
    // ========================================

    /**
     * 主題配色對應表
     */
    private static final java.util.Map<String, Color> THEME_COLORS = java.util.Map.of(
            "GREEN", new Color(0x1D, 0xB4, 0x46),   // LINE Green #1DB446
            "BLUE", new Color(0x21, 0x96, 0xF3),    // Ocean Blue #2196F3
            "PURPLE", new Color(0x9C, 0x27, 0xB0),  // Royal Purple #9C27B0
            "ORANGE", new Color(0xFF, 0x57, 0x22),  // Sunset Orange #FF5722
            "DARK", new Color(0x26, 0x32, 0x38)     // Dark Mode #263238
    );

    /**
     * 圖片上傳大小限制（1MB）
     */
    private static final long MAX_IMAGE_SIZE = 1024 * 1024;

    // ========================================
    // 選單項目
    // ========================================

    private static final String[][] MENU_ITEMS = {
            {"開始預約", "start_booking"},
            {"我的預約", "view_bookings"},
            {"瀏覽商品", "start_shopping"},
            {"領取票券", "view_coupons"},
            {"會員資訊", "view_member_info"},
            {"聯絡店家", "contact_shop"}
    };

    // 使用 Unicode Emoji 作為圖示（跨平台支援）
    private static final String[] MENU_ICONS = {
            "📅",  // 開始預約
            "📋",  // 我的預約
            "🛒",  // 瀏覽商品
            "🎁",  // 領取票券
            "👤",  // 會員資訊
            "📞"   // 聯絡店家
    };

    // ========================================
    // 公開方法
    // ========================================

    /**
     * 為租戶建立並設定 Rich Menu
     *
     * @param tenantId 租戶 ID
     * @return Rich Menu ID
     */
    @Transactional
    public String createAndSetRichMenu(String tenantId) {
        return createAndSetRichMenu(tenantId, "GREEN");
    }

    /**
     * 為租戶建立並設定指定主題的 Rich Menu
     *
     * @param tenantId 租戶 ID
     * @param theme 主題配色（GREEN, BLUE, PURPLE, ORANGE, DARK）
     * @return Rich Menu ID
     */
    @Transactional
    public String createAndSetRichMenu(String tenantId, String theme) {
        log.info("開始建立 Rich Menu，租戶：{}，主題：{}", tenantId, theme);

        try {
            // ========================================
            // 1. 刪除現有的 Rich Menu
            // ========================================
            deleteRichMenu(tenantId);

            // ========================================
            // 2. 取得店家名稱
            // ========================================
            String shopName = tenantRepository.findById(tenantId)
                    .map(Tenant::getName)
                    .orElse("預約服務");

            // ========================================
            // 3. 建立 Rich Menu 結構
            // ========================================
            String richMenuId = createRichMenu(tenantId, shopName);
            log.info("Rich Menu 建立成功，ID：{}", richMenuId);

            // ========================================
            // 4. 產生並上傳圖片
            // ========================================
            byte[] imageBytes = generateRichMenuImage(shopName, theme);
            uploadRichMenuImage(tenantId, richMenuId, imageBytes);
            log.info("Rich Menu 圖片上傳成功");

            // ========================================
            // 5. 設為預設選單
            // ========================================
            setDefaultRichMenu(tenantId, richMenuId);
            log.info("Rich Menu 設為預設成功");

            // ========================================
            // 6. 儲存 Rich Menu ID 和主題
            // ========================================
            saveRichMenuIdAndTheme(tenantId, richMenuId, theme);

            return richMenuId;

        } catch (Exception e) {
            log.error("建立 Rich Menu 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗：" + e.getMessage());
        }
    }

    /**
     * 使用自訂圖片建立 Rich Menu
     *
     * @param tenantId 租戶 ID
     * @param imageBytes 圖片位元組陣列
     * @return Rich Menu ID
     */
    @Transactional
    public String createRichMenuWithCustomImage(String tenantId, byte[] imageBytes) {
        log.info("開始建立自訂圖片 Rich Menu，租戶：{}", tenantId);

        try {
            // ========================================
            // 1. 驗證圖片格式
            // ========================================
            validateImage(imageBytes);

            // ========================================
            // 2. 自動縮放圖片到 Rich Menu 尺寸
            // ========================================
            byte[] resizedImageBytes = resizeImageToRichMenuSize(imageBytes);

            // ========================================
            // 3. 刪除現有的 Rich Menu
            // ========================================
            deleteRichMenu(tenantId);

            // ========================================
            // 4. 取得店家名稱
            // ========================================
            String shopName = tenantRepository.findById(tenantId)
                    .map(Tenant::getName)
                    .orElse("預約服務");

            // ========================================
            // 5. 建立 Rich Menu 結構
            // ========================================
            String richMenuId = createRichMenu(tenantId, shopName);
            log.info("Rich Menu 建立成功，ID：{}", richMenuId);

            // ========================================
            // 6. 上傳縮放後的圖片
            // ========================================
            uploadRichMenuImage(tenantId, richMenuId, resizedImageBytes);
            log.info("Rich Menu 自訂圖片上傳成功");

            // ========================================
            // 7. 設為預設選單
            // ========================================
            setDefaultRichMenu(tenantId, richMenuId);
            log.info("Rich Menu 設為預設成功");

            // ========================================
            // 8. 儲存 Rich Menu ID 和主題
            // ========================================
            saveRichMenuIdAndTheme(tenantId, richMenuId, "CUSTOM");

            return richMenuId;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("建立自訂圖片 Rich Menu 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗：" + e.getMessage());
        }
    }

    /**
     * 驗證上傳的圖片（僅檢查格式，不檢查尺寸）
     *
     * @param imageBytes 圖片位元組陣列
     */
    private void validateImage(byte[] imageBytes) {
        // 檢查檔案是否存在
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "請上傳圖片");
        }

        // 檢查圖片格式（嘗試讀取）
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "無法讀取圖片，請確認格式為 PNG 或 JPG");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片格式無效");
        }
    }

    /**
     * 將圖片縮放到 Rich Menu 規格尺寸（2500x843）
     *
     * <p>縮放策略：
     * <ul>
     *   <li>等比例縮放圖片以填滿 2500x843 區域</li>
     *   <li>超出部分會被裁切（置中裁切）</li>
     *   <li>使用高品質縮放演算法</li>
     * </ul>
     *
     * @param imageBytes 原始圖片位元組陣列
     * @return 縮放後的圖片位元組陣列（PNG 格式）
     */
    private byte[] resizeImageToRichMenuSize(byte[] imageBytes) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "無法讀取圖片");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 如果尺寸已正確，直接返回
            if (originalWidth == MENU_WIDTH && originalHeight == MENU_HEIGHT) {
                log.debug("圖片尺寸已正確，無需縮放");
                return imageBytes;
            }

            log.info("縮放圖片：{}x{} → {}x{}", originalWidth, originalHeight, MENU_WIDTH, MENU_HEIGHT);

            // 計算縮放比例（使用 cover 策略，填滿整個區域）
            double scaleX = (double) MENU_WIDTH / originalWidth;
            double scaleY = (double) MENU_HEIGHT / originalHeight;
            double scale = Math.max(scaleX, scaleY);  // 使用較大的比例以填滿

            int scaledWidth = (int) Math.round(originalWidth * scale);
            int scaledHeight = (int) Math.round(originalHeight * scale);

            // 建立縮放後的圖片
            BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();

            // 設定高品質縮放
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();

            // 置中裁切到目標尺寸
            int cropX = (scaledWidth - MENU_WIDTH) / 2;
            int cropY = (scaledHeight - MENU_HEIGHT) / 2;
            BufferedImage finalImage = scaledImage.getSubimage(cropX, cropY, MENU_WIDTH, MENU_HEIGHT);

            // 複製裁切後的圖片（避免 SubImage 的記憶體問題）
            BufferedImage outputImage = new BufferedImage(MENU_WIDTH, MENU_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D outputG2d = outputImage.createGraphics();
            outputG2d.drawImage(finalImage, 0, 0, null);
            outputG2d.dispose();

            // 轉換為 PNG 位元組陣列
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "png", baos);
            byte[] result = baos.toByteArray();

            // 檢查輸出大小
            if (result.length > MAX_IMAGE_SIZE) {
                log.warn("縮放後圖片超過 1MB（{}KB），嘗試轉為 JPEG", result.length / 1024);
                // 嘗試用 JPEG 格式（較小檔案）
                baos = new ByteArrayOutputStream();
                ImageIO.write(outputImage, "jpg", baos);
                result = baos.toByteArray();

                if (result.length > MAX_IMAGE_SIZE) {
                    throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR,
                            "圖片處理後仍超過 1MB，請使用較小的原始圖片");
                }
            }

            log.info("圖片縮放完成，輸出大小：{}KB", result.length / 1024);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("圖片縮放失敗：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片處理失敗：" + e.getMessage());
        }
    }

    /**
     * 取得當前 Rich Menu 資訊
     *
     * @param tenantId 租戶 ID
     * @return Rich Menu 資訊（包含 richMenuId 和 theme）
     */
    public java.util.Map<String, String> getRichMenuInfo(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId).orElse(null);
        if (config == null) {
            return java.util.Map.of();
        }

        java.util.Map<String, String> info = new java.util.HashMap<>();
        if (config.getRichMenuId() != null) {
            info.put("richMenuId", config.getRichMenuId());
        }
        if (config.getRichMenuTheme() != null) {
            info.put("theme", config.getRichMenuTheme());
        }
        return info;
    }

    /**
     * 刪除租戶的 Rich Menu
     *
     * @param tenantId 租戶 ID
     */
    @Transactional
    public void deleteRichMenu(String tenantId) {
        log.info("開始刪除 Rich Menu，租戶：{}", tenantId);

        try {
            TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId).orElse(null);
            if (config == null || config.getRichMenuId() == null) {
                log.info("租戶無 Rich Menu，跳過刪除");
                return;
            }

            String richMenuId = config.getRichMenuId();

            // ========================================
            // 1. 取消預設選單
            // ========================================
            cancelDefaultRichMenu(tenantId);

            // ========================================
            // 2. 刪除 Rich Menu
            // ========================================
            deleteRichMenuById(tenantId, richMenuId);

            // ========================================
            // 3. 清除儲存的 ID
            // ========================================
            config.setRichMenuId(null);
            lineConfigRepository.save(config);

            log.info("Rich Menu 刪除成功，租戶：{}", tenantId);

        } catch (Exception e) {
            log.warn("刪除 Rich Menu 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
            // 刪除失敗不影響主流程
        }
    }

    // ========================================
    // 私有方法 - API 呼叫
    // ========================================

    /**
     * 建立 Rich Menu
     */
    private String createRichMenu(String tenantId, String shopName) {
        String accessToken = getAccessToken(tenantId);

        // 建立請求
        ObjectNode requestBody = buildRichMenuRequest(shopName);
        String url = apiEndpoint + CREATE_RICH_MENU_API;

        log.debug("建立 Rich Menu，URL：{}，請求：{}", url, requestBody.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗");
            }

            String richMenuId = response.getBody().path("richMenuId").asText();
            log.info("Rich Menu 建立成功，ID：{}", richMenuId);
            return richMenuId;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("LINE API 錯誤：{}，回應：{}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.LINE_API_ERROR,
                    "建立 Rich Menu 失敗：" + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    /**
     * 上傳 Rich Menu 圖片
     */
    private void uploadRichMenuImage(String tenantId, String richMenuId, byte[] imageBytes) {
        String accessToken = getAccessToken(tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(imageBytes.length);

        HttpEntity<byte[]> request = new HttpEntity<>(imageBytes, headers);

        String url = DATA_API_ENDPOINT + String.format(UPLOAD_IMAGE_API, richMenuId);
        log.debug("上傳 Rich Menu 圖片，URL：{}，大小：{} bytes", url, imageBytes.length);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.LINE_API_ERROR, "上傳 Rich Menu 圖片失敗");
            }
            log.info("Rich Menu 圖片上傳成功");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("上傳圖片失敗：{}，回應：{}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.LINE_API_ERROR,
                    "上傳 Rich Menu 圖片失敗：" + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    /**
     * 設為預設 Rich Menu
     */
    private void setDefaultRichMenu(String tenantId, String richMenuId) {
        String accessToken = getAccessToken(tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = apiEndpoint + String.format(SET_DEFAULT_API, richMenuId);
        log.debug("設定預設 Rich Menu，URL：{}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("設定預設 Rich Menu 回應非 2xx：{}", response.getStatusCode());
            } else {
                log.info("Rich Menu 設為預設成功");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("設定預設 Rich Menu 失敗：{}，回應：{}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.LINE_API_ERROR,
                    "設定預設 Rich Menu 失敗：" + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    /**
     * 取消預設 Rich Menu
     */
    private void cancelDefaultRichMenu(String tenantId) {
        try {
            String accessToken = getAccessToken(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    apiEndpoint + CANCEL_DEFAULT_API,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (Exception e) {
            log.warn("取消預設 Rich Menu 失敗：{}", e.getMessage());
        }
    }

    /**
     * 刪除 Rich Menu
     */
    private void deleteRichMenuById(String tenantId, String richMenuId) {
        String accessToken = getAccessToken(tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = apiEndpoint + String.format(DELETE_RICH_MENU_API, richMenuId);

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                String.class
        );
    }

    // ========================================
    // 私有方法 - Rich Menu 結構
    // ========================================

    /**
     * 建立 Rich Menu 請求結構
     */
    private ObjectNode buildRichMenuRequest(String shopName) {
        ObjectNode root = objectMapper.createObjectNode();

        // 基本設定
        root.put("selected", true);
        root.put("name", shopName + " - 快捷選單");
        root.put("chatBarText", "點我展開選單");

        // 尺寸
        ObjectNode size = objectMapper.createObjectNode();
        size.put("width", MENU_WIDTH);
        size.put("height", MENU_HEIGHT);
        root.set("size", size);

        // 區域（6 格：2行 x 3列）
        ArrayNode areas = objectMapper.createArrayNode();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                if (index < MENU_ITEMS.length) {
                    areas.add(createAreaObject(row, col, MENU_ITEMS[index][0], MENU_ITEMS[index][1]));
                }
            }
        }

        root.set("areas", areas);

        return root;
    }

    /**
     * 建立區域物件
     */
    private ObjectNode createAreaObject(int row, int col, String label, String action) {
        ObjectNode area = objectMapper.createObjectNode();

        // 邊界
        ObjectNode bounds = objectMapper.createObjectNode();
        bounds.put("x", col * CELL_WIDTH);
        bounds.put("y", row * CELL_HEIGHT);
        bounds.put("width", CELL_WIDTH);
        bounds.put("height", CELL_HEIGHT);
        area.set("bounds", bounds);

        // 動作
        ObjectNode actionNode = objectMapper.createObjectNode();
        actionNode.put("type", "postback");
        actionNode.put("label", label);
        actionNode.put("data", "action=" + action);
        actionNode.put("displayText", label);
        area.set("action", actionNode);

        return area;
    }

    // ========================================
    // 私有方法 - 圖片生成
    // ========================================

    /**
     * 產生 Rich Menu 圖片（使用預設主題）
     */
    private byte[] generateRichMenuImage(String shopName) throws IOException {
        return generateRichMenuImage(shopName, "GREEN");
    }

    /**
     * 產生指定主題的 Rich Menu 圖片
     *
     * @param shopName 店家名稱
     * @param theme 主題配色
     * @return 圖片位元組陣列
     */
    private byte[] generateRichMenuImage(String shopName, String theme) throws IOException {
        BufferedImage image = new BufferedImage(MENU_WIDTH, MENU_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 取得主題顏色
        Color themeColor = THEME_COLORS.getOrDefault(
                theme != null ? theme.toUpperCase() : "GREEN",
                BACKGROUND_COLOR
        );

        // 設定抗鋸齒
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 填充背景
        g2d.setColor(themeColor);
        g2d.fillRect(0, 0, MENU_WIDTH, MENU_HEIGHT);

        // 繪製格線（淡色）
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(2));

        // 垂直線
        for (int col = 1; col < COLS; col++) {
            int x = col * CELL_WIDTH;
            g2d.drawLine(x, 0, x, MENU_HEIGHT);
        }

        // 水平線
        for (int row = 1; row < ROWS; row++) {
            int y = row * CELL_HEIGHT;
            g2d.drawLine(0, y, MENU_WIDTH, y);
        }

        // 繪製每個選單項目
        // 使用跨平台字型載入策略（依序嘗試：思源黑體 > 微軟正黑體 > Noto Sans CJK > 邏輯字型）
        Font iconFont = loadChineseFont(Font.BOLD, 90);
        Font textFont = loadChineseFont(Font.BOLD, 48);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                if (index < MENU_ITEMS.length) {
                    int centerX = col * CELL_WIDTH + CELL_WIDTH / 2;
                    int centerY = row * CELL_HEIGHT + CELL_HEIGHT / 2;

                    // 繪製圖示背景圓圈
                    g2d.setColor(ICON_BG_COLOR);
                    int circleSize = 140;
                    g2d.fillOval(centerX - circleSize / 2, centerY - 100, circleSize, circleSize);

                    // 繪製圖示
                    g2d.setColor(TEXT_COLOR);
                    g2d.setFont(iconFont);
                    FontMetrics iconMetrics = g2d.getFontMetrics();
                    String icon = MENU_ICONS[index];
                    int iconWidth = iconMetrics.stringWidth(icon);
                    g2d.drawString(icon, centerX - iconWidth / 2, centerY - 30);

                    // 繪製文字
                    g2d.setFont(textFont);
                    FontMetrics textMetrics = g2d.getFontMetrics();
                    String text = MENU_ITEMS[index][0];
                    int textWidth = textMetrics.stringWidth(text);
                    g2d.drawString(text, centerX - textWidth / 2, centerY + 100);
                }
            }
        }

        g2d.dispose();

        // 轉換為 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // ========================================
    // 私有方法 - 輔助
    // ========================================

    /**
     * 取得解密後的 Access Token
     */
    private String getAccessToken(String tenantId) {
        TenantLineConfig config = lineConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LINE_CONFIG_NOT_FOUND, "LINE 設定不存在"
                ));

        if (config.getChannelAccessTokenEncrypted() == null) {
            throw new BusinessException(
                    ErrorCode.LINE_CONFIG_INVALID, "LINE Access Token 未設定"
            );
        }

        return encryptionService.decrypt(config.getChannelAccessTokenEncrypted());
    }

    /**
     * 儲存 Rich Menu ID
     */
    private void saveRichMenuId(String tenantId, String richMenuId) {
        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            config.setRichMenuId(richMenuId);
            lineConfigRepository.save(config);
        });
    }

    /**
     * 儲存 Rich Menu ID 和主題
     *
     * @param tenantId 租戶 ID
     * @param richMenuId Rich Menu ID
     * @param theme 主題
     */
    private void saveRichMenuIdAndTheme(String tenantId, String richMenuId, String theme) {
        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            config.setRichMenuId(richMenuId);
            config.setRichMenuTheme(theme);
            lineConfigRepository.save(config);
        });
    }

    /**
     * 載入支援中文的字型（跨平台相容）
     *
     * <p>依序嘗試以下字型：
     * <ol>
     *   <li>WenQuanYi Zen Hei（Docker/Linux - 文泉驛正黑，優先使用）</li>
     *   <li>WenQuanYi Micro Hei（Linux - 文泉驛微米黑）</li>
     *   <li>Noto Sans CJK TC（Linux - Google Noto）</li>
     *   <li>Microsoft JhengHei（Windows - 微軟正黑體）</li>
     *   <li>PingFang TC（macOS - 蘋方）</li>
     *   <li>SansSerif（Java 邏輯字型，最後備援）</li>
     * </ol>
     *
     * @param style 字型樣式（Font.PLAIN, Font.BOLD 等）
     * @param size 字型大小
     * @return 可用的中文字型
     */
    private Font loadChineseFont(int style, int size) {
        // 候選字型列表（依優先順序）
        // 注意：Docker 環境安裝的是 font-wqy-zenhei（文泉驛正黑）
        String[] fontCandidates = {
                "WenQuanYi Zen Hei",     // Docker/Linux - 文泉驛正黑（優先使用）
                "WenQuanYi Micro Hei",   // Linux - 文泉驛微米黑
                "Noto Sans CJK TC",      // Linux - Google Noto 字型
                "Noto Sans TC",          // Linux - Noto 變體
                "Droid Sans Fallback",   // Linux/Android
                "Microsoft JhengHei",    // Windows - 微軟正黑體
                "Microsoft YaHei",       // Windows - 微軟雅黑
                "PingFang TC",           // macOS - 蘋方繁體
                "Heiti TC",              // macOS - 黑體繁體
                "SimHei",                // Windows - 黑體
                "SansSerif"              // Java 邏輯字型（最後備援）
        };

        // 取得系統可用字型
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> availableFonts = new java.util.HashSet<>();
        for (String fontName : ge.getAvailableFontFamilyNames()) {
            availableFonts.add(fontName);
        }

        // 嘗試找到可用的中文字型
        for (String fontName : fontCandidates) {
            if (availableFonts.contains(fontName)) {
                Font font = new Font(fontName, style, size);
                // 驗證字型確實可以顯示中文
                if (font.canDisplay('預')) {
                    log.debug("使用字型：{}", fontName);
                    return font;
                }
            }
        }

        // 如果都找不到，使用 SansSerif 並記錄警告
        log.warn("找不到支援中文的字型，使用預設 SansSerif（可能無法正確顯示中文）");
        log.warn("可用字型列表：{}", String.join(", ", availableFonts));
        return new Font("SansSerif", style, size);
    }
}
