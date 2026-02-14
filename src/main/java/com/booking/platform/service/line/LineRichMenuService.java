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
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
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
     * Rich Menu 圖片高度（全尺寸，3 行以上）
     */
    private static final int MENU_HEIGHT_FULL = 1686;

    /**
     * 預設佈局：上排 3 格 + 下排 4 格
     */
    private static final String DEFAULT_LAYOUT = "3+4";

    /**
     * 上排高度
     */
    private static final int ROW_TOP_HEIGHT = MENU_HEIGHT / 2;

    /**
     * 下排高度
     */
    private static final int ROW_BOTTOM_HEIGHT = MENU_HEIGHT - ROW_TOP_HEIGHT;

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

    // 選單項目（7 個，與主選單 Flex Message 同步）
    // 上排 3 個、下排 4 個
    private static final String[][] ROW1_ITEMS = {
            {"開始預約", "start_booking"},
            {"我的預約", "view_bookings"},
            {"瀏覽商品", "start_shopping"}
    };
    private static final String[][] ROW2_ITEMS = {
            {"領取票券", "view_coupons"},
            {"我的票券", "view_my_coupons"},
            {"會員資訊", "view_member_info"},
            {"聯絡店家", "contact_shop"}
    };

    // 合併為完整列表（供圖片渲染使用）
    private static final String[][] MENU_ITEMS = {
            {"開始預約", "start_booking"},
            {"我的預約", "view_bookings"},
            {"瀏覽商品", "start_shopping"},
            {"領取票券", "view_coupons"},
            {"我的票券", "view_my_coupons"},
            {"會員資訊", "view_member_info"},
            {"聯絡店家", "contact_shop"}
    };

    // 圖示類型（使用 Java2D 繪製向量圖示，避免 emoji 字型問題）
    private enum IconType {
        CALENDAR,    // 開始預約 - 日曆
        CLIPBOARD,   // 我的預約 - 剪貼板
        CART,        // 瀏覽商品 - 購物車
        TICKET,      // 領取票券 - 票券
        GIFT,        // 我的票券 - 禮物
        PERSON,      // 會員資訊 - 人像
        PHONE        // 聯絡店家 - 電話
    }

    private static final IconType[] MENU_ICON_TYPES = {
            IconType.CALENDAR,
            IconType.CLIPBOARD,
            IconType.CART,
            IconType.TICKET,
            IconType.GIFT,
            IconType.PERSON,
            IconType.PHONE
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
     * <p>圖片會作為背景，選單文字和圖示會疊加在上方
     *
     * @param tenantId 租戶 ID
     * @param imageBytes 圖片位元組陣列
     * @return Rich Menu ID
     */
    @Transactional
    public String createRichMenuWithCustomImage(String tenantId, byte[] imageBytes) {
        return createRichMenuWithCustomImage(tenantId, imageBytes, null);
    }

    /**
     * 建立自訂圖片 Rich Menu（帶文字顏色）
     *
     * @param tenantId 租戶 ID
     * @param imageBytes 圖片位元組陣列
     * @param textColorHex 文字顏色（十六進位，如 #FFFFFF），null 則預設白色
     * @return Rich Menu ID
     */
    @Transactional
    public String createRichMenuWithCustomImage(String tenantId, byte[] imageBytes, String textColorHex) {
        return createRichMenuWithCustomImage(tenantId, imageBytes, textColorHex, false);
    }

    /**
     * 建立自訂圖片 Rich Menu（帶文字顏色，可選不疊加文字圖示）
     *
     * @param tenantId 租戶 ID
     * @param imageBytes 圖片位元組陣列
     * @param textColorHex 文字顏色（十六進位，如 #FFFFFF），null 則預設白色
     * @param noOverlay true 時跳過疊加文字和圖示，直接使用背景圖
     * @return Rich Menu ID
     */
    @Transactional
    public String createRichMenuWithCustomImage(String tenantId, byte[] imageBytes, String textColorHex, boolean noOverlay) {
        log.info("開始建立自訂圖片 Rich Menu，租戶：{}，noOverlay：{}", tenantId, noOverlay);

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
            // 3. 決定最終圖片（是否疊加文字圖示）
            // ========================================
            byte[] finalImageBytes;
            if (noOverlay) {
                // 不疊加文字圖示，直接使用縮放後的背景圖
                finalImageBytes = resizedImageBytes;
                log.info("noOverlay=true，跳過疊加文字圖示");
            } else {
                // 解析文字顏色
                Color overlayTextColor = TEXT_COLOR;
                if (textColorHex != null && textColorHex.startsWith("#") && textColorHex.length() == 7) {
                    try {
                        overlayTextColor = Color.decode(textColorHex);
                    } catch (NumberFormatException e) {
                        log.warn("無效的文字顏色：{}，使用預設白色", textColorHex);
                    }
                }
                // 在背景圖片上疊加選單文字和圖示
                finalImageBytes = overlayMenuItemsOnImage(resizedImageBytes, overlayTextColor);
            }

            // ========================================
            // 4. 刪除現有的 Rich Menu
            // ========================================
            deleteRichMenu(tenantId);

            // ========================================
            // 5. 取得店家名稱
            // ========================================
            String shopName = tenantRepository.findById(tenantId)
                    .map(Tenant::getName)
                    .orElse("預約服務");

            // ========================================
            // 6. 建立 Rich Menu 結構
            // ========================================
            String richMenuId = createRichMenu(tenantId, shopName);
            log.info("Rich Menu 建立成功，ID：{}", richMenuId);

            // ========================================
            // 7. 上傳圖片
            // ========================================
            uploadRichMenuImage(tenantId, richMenuId, finalImageBytes);
            log.info("Rich Menu 自訂圖片上傳成功");

            // ========================================
            // 8. 設為預設選單
            // ========================================
            setDefaultRichMenu(tenantId, richMenuId);
            log.info("Rich Menu 設為預設成功");

            // ========================================
            // 9. 儲存 Rich Menu ID 和主題
            // ========================================
            saveRichMenuIdAndTheme(tenantId, richMenuId, "CUSTOM_BG");

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
     * 在背景圖片上疊加選單文字和圖示（描邊文字，不遮蓋背景）
     *
     * <p>此方法會在自訂背景圖片上繪製：
     * <ul>
     *   <li>淡色格線（區分各格）</li>
     *   <li>描邊向量圖示（可在任何背景上清晰可見）</li>
     *   <li>描邊選單文字（使用 TextLayout 描邊，無需半透明遮罩）</li>
     * </ul>
     *
     * @param backgroundImageBytes 背景圖片位元組陣列（已縮放至 2500x843）
     * @param textColor 文字與圖示顏色
     * @return 合成後的圖片位元組陣列
     */
    private byte[] overlayMenuItemsOnImage(byte[] backgroundImageBytes, Color textColor) {
        try {
            // 讀取背景圖片
            BufferedImage backgroundImage = ImageIO.read(new java.io.ByteArrayInputStream(backgroundImageBytes));
            if (backgroundImage == null) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "無法讀取背景圖片");
            }

            // 建立可繪製的圖片副本
            BufferedImage composite = new BufferedImage(MENU_WIDTH, MENU_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = composite.createGraphics();

            // 設定高品質繪圖
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // 繪製背景圖片
            g2d.drawImage(backgroundImage, 0, 0, MENU_WIDTH, MENU_HEIGHT, null);

            // 取得佈局區域
            int[][] layoutAreas = getLayoutAreas(DEFAULT_LAYOUT);

            // 繪製淡色格線（區分各格）
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.setStroke(new BasicStroke(2));
            drawGridLines(g2d, layoutAreas);

            // 計算描邊顏色（文字亮→黑色描邊，文字暗→白色描邊）
            Color outlineColor = getContrastOutlineColor(textColor);

            // 在每個格子中央繪製描邊圖示 + 描邊文字（不遮蓋背景圖）
            Font textFont = loadChineseFont(Font.BOLD, 72);

            for (int i = 0; i < layoutAreas.length && i < MENU_ITEMS.length; i++) {
                drawMenuCellOutlined(g2d, layoutAreas[i], i, textFont, textColor, outlineColor);
            }

            g2d.dispose();

            // 轉換為 PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(composite, "png", baos);
            byte[] result = baos.toByteArray();

            // 檢查大小
            if (result.length > MAX_IMAGE_SIZE) {
                log.warn("合成圖片超過 1MB（{}KB），嘗試轉為 JPEG", result.length / 1024);
                baos = new ByteArrayOutputStream();
                ImageIO.write(composite, "jpg", baos);
                result = baos.toByteArray();
            }

            log.info("圖片合成完成，輸出大小：{}KB", result.length / 1024);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("圖片合成失敗：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片處理失敗：" + e.getMessage());
        }
    }

    /**
     * 根據文字顏色計算對比描邊顏色
     * 亮色文字 → 黑色描邊，暗色文字 → 白色描邊
     */
    private Color getContrastOutlineColor(Color textColor) {
        double luminance = (0.299 * textColor.getRed() + 0.587 * textColor.getGreen() + 0.114 * textColor.getBlue()) / 255.0;
        return luminance > 0.5 ? new Color(0, 0, 0, 200) : new Color(255, 255, 255, 200);
    }

    /**
     * 在格子中央繪製描邊圖示和描邊文字（無背景遮罩，背景圖完全可見）
     *
     * @param g2d Graphics2D 繪圖物件
     * @param area 區域座標 {x, y, width, height}
     * @param index 選單項目索引
     * @param textFont 文字字型
     * @param textColor 文字填充顏色
     * @param outlineColor 描邊顏色
     */
    private void drawMenuCellOutlined(Graphics2D g2d, int[] area, int index, Font textFont, Color textColor, Color outlineColor) {
        if (index >= MENU_ITEMS.length || index >= MENU_ICON_TYPES.length) return;

        int cellX = area[0];
        int cellY = area[1];
        int cellW = area[2];
        int cellH = area[3];
        int centerX = cellX + cellW / 2;
        int centerY = cellY + cellH / 2;

        // 根據格子大小調整尺寸
        int iconSize = Math.min(cellW, cellH) / 5;
        int circleSize = iconSize * 2;
        int fontSize = Math.max(36, Math.min(72, cellW / 12));

        // ========================================
        // 1. 繪製描邊圖示（先畫粗描邊，再畫正常圖示）
        // ========================================
        int iconCenterY = centerY - iconSize / 4;

        // 描邊層（加粗描邊色）
        int outlineStrokeWidth = Math.max(10, iconSize / 4);
        g2d.setStroke(new BasicStroke(outlineStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(outlineColor);
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX, iconCenterY, iconSize);

        // 正常層（文字色）
        int normalStrokeWidth = Math.max(6, iconSize / 8);
        g2d.setStroke(new BasicStroke(normalStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(textColor);
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX, iconCenterY, iconSize);

        // ========================================
        // 2. 繪製描邊文字（使用 TextLayout 描邊，清晰可讀）
        // ========================================
        Font cellFont = textFont.deriveFont((float) fontSize);
        g2d.setFont(cellFont);
        FontMetrics fm = g2d.getFontMetrics();
        String text = MENU_ITEMS[index][0];
        int textWidth = fm.stringWidth(text);
        int textX = centerX - textWidth / 2;
        int textY = centerY + circleSize / 2 + fm.getAscent();

        FontRenderContext frc = g2d.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text, cellFont, frc);
        AffineTransform transform = AffineTransform.getTranslateInstance(textX, textY);
        Shape textShape = textLayout.getOutline(transform);

        // 描邊層
        g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(outlineColor);
        g2d.draw(textShape);

        // 填充層
        g2d.setColor(textColor);
        g2d.fill(textShape);
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
        if (config.getRichMenuMode() != null) {
            info.put("mode", config.getRichMenuMode());
        }
        if (config.getRichMenuCustomConfig() != null) {
            info.put("customConfig", config.getRichMenuCustomConfig());
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
     * 建立 Rich Menu 請求結構（預設 3+4 佈局）
     */
    private ObjectNode buildRichMenuRequest(String shopName) {
        return buildRichMenuRequestWithLayout(shopName, DEFAULT_LAYOUT, MENU_ITEMS);
    }

    /**
     * 使用指定佈局和選單項目建立 Rich Menu 請求結構
     */
    private ObjectNode buildRichMenuRequestWithLayout(String shopName, String layout, String[][] menuItems) {
        int menuHeight = getMenuHeightForLayout(layout);
        ObjectNode root = objectMapper.createObjectNode();

        // 基本設定
        root.put("selected", true);
        root.put("name", shopName + " - 快捷選單");
        root.put("chatBarText", "點我展開選單");

        // 尺寸
        ObjectNode size = objectMapper.createObjectNode();
        size.put("width", MENU_WIDTH);
        size.put("height", menuHeight);
        root.set("size", size);

        // 區域（依佈局動態計算）
        ArrayNode areas = objectMapper.createArrayNode();
        int[][] layoutAreas = getLayoutAreasWithHeight(layout, menuHeight);

        for (int i = 0; i < layoutAreas.length && i < menuItems.length; i++) {
            areas.add(createAreaObject(
                    layoutAreas[i][0], layoutAreas[i][1],
                    layoutAreas[i][2], layoutAreas[i][3],
                    menuItems[i][0], menuItems[i][1]
            ));
        }

        root.set("areas", areas);

        return root;
    }

    /**
     * 建立區域物件（使用明確的座標和尺寸）
     */
    private ObjectNode createAreaObject(int x, int y, int width, int height, String label, String action) {
        ObjectNode area = objectMapper.createObjectNode();

        // 邊界
        ObjectNode bounds = objectMapper.createObjectNode();
        bounds.put("x", x);
        bounds.put("y", y);
        bounds.put("width", width);
        bounds.put("height", height);
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

    /**
     * 取得佈局的區域定義（每個區域為 {x, y, width, height}）
     */
    private int[][] getLayoutAreas(String layout) {
        return getLayoutAreasWithHeight(layout, MENU_HEIGHT);
    }

    /**
     * 取得佈局的區域定義（支援指定高度）
     *
     * @param layout 佈局代碼
     * @param menuHeight 選單高度（843 或 1686）
     * @return 區域陣列
     */
    private int[][] getLayoutAreasWithHeight(String layout, int menuHeight) {
        int rowTopH = menuHeight / 2;
        int rowBottomH = menuHeight - rowTopH;

        switch (layout) {
            case "3+4": {
                int topW = MENU_WIDTH / 3;
                int topLastW = MENU_WIDTH - topW * 2;
                int bottomW = MENU_WIDTH / 4;
                return new int[][] {
                        {0, 0, topW, rowTopH},
                        {topW, 0, topW, rowTopH},
                        {topW * 2, 0, topLastW, rowTopH},
                        {0, rowTopH, bottomW, rowBottomH},
                        {bottomW, rowTopH, bottomW, rowBottomH},
                        {bottomW * 2, rowTopH, bottomW, rowBottomH},
                        {bottomW * 3, rowTopH, MENU_WIDTH - bottomW * 3, rowBottomH}
                };
            }
            case "2x3": {
                int cellW = MENU_WIDTH / 3;
                int lastW = MENU_WIDTH - cellW * 2;
                return new int[][] {
                        {0, 0, cellW, rowTopH},
                        {cellW, 0, cellW, rowTopH},
                        {cellW * 2, 0, lastW, rowTopH},
                        {0, rowTopH, cellW, rowBottomH},
                        {cellW, rowTopH, cellW, rowBottomH},
                        {cellW * 2, rowTopH, lastW, rowBottomH}
                };
            }
            case "2+3": {
                int topW = MENU_WIDTH / 2;
                int bottomW = MENU_WIDTH / 3;
                int bottomLastW = MENU_WIDTH - bottomW * 2;
                return new int[][] {
                        {0, 0, topW, rowTopH},
                        {topW, 0, MENU_WIDTH - topW, rowTopH},
                        {0, rowTopH, bottomW, rowBottomH},
                        {bottomW, rowTopH, bottomW, rowBottomH},
                        {bottomW * 2, rowTopH, bottomLastW, rowBottomH}
                };
            }
            case "2x2": {
                int cellW = MENU_WIDTH / 2;
                return new int[][] {
                        {0, 0, cellW, rowTopH},
                        {cellW, 0, MENU_WIDTH - cellW, rowTopH},
                        {0, rowTopH, cellW, rowBottomH},
                        {cellW, rowTopH, MENU_WIDTH - cellW, rowBottomH}
                };
            }
            case "1+2": {
                int bottomW = MENU_WIDTH / 2;
                return new int[][] {
                        {0, 0, MENU_WIDTH, rowTopH},
                        {0, rowTopH, bottomW, rowBottomH},
                        {bottomW, rowTopH, MENU_WIDTH - bottomW, rowBottomH}
                };
            }
            // ========================================
            // 大尺寸佈局（Full: 2500x1686，3 行以上）
            // ========================================
            case "3+4+4": {
                int rowH = menuHeight / 3;
                int lastRowH = menuHeight - rowH * 2;
                int topW = MENU_WIDTH / 3;
                int topLastW = MENU_WIDTH - topW * 2;
                int midW = MENU_WIDTH / 4;
                int bottomW = MENU_WIDTH / 4;
                return new int[][] {
                        // 上排 3 格
                        {0, 0, topW, rowH},
                        {topW, 0, topW, rowH},
                        {topW * 2, 0, topLastW, rowH},
                        // 中排 4 格
                        {0, rowH, midW, rowH},
                        {midW, rowH, midW, rowH},
                        {midW * 2, rowH, midW, rowH},
                        {midW * 3, rowH, MENU_WIDTH - midW * 3, rowH},
                        // 下排 4 格
                        {0, rowH * 2, bottomW, lastRowH},
                        {bottomW, rowH * 2, bottomW, lastRowH},
                        {bottomW * 2, rowH * 2, bottomW, lastRowH},
                        {bottomW * 3, rowH * 2, MENU_WIDTH - bottomW * 3, lastRowH}
                };
            }
            case "3+4+4+1": {
                int rowH = menuHeight / 4;
                int lastRowH = menuHeight - rowH * 3;
                int topW = MENU_WIDTH / 3;
                int topLastW = MENU_WIDTH - topW * 2;
                int midW = MENU_WIDTH / 4;
                return new int[][] {
                        // 上排 3 格
                        {0, 0, topW, rowH},
                        {topW, 0, topW, rowH},
                        {topW * 2, 0, topLastW, rowH},
                        // 中排 4 格
                        {0, rowH, midW, rowH},
                        {midW, rowH, midW, rowH},
                        {midW * 2, rowH, midW, rowH},
                        {midW * 3, rowH, MENU_WIDTH - midW * 3, rowH},
                        // 下排 4 格
                        {0, rowH * 2, midW, rowH},
                        {midW, rowH * 2, midW, rowH},
                        {midW * 2, rowH * 2, midW, rowH},
                        {midW * 3, rowH * 2, MENU_WIDTH - midW * 3, rowH},
                        // 底部 1 格（全寬）
                        {0, rowH * 3, MENU_WIDTH, lastRowH}
                };
            }
            case "1+4+4": {
                int rowH = menuHeight / 3;
                int lastRowH = menuHeight - rowH * 2;
                int midW = MENU_WIDTH / 4;
                return new int[][] {
                        // 上排 1 格（Logo 滿版）
                        {0, 0, MENU_WIDTH, rowH},
                        // 中排 4 格
                        {0, rowH, midW, rowH},
                        {midW, rowH, midW, rowH},
                        {midW * 2, rowH, midW, rowH},
                        {midW * 3, rowH, MENU_WIDTH - midW * 3, rowH},
                        // 下排 4 格
                        {0, rowH * 2, midW, lastRowH},
                        {midW, rowH * 2, midW, lastRowH},
                        {midW * 2, rowH * 2, midW, lastRowH},
                        {midW * 3, rowH * 2, MENU_WIDTH - midW * 3, lastRowH}
                };
            }
            case "4+4": {
                int rowH = menuHeight / 2;
                int lastRowH = menuHeight - rowH;
                int cellW = MENU_WIDTH / 4;
                return new int[][] {
                        // 上排 4 格
                        {0, 0, cellW, rowH},
                        {cellW, 0, cellW, rowH},
                        {cellW * 2, 0, cellW, rowH},
                        {cellW * 3, 0, MENU_WIDTH - cellW * 3, rowH},
                        // 下排 4 格
                        {0, rowH, cellW, lastRowH},
                        {cellW, rowH, cellW, lastRowH},
                        {cellW * 2, rowH, cellW, lastRowH},
                        {cellW * 3, rowH, MENU_WIDTH - cellW * 3, lastRowH}
                };
            }
            default:
                // 支援自訂格數佈局（格式：custom_RxC，例如 custom_3x4）
                if (layout != null && layout.startsWith("custom_")) {
                    return parseCustomGridLayout(layout, menuHeight);
                }
                return getLayoutAreasWithHeight(DEFAULT_LAYOUT, MENU_HEIGHT);
        }
    }

    /**
     * 解析自訂格數佈局（custom_RxC）
     *
     * @param layout 佈局代碼（如 custom_2x3, custom_3x4）
     * @param menuHeight 選單高度
     * @return 均等分割的區域陣列
     */
    private int[][] parseCustomGridLayout(String layout, int menuHeight) {
        try {
            String grid = layout.replace("custom_", "");
            String[] parts = grid.split("x");
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);

            // 限制範圍
            rows = Math.max(1, Math.min(rows, 4));
            cols = Math.max(1, Math.min(cols, 5));

            int cellCount = rows * cols;
            int[][] areas = new int[cellCount][4];
            int cellW = MENU_WIDTH / cols;
            int cellH = menuHeight / rows;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    int x = c * cellW;
                    int y = r * cellH;
                    // 最後一列/行使用剩餘空間，避免像素縫隙
                    int w = (c == cols - 1) ? (MENU_WIDTH - x) : cellW;
                    int h = (r == rows - 1) ? (menuHeight - y) : cellH;
                    areas[idx] = new int[]{x, y, w, h};
                }
            }
            return areas;
        } catch (Exception e) {
            log.warn("解析自訂佈局失敗：{}，使用預設佈局", layout);
            return getLayoutAreasWithHeight(DEFAULT_LAYOUT, MENU_HEIGHT);
        }
    }

    /**
     * 判斷佈局是否為大尺寸（Full: 2500x1686）
     */
    private boolean isFullSizeLayout(String layout) {
        if (layout == null) return false;
        // 自訂佈局 3 行以上為大尺寸
        if (layout.startsWith("custom_")) {
            try {
                int rows = Integer.parseInt(layout.replace("custom_", "").split("x")[0]);
                return rows > 2;
            } catch (Exception e) { return false; }
        }
        return layout.equals("3+4+4") ||
                layout.equals("3+4+4+1") ||
                layout.equals("1+4+4") ||
                layout.equals("4+4");
    }

    /**
     * 取得佈局對應的選單高度
     */
    private int getMenuHeightForLayout(String layout) {
        return isFullSizeLayout(layout) ? MENU_HEIGHT_FULL : MENU_HEIGHT;
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

        // 取得佈局區域
        int[][] layoutAreas = getLayoutAreas(DEFAULT_LAYOUT);

        // 繪製格線（淡色）
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(2));
        drawGridLines(g2d, layoutAreas);

        // 繪製每個選單項目
        Font textFont = loadChineseFont(Font.BOLD, 72);

        for (int i = 0; i < layoutAreas.length && i < MENU_ITEMS.length; i++) {
            drawMenuCell(g2d, layoutAreas[i], i, textFont, themeColor);
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
            config.setRichMenuMode("CUSTOM".equals(theme) ? "CUSTOM" : "DEFAULT");
            config.setRichMenuCustomConfig(null);
            lineConfigRepository.save(config);
        });
    }

    /**
     * 繪製向量圖示（取代 emoji 以確保跨平台相容）
     *
     * @param g2d Graphics2D 繪圖物件
     * @param iconType 圖示類型
     * @param centerX 圖示中心 X 座標
     * @param centerY 圖示中心 Y 座標
     * @param size 圖示大小
     */
    private void drawIcon(Graphics2D g2d, IconType iconType, int centerX, int centerY, int size) {
        // 線條粗細隨圖示大小調整
        int strokeWidth = Math.max(6, size / 8);
        g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (iconType) {
            case CALENDAR:
                // 日曆圖示
                drawCalendarIcon(g2d, centerX, centerY, size);
                break;
            case CLIPBOARD:
                // 剪貼板圖示
                drawClipboardIcon(g2d, centerX, centerY, size);
                break;
            case CART:
                // 購物車圖示
                drawCartIcon(g2d, centerX, centerY, size);
                break;
            case GIFT:
                // 禮物圖示
                drawGiftIcon(g2d, centerX, centerY, size);
                break;
            case PERSON:
                // 人像圖示
                drawPersonIcon(g2d, centerX, centerY, size);
                break;
            case TICKET:
                // 票券圖示
                drawTicketIcon(g2d, centerX, centerY, size);
                break;
            case PHONE:
                // 電話圖示
                drawPhoneIcon(g2d, centerX, centerY, size);
                break;
        }
    }

    /**
     * 繪製日曆圖示（開始預約）
     */
    private void drawCalendarIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 1.1);
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 日曆主體（圓角矩形）
        g2d.drawRoundRect(x, y + 8, w, h - 8, 8, 8);

        // 日曆頂部夾子
        g2d.drawLine(x + w / 4, y, x + w / 4, y + 12);
        g2d.drawLine(x + w * 3 / 4, y, x + w * 3 / 4, y + 12);

        // 日曆頂部分隔線
        g2d.drawLine(x, y + 22, x + w, y + 22);

        // 日曆內日期點
        int dotSize = 6;
        g2d.fillOval(x + w / 4 - dotSize / 2, y + 32, dotSize, dotSize);
        g2d.fillOval(x + w / 2 - dotSize / 2, y + 32, dotSize, dotSize);
        g2d.fillOval(x + w * 3 / 4 - dotSize / 2, y + 32, dotSize, dotSize);
        g2d.fillOval(x + w / 4 - dotSize / 2, y + 44, dotSize, dotSize);
        g2d.fillOval(x + w / 2 - dotSize / 2, y + 44, dotSize, dotSize);
    }

    /**
     * 繪製剪貼板圖示（我的預約）
     */
    private void drawClipboardIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 1.2);
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 剪貼板主體
        g2d.drawRoundRect(x, y + 6, w, h - 6, 6, 6);

        // 頂部夾子
        g2d.drawRoundRect(x + w / 4, y, w / 2, 12, 4, 4);

        // 清單線條
        int lineY = y + 24;
        int lineGap = 12;
        for (int i = 0; i < 3; i++) {
            g2d.drawLine(x + 10, lineY + i * lineGap, x + w - 10, lineY + i * lineGap);
        }
    }

    /**
     * 繪製購物車圖示（瀏覽商品）
     */
    private void drawCartIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 購物車主體
        int[] xPoints = {x, x + 8, x + w - 8, x + w - 4};
        int[] yPoints = {y, y + h - 15, y + h - 15, y};
        g2d.drawPolyline(xPoints, yPoints, 4);

        // 購物車底部
        g2d.drawLine(x + 8, y + h - 15, x + w - 8, y + h - 15);

        // 輪子
        g2d.fillOval(x + 12, y + h - 10, 10, 10);
        g2d.fillOval(x + w - 22, y + h - 10, 10, 10);

        // 把手
        g2d.drawLine(x - 8, y, x + 5, y);
    }

    /**
     * 繪製禮物圖示（領取票券）
     */
    private void drawGiftIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 1.1);
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 禮物盒主體
        g2d.drawRect(x, y + 15, w, h - 15);

        // 禮物盒頂部蓋子
        g2d.drawRect(x - 4, y + 8, w + 8, 12);

        // 蝴蝶結（中間垂直線）
        g2d.drawLine(cx, y + 8, cx, y + h);

        // 蝴蝶結頂部
        g2d.drawOval(cx - 12, y - 2, 12, 12);
        g2d.drawOval(cx, y - 2, 12, 12);
    }

    /**
     * 繪製人像圖示（會員資訊）
     */
    private void drawPersonIcon(Graphics2D g2d, int cx, int cy, int size) {
        // 頭部
        int headSize = (int)(size * 0.5);
        g2d.drawOval(cx - headSize / 2, cy - size / 2, headSize, headSize);

        // 身體（半圓弧）
        int bodyWidth = (int)(size * 0.8);
        int bodyHeight = (int)(size * 0.5);
        g2d.drawArc(cx - bodyWidth / 2, cy + 2, bodyWidth, bodyHeight, 0, 180);
    }

    /**
     * 繪製票券圖示（領取票券）
     */
    private void drawTicketIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 0.7);
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 票券主體（圓角矩形）
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // 虛線分隔（左側 1/3）
        int lineX = x + w / 3;
        for (int dy = y + 6; dy < y + h - 4; dy += 8) {
            g2d.fillRect(lineX - 1, dy, 2, 4);
        }

        // 右側百分比符號（表示折扣）
        int symCx = x + w * 2 / 3;
        int dotR = 4;
        g2d.fillOval(symCx - 8, cy - 10, dotR * 2, dotR * 2);
        g2d.fillOval(symCx + 2, cy + 2, dotR * 2, dotR * 2);
        g2d.drawLine(symCx + 6, cy - 10, symCx - 6, cy + 10);
    }

    /**
     * 繪製電話圖示（聯絡店家）
     */
    private void drawPhoneIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = (int)(size * 0.6);
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;

        // 手機外框
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // 螢幕
        g2d.drawRect(x + 4, y + 8, w - 8, h - 20);

        // 底部圓形按鈕
        g2d.drawOval(cx - 4, y + h - 10, 8, 8);
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

    // ========================================
    // 私有方法 - 繪圖輔助
    // ========================================

    /**
     * 繪製單一選單格子（圖示 + 文字）
     *
     * @param g2d Graphics2D 繪圖物件
     * @param area 區域座標 {x, y, width, height}
     * @param index 選單項目索引
     * @param textFont 文字字型
     * @param bgColor 背景色（未使用，保留供未來擴展）
     */
    private void drawMenuCell(Graphics2D g2d, int[] area, int index, Font textFont, Color bgColor) {
        if (index >= MENU_ITEMS.length || index >= MENU_ICON_TYPES.length) return;

        int cellX = area[0];
        int cellY = area[1];
        int cellW = area[2];
        int cellH = area[3];
        int centerX = cellX + cellW / 2;
        int centerY = cellY + cellH / 2;

        // 根據格子大小調整圖示和文字尺寸
        int iconSize = Math.min(cellW, cellH) / 5;
        int circleSize = iconSize * 2;
        int fontSize = Math.max(36, Math.min(72, cellW / 12));

        // 繪製圖示背景圓圈
        g2d.setColor(ICON_BG_COLOR);
        g2d.fillOval(centerX - circleSize / 2, centerY - circleSize / 2 - iconSize / 2, circleSize, circleSize);

        // 繪製向量圖示
        g2d.setColor(TEXT_COLOR);
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX, centerY - iconSize / 4, iconSize);

        // 繪製文字
        Font cellFont = textFont.deriveFont((float) fontSize);
        g2d.setFont(cellFont);
        FontMetrics fm = g2d.getFontMetrics();
        String text = MENU_ITEMS[index][0];
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, centerX - textWidth / 2, centerY + circleSize / 2 + fm.getAscent());
    }

    /**
     * 繪製單一選單格子（含文字陰影，用於自訂背景圖片模式）
     */
    private void drawMenuCellWithShadow(Graphics2D g2d, int[] area, int index, Font textFont) {
        if (index >= MENU_ITEMS.length || index >= MENU_ICON_TYPES.length) return;

        int cellX = area[0];
        int cellY = area[1];
        int cellW = area[2];
        int cellH = area[3];
        int centerX = cellX + cellW / 2;
        int centerY = cellY + cellH / 2;

        // 根據格子大小調整圖示和文字尺寸
        int iconSize = Math.min(cellW, cellH) / 5;
        int circleSize = iconSize * 2;
        int fontSize = Math.max(36, Math.min(72, cellW / 12));

        // 繪製圖示背景圓圈（更不透明）
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillOval(centerX - circleSize / 2, centerY - circleSize / 2 - iconSize / 2, circleSize, circleSize);

        // 繪製向量圖示（先畫陰影再畫白色）
        int shadowOffset = 3;
        g2d.setColor(new Color(0, 0, 0, 120));
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX + shadowOffset, centerY - iconSize / 4 + shadowOffset, iconSize);
        g2d.setColor(TEXT_COLOR);
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX, centerY - iconSize / 4, iconSize);

        // 繪製文字（先畫陰影再畫白色）
        Font cellFont = textFont.deriveFont((float) fontSize);
        g2d.setFont(cellFont);
        FontMetrics fm = g2d.getFontMetrics();
        String text = MENU_ITEMS[index][0];
        int textWidth = fm.stringWidth(text);
        int textX = centerX - textWidth / 2;
        int textY = centerY + circleSize / 2 + fm.getAscent();

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(text, textX + 2, textY + 2);
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, textX, textY);
    }

    /**
     * 繪製膠囊式選單格子（自訂背景專用）
     *
     * <p>只在圖示+文字區域底下畫一個小型圓角膠囊半透明背景，
     * 讓大部分背景圖片都能看到，同時確保文字清晰可讀。
     *
     * @param g2d Graphics2D 繪圖物件
     * @param area 區域座標 {x, y, width, height}
     * @param index 選單項目索引
     * @param textFont 文字字型
     */
    private void drawMenuCellCapsule(Graphics2D g2d, int[] area, int index, Font textFont) {
        if (index >= MENU_ITEMS.length || index >= MENU_ICON_TYPES.length) return;

        int cellX = area[0];
        int cellY = area[1];
        int cellW = area[2];
        int cellH = area[3];
        int centerX = cellX + cellW / 2;
        int centerY = cellY + cellH / 2;

        // 根據格子大小調整尺寸
        int iconSize = Math.min(cellW, cellH) / 5;
        int circleSize = iconSize * 2;
        int fontSize = Math.max(36, Math.min(72, cellW / 12));

        // 計算文字尺寸
        Font cellFont = textFont.deriveFont((float) fontSize);
        g2d.setFont(cellFont);
        FontMetrics fm = g2d.getFontMetrics();
        String text = MENU_ITEMS[index][0];
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        // 計算膠囊範圍（包含圖示圓圈 + 文字 + padding）
        int capsulePadH = 20;
        int capsulePadV = 14;
        int capsuleW = Math.max(circleSize, textWidth) + capsulePadH * 2;
        int capsuleTop = centerY - circleSize / 2 - iconSize / 2 - capsulePadV;
        int capsuleBottom = centerY + circleSize / 2 + textHeight + capsulePadV + 4;
        int capsuleH = capsuleBottom - capsuleTop;
        int capsuleX = centerX - capsuleW / 2;

        // 繪製膠囊半透明背景
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(capsuleX, capsuleTop, capsuleW, capsuleH, 24, 24);

        // 繪製向量圖示（白色，帶陰影）
        int shadowOffset = 2;
        g2d.setColor(new Color(0, 0, 0, 80));
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX + shadowOffset, centerY - iconSize / 4 + shadowOffset, iconSize);
        g2d.setColor(TEXT_COLOR);
        drawIcon(g2d, MENU_ICON_TYPES[index], centerX, centerY - iconSize / 4, iconSize);

        // 繪製文字（白色，帶陰影）
        g2d.setFont(cellFont);
        int textX = centerX - textWidth / 2;
        int textY = centerY + circleSize / 2 + fm.getAscent();

        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawString(text, textX + 2, textY + 2);
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, textX, textY);
    }

    /**
     * 繪製格線（根據佈局區域自動計算需要的線條）
     *
     * @param g2d Graphics2D 繪圖物件
     * @param layoutAreas 佈局區域陣列
     */
    private void drawGridLines(Graphics2D g2d, int[][] layoutAreas) {
        java.util.Set<Integer> verticalLines = new java.util.TreeSet<>();
        java.util.Set<Integer> horizontalLines = new java.util.TreeSet<>();

        for (int[] area : layoutAreas) {
            int right = area[0] + area[2];
            int bottom = area[1] + area[3];
            if (right > 0 && right < MENU_WIDTH) verticalLines.add(right);
            if (bottom > 0 && bottom < MENU_HEIGHT) horizontalLines.add(bottom);
        }

        // 繪製水平線（只畫跨越整個寬度的）
        for (int y : horizontalLines) {
            g2d.drawLine(0, y, MENU_WIDTH, y);
        }

        // 繪製垂直線（根據所在行判斷高度）
        for (int[] area : layoutAreas) {
            int right = area[0] + area[2];
            if (right > 0 && right < MENU_WIDTH) {
                g2d.drawLine(right, area[1], right, area[1] + area[3]);
            }
        }
    }

    // ========================================
    // 公開方法 - 自訂配置 Rich Menu
    // ========================================

    /**
     * 使用自訂圖片和佈局配置建立 Rich Menu
     *
     * <p>圖片不疊加任何文字/圖示，直接上傳。
     * 根據 config 中定義的佈局和動作建立 Rich Menu 區域。
     *
     * @param tenantId 租戶 ID
     * @param imageBytes 圖片位元組陣列
     * @param configJson 配置 JSON 字串
     * @return Rich Menu ID
     */
    @Transactional
    public String createCustomConfigRichMenu(String tenantId, byte[] imageBytes, String configJson) {
        log.info("開始建立自訂配置 Rich Menu，租戶：{}", tenantId);

        try {
            // ========================================
            // 1. 驗證圖片
            // ========================================
            validateImage(imageBytes);

            // ========================================
            // 2. 解析配置
            // ========================================
            JsonNode config = objectMapper.readTree(configJson);
            String layout = config.path("layout").asText(DEFAULT_LAYOUT);
            JsonNode areasConfig = config.path("areas");

            if (!areasConfig.isArray() || areasConfig.isEmpty()) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "請設定至少一個選單區域");
            }

            // 驗證佈局
            int[][] layoutAreas = getLayoutAreas(layout);
            if (areasConfig.size() > layoutAreas.length) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR,
                        "區域數量（" + areasConfig.size() + "）超過佈局上限（" + layoutAreas.length + "）");
            }

            // 建立選單項目陣列
            String[][] menuItems = new String[areasConfig.size()][2];
            for (int i = 0; i < areasConfig.size(); i++) {
                JsonNode areaNode = areasConfig.get(i);
                menuItems[i][0] = areaNode.path("label").asText("選單 " + (i + 1));
                menuItems[i][1] = areaNode.path("action").asText("main_menu");
            }

            // ========================================
            // 3. 刪除現有的 Rich Menu
            // ========================================
            deleteRichMenu(tenantId);

            // ========================================
            // 4. 縮放圖片至 2500x843（不疊加文字）
            // ========================================
            byte[] resizedImageBytes = resizeImageToRichMenuSize(imageBytes);

            // ========================================
            // 5. 取得店家名稱
            // ========================================
            String shopName = tenantRepository.findById(tenantId)
                    .map(Tenant::getName)
                    .orElse("預約服務");

            // ========================================
            // 6. 建立 Rich Menu（用自訂區域）
            // ========================================
            ObjectNode requestBody = buildRichMenuRequestWithLayout(shopName, layout, menuItems);
            String accessToken = getAccessToken(tenantId);
            String url = apiEndpoint + CREATE_RICH_MENU_API;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗");
            }

            String richMenuId = response.getBody().path("richMenuId").asText();
            log.info("自訂 Rich Menu 建立成功，ID：{}", richMenuId);

            // ========================================
            // 7. 上傳圖片 + 設為預設
            // ========================================
            uploadRichMenuImage(tenantId, richMenuId, resizedImageBytes);
            setDefaultRichMenu(tenantId, richMenuId);

            // ========================================
            // 8. 儲存 mode=CUSTOM + config
            // ========================================
            lineConfigRepository.findByTenantId(tenantId).ifPresent(lineConfig -> {
                lineConfig.setRichMenuId(richMenuId);
                lineConfig.setRichMenuTheme("CUSTOM");
                lineConfig.setRichMenuMode("CUSTOM");
                lineConfig.setRichMenuCustomConfig(configJson);
                lineConfigRepository.save(lineConfig);
            });

            return richMenuId;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("建立自訂配置 Rich Menu 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗：" + e.getMessage());
        }
    }

    // ========================================
    // 進階自訂 Rich Menu（付費功能：CUSTOM_RICH_MENU）
    // ========================================

    /**
     * 將圖片縮放到指定尺寸（支援 Full/Half 兩種高度）
     *
     * @param imageBytes 原始圖片
     * @param targetWidth 目標寬度
     * @param targetHeight 目標高度
     * @return 縮放後的圖片位元組陣列
     */
    private byte[] resizeImageToSize(byte[] imageBytes, int targetWidth, int targetHeight) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "無法讀取圖片");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            if (originalWidth == targetWidth && originalHeight == targetHeight) {
                return imageBytes;
            }

            log.info("縮放圖片：{}x{} → {}x{}", originalWidth, originalHeight, targetWidth, targetHeight);

            // Cover 策略
            double scale = Math.max((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);
            int scaledWidth = (int) Math.round(originalWidth * scale);
            int scaledHeight = (int) Math.round(originalHeight * scale);

            BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();

            // 置中裁切
            int cropX = (scaledWidth - targetWidth) / 2;
            int cropY = (scaledHeight - targetHeight) / 2;
            BufferedImage cropped = scaledImage.getSubimage(cropX, cropY, targetWidth, targetHeight);

            BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D outG = output.createGraphics();
            outG.drawImage(cropped, 0, 0, null);
            outG.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(output, "png", baos);
            byte[] result = baos.toByteArray();

            // 超過 1MB 轉 JPEG
            if (result.length > MAX_IMAGE_SIZE) {
                log.warn("圖片超過 1MB（{}KB），轉為 JPEG", result.length / 1024);
                baos = new ByteArrayOutputStream();
                ImageIO.write(output, "jpg", baos);
                result = baos.toByteArray();
                if (result.length > MAX_IMAGE_SIZE) {
                    throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片處理後仍超過 1MB");
                }
            }

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片處理失敗：" + e.getMessage());
        }
    }

    /**
     * 合成進階自訂 Rich Menu 圖片
     *
     * <p>合成步驟：
     * <ol>
     *   <li>載入/縮放背景到目標尺寸</li>
     *   <li>對每格有自訂圖示的 cell：圓形裁切後繪製到格子中心偏上</li>
     *   <li>對每格繪製文字標籤（可設顏色、帶陰影描邊）</li>
     *   <li>匯出 PNG（超過 1MB 轉 JPEG）</li>
     * </ol>
     *
     * @param backgroundBytes 背景圖片（可為 null，則使用純色）
     * @param cellIcons 每格圖示 Map（key=格子索引, value=圖示圖片 bytes）
     * @param configJson 配置 JSON
     * @return 合成後的圖片位元組陣列
     */
    public byte[] composeAdvancedRichMenuImage(
            byte[] backgroundBytes,
            java.util.Map<Integer, byte[]> cellIcons,
            String configJson
    ) {
        if (configJson == null || configJson.isBlank()) {
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "進階選單配置不能為空");
        }
        try {
            JsonNode config = objectMapper.readTree(configJson);
            String layout = config.path("layout").asText(DEFAULT_LAYOUT);
            String bgColor = config.path("backgroundColor").asText("#F5F0E8");
            JsonNode cellsConfig = config.path("cells");

            int menuHeight = getMenuHeightForLayout(layout);
            int[][] layoutAreas = getLayoutAreasWithHeight(layout, menuHeight);

            // ========================================
            // 1. 建立畫布 + 背景
            // ========================================
            BufferedImage canvas = new BufferedImage(MENU_WIDTH, menuHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            if (backgroundBytes != null && backgroundBytes.length > 0) {
                // 使用背景圖片
                byte[] resizedBg = resizeImageToSize(backgroundBytes, MENU_WIDTH, menuHeight);
                BufferedImage bgImage = ImageIO.read(new java.io.ByteArrayInputStream(resizedBg));
                if (bgImage != null) {
                    g2d.drawImage(bgImage, 0, 0, MENU_WIDTH, menuHeight, null);
                }
            } else {
                // 使用純色背景
                try {
                    g2d.setColor(Color.decode(bgColor));
                } catch (NumberFormatException e) {
                    g2d.setColor(new Color(0xF5, 0xF0, 0xE8));
                }
                g2d.fillRect(0, 0, MENU_WIDTH, menuHeight);
            }

            // ========================================
            // 2. 對每格繪製圖示和文字
            // ========================================
            Font textFont = loadChineseFont(Font.BOLD, 48);

            for (int i = 0; i < layoutAreas.length; i++) {
                int[] area = layoutAreas[i];
                int cellX = area[0];
                int cellY = area[1];
                int cellW = area[2];
                int cellH = area[3];
                int centerX = cellX + cellW / 2;
                int centerY = cellY + cellH / 2;

                // 取得 cell 配置
                JsonNode cellConfig = null;
                if (cellsConfig != null && cellsConfig.isArray()) {
                    for (JsonNode c : cellsConfig) {
                        if (c.path("index").asInt(-1) == i) {
                            cellConfig = c;
                            break;
                        }
                    }
                }

                // 繪製圓形圖示
                if (cellIcons != null && cellIcons.containsKey(i)) {
                    byte[] iconBytes = cellIcons.get(i);
                    if (iconBytes != null && iconBytes.length > 0) {
                        try {
                            BufferedImage iconImg = ImageIO.read(new java.io.ByteArrayInputStream(iconBytes));
                            if (iconImg != null) {
                                int iconSize = Math.min(cellW, cellH) / 3;
                                String iconShape = cellConfig != null ? cellConfig.path("iconShape").asText("circle") : "circle";

                                // 縮放圖示
                                BufferedImage scaledIcon = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
                                Graphics2D iconG = scaledIcon.createGraphics();
                                try {
                                    iconG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                    iconG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                                    // 圓形裁切
                                    if ("circle".equals(iconShape)) {
                                        iconG.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, iconSize, iconSize));
                                    }
                                    iconG.drawImage(iconImg, 0, 0, iconSize, iconSize, null);
                                } finally {
                                    iconG.dispose();
                                }

                                // 繪製到畫布（格子中心偏上）
                                int iconX = centerX - iconSize / 2;
                                int iconY = centerY - iconSize / 2 - cellH / 8;
                                g2d.drawImage(scaledIcon, iconX, iconY, null);
                            }
                        } catch (IOException e) {
                            log.warn("載入格子 {} 圖示失敗：{}", i, e.getMessage());
                        }
                    }
                }

                // 繪製文字標籤
                String label = cellConfig != null ? cellConfig.path("label").asText("") : "";
                if (!label.isEmpty()) {
                    String labelColorHex = cellConfig != null ? cellConfig.path("labelColor").asText("#FFFFFF") : "#FFFFFF";
                    int labelSize = cellConfig != null ? cellConfig.path("labelSize").asInt(0) : 0;
                    if (labelSize <= 0) {
                        labelSize = Math.max(28, Math.min(48, cellW / 14));
                    }

                    Color labelColor;
                    try {
                        labelColor = Color.decode(labelColorHex);
                    } catch (NumberFormatException e) {
                        labelColor = Color.WHITE;
                    }

                    Font cellFont = textFont.deriveFont((float) labelSize);
                    g2d.setFont(cellFont);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(label);
                    int textX = centerX - textWidth / 2;

                    // 文字位置：圖示下方或格子中心
                    boolean hasIcon = cellIcons != null && cellIcons.containsKey(i);
                    int textY;
                    if (hasIcon) {
                        int iconSize = Math.min(cellW, cellH) / 3;
                        textY = centerY + iconSize / 2 + fm.getAscent() / 2;
                    } else {
                        textY = centerY + fm.getAscent() / 4;
                    }

                    // 描邊文字（提高可讀性）
                    Color outlineColor = getContrastOutlineColor(labelColor);
                    FontRenderContext frc = g2d.getFontRenderContext();
                    TextLayout textLayout = new TextLayout(label, cellFont, frc);
                    AffineTransform transform = AffineTransform.getTranslateInstance(textX, textY);
                    Shape textShape = textLayout.getOutline(transform);

                    g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.setColor(outlineColor);
                    g2d.draw(textShape);
                    g2d.setColor(labelColor);
                    g2d.fill(textShape);
                }
            }

            g2d.dispose();

            // ========================================
            // 3. 匯出
            // ========================================
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            byte[] result = baos.toByteArray();

            if (result.length > MAX_IMAGE_SIZE) {
                log.warn("合成圖片超過 1MB（{}KB），轉為 JPEG", result.length / 1024);
                // 轉 RGB（JPEG 不支援 ARGB）
                BufferedImage rgbImage = new BufferedImage(MENU_WIDTH, menuHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D rgbG = rgbImage.createGraphics();
                rgbG.drawImage(canvas, 0, 0, null);
                rgbG.dispose();
                baos = new ByteArrayOutputStream();
                ImageIO.write(rgbImage, "jpg", baos);
                result = baos.toByteArray();
            }

            log.info("進階 Rich Menu 圖片合成完成，大小：{}KB", result.length / 1024);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("進階 Rich Menu 圖片合成失敗：{}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYS_VALIDATION_ERROR, "圖片合成失敗：" + e.getMessage());
        }
    }

    /**
     * 建立進階自訂 Rich Menu 並發布到 LINE
     *
     * @param tenantId 租戶 ID
     * @param backgroundBytes 背景圖片（可為 null）
     * @param cellIcons 每格圖示
     * @param configJson 配置 JSON
     * @return Rich Menu ID
     */
    @Transactional
    public String createAdvancedRichMenu(
            String tenantId,
            byte[] backgroundBytes,
            java.util.Map<Integer, byte[]> cellIcons,
            String configJson
    ) {
        log.info("開始建立進階自訂 Rich Menu，租戶：{}", tenantId);

        try {
            // ========================================
            // 1. 合成圖片
            // ========================================
            byte[] compositeImage = composeAdvancedRichMenuImage(backgroundBytes, cellIcons, configJson);

            // ========================================
            // 2. 解析配置取得佈局和選單項目
            // ========================================
            JsonNode config = objectMapper.readTree(configJson);
            String layout = config.path("layout").asText(DEFAULT_LAYOUT);
            JsonNode cellsConfig = config.path("cells");

            int menuHeight = getMenuHeightForLayout(layout);
            int[][] layoutAreas = getLayoutAreasWithHeight(layout, menuHeight);

            // 從 cells 配置建立選單項目
            int cellCount = Math.min(
                    cellsConfig != null && cellsConfig.isArray() ? cellsConfig.size() : layoutAreas.length,
                    layoutAreas.length
            );

            String[][] menuItems = new String[cellCount][2];
            for (int i = 0; i < cellCount; i++) {
                JsonNode cellConfig = null;
                if (cellsConfig != null && cellsConfig.isArray()) {
                    for (JsonNode c : cellsConfig) {
                        if (c.path("index").asInt(-1) == i) {
                            cellConfig = c;
                            break;
                        }
                    }
                }

                String label = cellConfig != null ? cellConfig.path("label").asText("選單 " + (i + 1)) : "選單 " + (i + 1);
                String action = "main_menu";

                if (cellConfig != null && cellConfig.has("action")) {
                    JsonNode actionNode = cellConfig.path("action");
                    if (actionNode.isTextual()) {
                        action = actionNode.asText("main_menu");
                    } else if (actionNode.isObject()) {
                        String actionType = actionNode.path("type").asText("");
                        if ("flex_popup".equals(actionType)) {
                            action = "flex_popup&cellKey=" + i;
                        } else {
                            action = actionNode.path("data").asText("main_menu");
                        }
                    }
                }

                menuItems[i][0] = label;
                menuItems[i][1] = action;
            }

            // ========================================
            // 3. 刪除現有 Rich Menu
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
            ObjectNode requestBody = buildRichMenuRequestWithLayout(shopName, layout, menuItems);
            String accessToken = getAccessToken(tenantId);
            String url = apiEndpoint + CREATE_RICH_MENU_API;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立進階 Rich Menu 失敗");
            }

            String richMenuId = response.getBody().path("richMenuId").asText();
            log.info("進階 Rich Menu 建立成功，ID：{}", richMenuId);

            // ========================================
            // 6. 上傳圖片 + 設為預設
            // ========================================
            uploadRichMenuImage(tenantId, richMenuId, compositeImage);
            setDefaultRichMenu(tenantId, richMenuId);

            // ========================================
            // 7. 儲存 mode=ADVANCED + config
            // ========================================
            lineConfigRepository.findByTenantId(tenantId).ifPresent(lineConfig -> {
                lineConfig.setRichMenuId(richMenuId);
                lineConfig.setRichMenuTheme("ADVANCED");
                lineConfig.setRichMenuMode("ADVANCED");
                lineConfig.setRichMenuCustomConfig(configJson);
                lineConfigRepository.save(lineConfig);
            });

            return richMenuId;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("建立進階 Rich Menu 失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立進階 Rich Menu 失敗：" + e.getMessage());
        }
    }

    /**
     * 取得進階 Rich Menu 配置
     *
     * @param tenantId 租戶 ID
     * @return 配置 JSON 字串（可能為 null）
     */
    public String getAdvancedConfig(String tenantId) {
        return lineConfigRepository.findByTenantId(tenantId)
                .map(TenantLineConfig::getRichMenuCustomConfig)
                .orElse(null);
    }

    /**
     * 儲存進階 Rich Menu 配置草稿（不發布到 LINE）
     *
     * @param tenantId 租戶 ID
     * @param configJson 配置 JSON
     */
    @Transactional
    public void saveAdvancedConfig(String tenantId, String configJson) {
        lineConfigRepository.findByTenantId(tenantId).ifPresent(config -> {
            config.setRichMenuCustomConfig(configJson);
            lineConfigRepository.save(config);
        });
        log.info("儲存進階 Rich Menu 配置草稿，租戶：{}", tenantId);
    }
}
