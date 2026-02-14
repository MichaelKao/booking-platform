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
        PHONE,       // 聯絡店家 - 電話
        COIN,        // 紅利點數 - 錢幣
        HOURGLASS,   // 儲值餘額 - 沙漏
        HEART,       // 線上預約 - 愛心
        FLOWER,      // 1對1諮詢 - 花朵
        RECEIPT,     // 消費紀錄 - 收據
        COUPON_STAR, // 電子優惠券 - 星章
        SCISSORS,    // 美容預約 - 剪刀
        HOUSE,       // 住宿預約 - 房屋
        ENVELOPE     // 會員優惠 - 信封
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
            String richMenuId;
            if ("BOUTIQUE".equalsIgnoreCase(theme)) {
                // 精品風使用全尺寸 3+4+4 佈局（11 格）
                // 上排：服務價目（flex彈窗）、店名Logo（主選單）、最新優惠（flex彈窗）
                // 中排：開始預約、查詢預約、我的票券、會員資訊
                // 下排：聯絡店家、瀏覽商品、領取票券、電子優惠券
                String[][] boutiqueItems = {
                        {"服務價目", "flex_popup&cellKey=0"},
                        {shopName, "show_menu"},
                        {"最新優惠", "flex_popup&cellKey=2"},
                        {"開始預約", "start_booking"},
                        {"查詢預約", "view_bookings"},
                        {"我的票券", "view_my_coupons"},
                        {"會員資訊", "view_member_info"},
                        {"聯絡店家", "contact_shop"},
                        {"瀏覽商品", "start_shopping"},
                        {"領取票券", "view_coupons"},
                        {"電子優惠券", "view_coupons"}
                };
                richMenuId = createRichMenuWithLayout(tenantId, shopName, "3+4+4", boutiqueItems);

                // 儲存 flex popup 配置到 richMenuCustomConfig
                saveBoutiqueFlexPopupConfig(tenantId, richMenuId, shopName);
            } else {
                richMenuId = createRichMenu(tenantId, shopName);
            }
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
     * 使用指定佈局建立 Rich Menu
     */
    private String createRichMenuWithLayout(String tenantId, String shopName, String layout, String[][] menuItems) {
        String accessToken = getAccessToken(tenantId);
        ObjectNode requestBody = buildRichMenuRequestWithLayout(shopName, layout, menuItems);
        String url = apiEndpoint + CREATE_RICH_MENU_API;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.LINE_API_ERROR, "建立 Rich Menu 失敗");
            }
            String richMenuId = response.getBody().path("richMenuId").asText();
            log.info("Rich Menu（{}佈局）建立成功，ID：{}", layout, richMenuId);
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
        // 精品風主題使用特殊渲染
        if ("BOUTIQUE".equalsIgnoreCase(theme)) {
            return generateBoutiqueRichMenuImage(shopName);
        }

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

    /**
     * 產生精品風 Rich Menu 圖片（全尺寸 3+4+4 佈局，2500x1686）
     *
     * <p>設計風格參考「和煦蒔光」＋「萌寵狗Ray咪」：
     * <ul>
     *   <li>暖米色漸層背景 + 細圓點底紋</li>
     *   <li>上排 3 格：左右藥丸按鈕 + 中間店名 Logo 區</li>
     *   <li>中排 4 格 + 下排 4 格：圓形圖示 + 虛線外環 + 文字標籤</li>
     *   <li>金色邊框 + 角落裝飾</li>
     * </ul>
     */
    private byte[] generateBoutiqueRichMenuImage(String shopName) throws IOException {
        int imgW = MENU_WIDTH;          // 2500
        int imgH = MENU_HEIGHT_FULL;    // 1686
        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 高品質渲染
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // ── 色彩定義 ──
        Color bgLight = new Color(0xF7, 0xEC, 0xDF);      // 暖米色
        Color bgDark = new Color(0xF0, 0xD8, 0xC8);       // 淡粉色
        Color goldAccent = new Color(0xC8, 0x9B, 0x6E);    // 金色裝飾
        Color goldLight = new Color(0xD4, 0xAF, 0x7F);     // 淺金色
        Color circleBgLight = new Color(0xFB, 0xF5, 0xED); // 圓圈淺色
        Color circleBgDark = new Color(0xF5, 0xEB, 0xDE);  // 圓圈深色
        Color iconBrown = new Color(0x7A, 0x5C, 0x3E);     // 圖示棕色
        Color textBrown = new Color(0x5C, 0x40, 0x33);     // 文字深棕
        Color pillBg = new Color(0x8B, 0x73, 0x55);        // 藥丸按鈕底色（暗金）
        Color pillText = Color.WHITE;                       // 藥丸按鈕文字
        Color shadowColor = new Color(0, 0, 0, 20);

        // ── 1. 漸層背景 ──
        GradientPaint bgGradient = new GradientPaint(0, 0, bgLight, imgW, imgH, bgDark);
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, imgW, imgH);

        // ── 2. 裝飾性圓點底紋 ──
        g2d.setColor(new Color(0xD4, 0xAF, 0x7F, 25));
        for (int px = 0; px < imgW; px += 40) {
            for (int py = 0; py < imgH; py += 40) {
                g2d.fillOval(px, py, 3, 3);
            }
        }

        // ── 3. 雙層外框 ──
        g2d.setColor(goldAccent);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRoundRect(15, 15, imgW - 30, imgH - 30, 20, 20);
        g2d.setColor(goldLight);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(28, 28, imgW - 56, imgH - 56, 15, 15);

        // ── 4. 角落裝飾（L 形 + 圓點）──
        drawBoutiqueCorners(g2d, goldAccent, imgH);

        // ── 行列定義（3+4+4 佈局）──
        int rowH = imgH / 3;                // 每行高度 562
        int topRow3W = imgW / 3;            // 上排每格 833
        int midBottomW = imgW / 4;          // 中下排每格 625

        // ── 5. 淡色分隔線 ──
        g2d.setColor(new Color(0xD4, 0xAF, 0x7F, 40));
        g2d.setStroke(new BasicStroke(1.5f));
        // 水平分隔線
        g2d.drawLine(50, rowH, imgW - 50, rowH);
        g2d.drawLine(50, rowH * 2, imgW - 50, rowH * 2);
        // 上排垂直分隔線
        for (int c = 1; c < 3; c++) {
            g2d.drawLine(c * topRow3W, 50, c * topRow3W, rowH - 20);
        }
        // 中排、下排垂直分隔線
        for (int c = 1; c < 4; c++) {
            g2d.drawLine(c * midBottomW, rowH + 20, c * midBottomW, rowH * 2 - 20);
            g2d.drawLine(c * midBottomW, rowH * 2 + 20, c * midBottomW, imgH - 50);
        }

        // ================================================================
        // 上排 3 格：[藥丸按鈕：服務價目] [店名 Logo] [藥丸按鈕：最新優惠]
        // ================================================================
        Font pillFont = loadChineseFont(Font.BOLD, 48);
        Font shopNameFont = loadChineseFont(Font.BOLD, 72);
        Font shopSubFont = loadChineseFont(Font.PLAIN, 36);

        // --- 左邊藥丸按鈕「服務價目」---
        {
            int cx = topRow3W / 2;
            int cy = rowH / 2;
            int pillW = 360;
            int pillH = 80;
            int px = cx - pillW / 2;
            int py = cy - pillH / 2;

            // 藥丸陰影
            g2d.setColor(new Color(0, 0, 0, 25));
            g2d.fillRoundRect(px + 3, py + 3, pillW, pillH, pillH, pillH);
            // 藥丸底色
            g2d.setColor(pillBg);
            g2d.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
            // 藥丸邊框
            g2d.setColor(goldAccent);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(px, py, pillW, pillH, pillH, pillH);
            // 文字
            g2d.setFont(pillFont);
            g2d.setColor(pillText);
            FontMetrics pfm = g2d.getFontMetrics();
            String pillLabel = "\u27A1 服務價目";
            int tw = pfm.stringWidth(pillLabel);
            g2d.drawString(pillLabel, cx - tw / 2, cy + pfm.getAscent() / 2 - 2);
        }

        // --- 中間店名 Logo ---
        {
            int cx = topRow3W + topRow3W / 2;
            int cy = rowH / 2;

            // 裝飾性大圓環（Logo 背景）
            int logoR = 160;
            g2d.setColor(new Color(0xC8, 0x9B, 0x6E, 40));
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(cx - logoR, cy - logoR, logoR * 2, logoR * 2);

            // 內圈
            g2d.setColor(new Color(0xC8, 0x9B, 0x6E, 25));
            g2d.fillOval(cx - logoR + 15, cy - logoR + 15, (logoR - 15) * 2, (logoR - 15) * 2);

            // 店名（大字）
            g2d.setFont(shopNameFont);
            g2d.setColor(textBrown);
            FontMetrics sfm = g2d.getFontMetrics();
            // 截取前 4 個字，避免過長
            String displayName = shopName.length() > 5 ? shopName.substring(0, 5) : shopName;
            int snw = sfm.stringWidth(displayName);
            g2d.drawString(displayName, cx - snw / 2, cy + sfm.getAscent() / 3);

            // 副標題
            g2d.setFont(shopSubFont);
            g2d.setColor(goldAccent);
            FontMetrics subfm = g2d.getFontMetrics();
            String sub = "BOOKING";
            int subW = subfm.stringWidth(sub);
            g2d.drawString(sub, cx - subW / 2, cy + sfm.getAscent() / 3 + 40);
        }

        // --- 右邊藥丸按鈕「最新優惠」---
        {
            int cx = topRow3W * 2 + topRow3W / 2;
            int cy = rowH / 2;
            int pillW = 360;
            int pillH = 80;
            int px = cx - pillW / 2;
            int py = cy - pillH / 2;

            g2d.setColor(new Color(0, 0, 0, 25));
            g2d.fillRoundRect(px + 3, py + 3, pillW, pillH, pillH, pillH);
            g2d.setColor(pillBg);
            g2d.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
            g2d.setColor(goldAccent);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(px, py, pillW, pillH, pillH, pillH);
            g2d.setFont(pillFont);
            g2d.setColor(pillText);
            FontMetrics pfm = g2d.getFontMetrics();
            String pillLabel = "\u27A1 最新優惠";
            int tw = pfm.stringWidth(pillLabel);
            g2d.drawString(pillLabel, cx - tw / 2, cy + pfm.getAscent() / 2 - 2);
        }

        // ================================================================
        // 中排 4 格 + 下排 4 格：圓形圖示 + 文字標籤
        // ================================================================
        String[] midLabels = {"開始預約", "查詢預約", "我的票券", "會員資訊"};
        IconType[] midIcons = {IconType.HEART, IconType.CLIPBOARD, IconType.TICKET, IconType.COIN};

        String[] botLabels = {"聯絡店家", "瀏覽商品", "領取票券", "電子優惠券"};
        IconType[] botIcons = {IconType.FLOWER, IconType.RECEIPT, IconType.COUPON_STAR, IconType.COUPON_STAR};

        Font labelFont = loadChineseFont(Font.BOLD, 48);

        // 中排（row = 1）
        for (int i = 0; i < 4; i++) {
            int cellX = i * midBottomW;
            int cellY = rowH;
            drawBoutiqueCell(g2d, cellX, cellY, midBottomW, rowH,
                    midLabels[i], midIcons[i], labelFont,
                    goldAccent, circleBgLight, circleBgDark, iconBrown, textBrown, shadowColor);
        }

        // 下排（row = 2）
        for (int i = 0; i < 4; i++) {
            int cellX = i * midBottomW;
            int cellY = rowH * 2;
            int cellH = imgH - rowH * 2;
            drawBoutiqueCell(g2d, cellX, cellY, midBottomW, cellH,
                    botLabels[i], botIcons[i], labelFont,
                    goldAccent, circleBgLight, circleBgDark, iconBrown, textBrown, shadowColor);
        }

        // ── 上排與中排之間的裝飾點 ──
        g2d.setColor(goldAccent);
        int dotY = rowH;
        for (int dx = 80; dx < imgW - 60; dx += 50) {
            g2d.fillOval(dx, dotY - 2, 4, 4);
        }
        int dotY2 = rowH * 2;
        for (int dx = 80; dx < imgW - 60; dx += 50) {
            g2d.fillOval(dx, dotY2 - 2, 4, 4);
        }

        g2d.dispose();

        // 轉換為 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] result = baos.toByteArray();

        // 超過 1MB 轉 JPEG
        if (result.length > MAX_IMAGE_SIZE) {
            baos.reset();
            // 轉為 RGB（JPEG 不支援 ARGB）
            BufferedImage rgbImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
            Graphics2D rg = rgbImage.createGraphics();
            rg.setColor(bgLight);
            rg.fillRect(0, 0, imgW, imgH);
            rg.drawImage(image, 0, 0, null);
            rg.dispose();
            ImageIO.write(rgbImage, "jpg", baos);
            result = baos.toByteArray();
        }

        return result;
    }

    /**
     * 繪製精品風圓形圖示格（中排/下排通用）
     */
    private void drawBoutiqueCell(Graphics2D g2d, int cellX, int cellY, int cellW, int cellH,
                                   String label, IconType iconType, Font labelFont,
                                   Color goldAccent, Color circleBgLight, Color circleBgDark,
                                   Color iconBrown, Color textBrown, Color shadowColor) {
        int centerX = cellX + cellW / 2;
        int centerY = cellY + cellH / 2 - 25;
        int radius = Math.min(cellW, cellH) / 2 - 65;
        radius = Math.min(radius, 120);

        // 裝飾外環（虛線）
        g2d.setColor(new Color(0xC8, 0x9B, 0x6E, 90));
        float[] dash = {10f, 6f};
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0));
        g2d.drawOval(centerX - radius - 18, centerY - radius - 18,
                (radius + 18) * 2, (radius + 18) * 2);

        // 圓圈陰影
        g2d.setColor(new Color(0, 0, 0, 12));
        g2d.fillOval(centerX - radius + 4, centerY - radius + 4, radius * 2, radius * 2);

        // 圓圈填色（漸層）
        GradientPaint circleGrad = new GradientPaint(
                centerX - radius, centerY - radius, circleBgLight,
                centerX + radius, centerY + radius, circleBgDark
        );
        g2d.setPaint(circleGrad);
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // 圓圈邊框
        g2d.setColor(goldAccent);
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // 內圈裝飾線
        g2d.setColor(new Color(0xC8, 0x9B, 0x6E, 50));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawOval(centerX - radius + 8, centerY - radius + 8,
                (radius - 8) * 2, (radius - 8) * 2);

        // 繪製圖示
        int iconSize = Math.min(70, radius * 2 / 3);
        g2d.setColor(iconBrown);
        g2d.setStroke(new BasicStroke(Math.max(5, iconSize / 10),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawIcon(g2d, iconType, centerX, centerY, iconSize);

        // 文字標籤
        g2d.setFont(labelFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = centerX - textWidth / 2;
        int textY = centerY + radius + 50;

        // 文字陰影
        g2d.setColor(shadowColor);
        g2d.drawString(label, textX + 1, textY + 1);

        // 文字主體
        g2d.setColor(textBrown);
        g2d.drawString(label, textX, textY);
    }

    /**
     * 繪製精品風角落裝飾（支援不同高度）
     */
    private void drawBoutiqueCorners(Graphics2D g2d, Color color, int height) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int inset = 40;
        int len = 35;

        // 左上角 L 形裝飾 + 圓點
        g2d.drawLine(inset, inset + len, inset, inset);
        g2d.drawLine(inset, inset, inset + len, inset);
        g2d.fillOval(inset - 4, inset - 4, 8, 8);

        // 右上角
        g2d.drawLine(MENU_WIDTH - inset - len, inset, MENU_WIDTH - inset, inset);
        g2d.drawLine(MENU_WIDTH - inset, inset, MENU_WIDTH - inset, inset + len);
        g2d.fillOval(MENU_WIDTH - inset - 4, inset - 4, 8, 8);

        // 左下角
        g2d.drawLine(inset, height - inset - len, inset, height - inset);
        g2d.drawLine(inset, height - inset, inset + len, height - inset);
        g2d.fillOval(inset - 4, height - inset - 4, 8, 8);

        // 右下角
        g2d.drawLine(MENU_WIDTH - inset - len, height - inset, MENU_WIDTH - inset, height - inset);
        g2d.drawLine(MENU_WIDTH - inset, height - inset - len, MENU_WIDTH - inset, height - inset);
        g2d.fillOval(MENU_WIDTH - inset - 4, height - inset - 4, 8, 8);
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
            // BOUTIQUE 主題不清除自訂配置（含 flex popup）
            if (!"BOUTIQUE".equalsIgnoreCase(theme)) {
                config.setRichMenuCustomConfig(null);
            }
            lineConfigRepository.save(config);
        });
    }

    /**
     * 儲存精品風 Flex 彈窗配置
     *
     * <p>為上排的「服務價目」（cell 0）和「最新優惠」（cell 2）設定 Flex Message 彈窗
     */
    private void saveBoutiqueFlexPopupConfig(String tenantId, String richMenuId, String shopName) {
        try {
            ObjectNode config = objectMapper.createObjectNode();
            config.put("mode", "BOUTIQUE");
            config.put("size", "FULL");
            config.put("layout", "3+4+4");

            ArrayNode cells = objectMapper.createArrayNode();

            // ── cell 0：服務價目（Carousel 彈窗）──
            ObjectNode cell0 = objectMapper.createObjectNode();
            cell0.put("index", 0);
            cell0.put("label", "服務價目");
            ObjectNode flexPopup0 = objectMapper.createObjectNode();
            flexPopup0.put("type", "carousel");
            ArrayNode bubbles0 = objectMapper.createArrayNode();

            ObjectNode bubble0_1 = objectMapper.createObjectNode();
            bubble0_1.put("heroImageUrl", "https://via.placeholder.com/800x600/F5F0E8/8B7355?text=" + shopName);
            bubble0_1.put("title", "服務價目表");
            bubble0_1.put("description", "查看我們提供的所有服務項目與價格");
            ArrayNode buttons0_1 = objectMapper.createArrayNode();
            ObjectNode btn0_1 = objectMapper.createObjectNode();
            btn0_1.put("label", "開始預約");
            ObjectNode btnAction0_1 = objectMapper.createObjectNode();
            btnAction0_1.put("type", "postback");
            btnAction0_1.put("data", "action=start_booking");
            btn0_1.set("action", btnAction0_1);
            buttons0_1.add(btn0_1);
            bubble0_1.set("buttons", buttons0_1);
            bubbles0.add(bubble0_1);

            ObjectNode bubble0_2 = objectMapper.createObjectNode();
            bubble0_2.put("heroImageUrl", "https://via.placeholder.com/800x600/E8D5C4/8B7355?text=Services");
            bubble0_2.put("title", "人氣推薦");
            bubble0_2.put("description", "最受歡迎的服務項目");
            ArrayNode buttons0_2 = objectMapper.createArrayNode();
            ObjectNode btn0_2 = objectMapper.createObjectNode();
            btn0_2.put("label", "查看詳情");
            ObjectNode btnAction0_2 = objectMapper.createObjectNode();
            btnAction0_2.put("type", "postback");
            btnAction0_2.put("data", "action=start_booking");
            btn0_2.set("action", btnAction0_2);
            buttons0_2.add(btn0_2);
            bubble0_2.set("buttons", buttons0_2);
            bubbles0.add(bubble0_2);

            flexPopup0.set("bubbles", bubbles0);
            cell0.set("flexPopup", flexPopup0);
            cells.add(cell0);

            // ── cell 2：最新優惠（Carousel 彈窗）──
            ObjectNode cell2 = objectMapper.createObjectNode();
            cell2.put("index", 2);
            cell2.put("label", "最新優惠");
            ObjectNode flexPopup2 = objectMapper.createObjectNode();
            flexPopup2.put("type", "carousel");
            ArrayNode bubbles2 = objectMapper.createArrayNode();

            ObjectNode bubble2_1 = objectMapper.createObjectNode();
            bubble2_1.put("heroImageUrl", "https://via.placeholder.com/800x600/FFE4C4/8B7355?text=Coupon");
            bubble2_1.put("title", "最新優惠活動");
            bubble2_1.put("description", "查看目前進行中的優惠活動");
            ArrayNode buttons2_1 = objectMapper.createArrayNode();
            ObjectNode btn2_1 = objectMapper.createObjectNode();
            btn2_1.put("label", "領取票券");
            ObjectNode btnAction2_1 = objectMapper.createObjectNode();
            btnAction2_1.put("type", "postback");
            btnAction2_1.put("data", "action=view_coupons");
            btn2_1.set("action", btnAction2_1);
            buttons2_1.add(btn2_1);
            bubble2_1.set("buttons", buttons2_1);
            bubbles2.add(bubble2_1);

            ObjectNode bubble2_2 = objectMapper.createObjectNode();
            bubble2_2.put("heroImageUrl", "https://via.placeholder.com/800x600/D4C4B0/8B7355?text=Member");
            bubble2_2.put("title", "會員專屬優惠");
            bubble2_2.put("description", "加入會員享更多折扣");
            ArrayNode buttons2_2 = objectMapper.createArrayNode();
            ObjectNode btn2_2 = objectMapper.createObjectNode();
            btn2_2.put("label", "會員資訊");
            ObjectNode btnAction2_2 = objectMapper.createObjectNode();
            btnAction2_2.put("type", "postback");
            btnAction2_2.put("data", "action=view_member_info");
            btn2_2.set("action", btnAction2_2);
            buttons2_2.add(btn2_2);
            bubble2_2.set("buttons", buttons2_2);
            bubbles2.add(bubble2_2);

            flexPopup2.set("bubbles", bubbles2);
            cell2.set("flexPopup", flexPopup2);
            cells.add(cell2);

            config.set("cells", cells);

            // 儲存到 DB
            lineConfigRepository.findByTenantId(tenantId).ifPresent(lineConfig -> {
                lineConfig.setRichMenuCustomConfig(config.toString());
                lineConfigRepository.save(lineConfig);
            });

            log.info("精品風 Flex 彈窗配置已儲存，租戶：{}", tenantId);

        } catch (Exception e) {
            log.warn("儲存精品風 Flex 彈窗配置失敗，租戶：{}，錯誤：{}", tenantId, e.getMessage());
        }
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
            case COIN:
                // 錢幣圖示（紅利點數）
                drawCoinIcon(g2d, centerX, centerY, size);
                break;
            case HOURGLASS:
                // 沙漏圖示（儲值餘額）
                drawHourglassIcon(g2d, centerX, centerY, size);
                break;
            case HEART:
                // 愛心圖示（線上預約）
                drawHeartIcon(g2d, centerX, centerY, size);
                break;
            case FLOWER:
                // 花朵圖示（1對1諮詢）
                drawFlowerIcon(g2d, centerX, centerY, size);
                break;
            case RECEIPT:
                // 收據圖示（消費紀錄）
                drawReceiptIcon(g2d, centerX, centerY, size);
                break;
            case COUPON_STAR:
                // 星章圖示（電子優惠券）
                drawCouponStarIcon(g2d, centerX, centerY, size);
                break;
            case SCISSORS:
                // 剪刀圖示（美容預約）
                drawScissorsIcon(g2d, centerX, centerY, size);
                break;
            case HOUSE:
                // 房屋圖示（住宿預約）
                drawHouseIcon(g2d, centerX, centerY, size);
                break;
            case ENVELOPE:
                // 信封圖示（會員優惠）
                drawEnvelopeIcon(g2d, centerX, centerY, size);
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
     * 繪製錢幣圖示（紅利點數）
     */
    private void drawCoinIcon(Graphics2D g2d, int cx, int cy, int size) {
        int r = size / 2;
        // 外圈
        g2d.drawOval(cx - r, cy - r, r * 2, r * 2);
        // 內圈
        g2d.drawOval(cx - r + 8, cy - r + 8, (r - 8) * 2, (r - 8) * 2);
        // $ 符號
        int sw = Math.max(4, size / 12);
        g2d.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(cx, cy - r / 2, cx, cy + r / 2);
        g2d.drawArc(cx - r / 3, cy - r / 3, r * 2 / 3, r / 3, 0, 180);
        g2d.drawArc(cx - r / 3, cy, r * 2 / 3, r / 3, 180, 180);
    }

    /**
     * 繪製沙漏圖示（儲值餘額）
     */
    private void drawHourglassIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = (int)(size * 0.6);
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;
        // 上下兩條橫線
        g2d.drawLine(x, y, x + w, y);
        g2d.drawLine(x, y + h, x + w, y + h);
        // 交叉線（沙漏形狀）
        g2d.drawLine(x + 4, y + 4, cx + w / 2 - 4, cy);
        g2d.drawLine(cx + w / 2 - 4, cy, x + 4, y + h - 4);
        g2d.drawLine(x + w - 4, y + 4, cx - w / 2 + 4, cy);
        g2d.drawLine(cx - w / 2 + 4, cy, x + w - 4, y + h - 4);
        // 中間沙粒
        g2d.fillOval(cx - 3, cy - 3, 6, 6);
    }

    /**
     * 繪製愛心圖示（線上預約）
     */
    private void drawHeartIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 0.9);
        // 使用兩個圓弧和底部尖角構成愛心
        int topY = cy - h / 3;
        int r = w / 4;
        g2d.drawOval(cx - w / 2, topY - r, r * 2, r * 2);
        g2d.drawOval(cx, topY - r, r * 2, r * 2);
        // 下半部三角形
        int[] xPts = {cx - w / 2, cx + w / 2, cx};
        int[] yPts = {topY + r / 2, topY + r / 2, cy + h / 2};
        g2d.drawPolyline(xPts, yPts, 3);
    }

    /**
     * 繪製花朵圖示（1對1諮詢）
     */
    private void drawFlowerIcon(Graphics2D g2d, int cx, int cy, int size) {
        int r = size / 5;
        int dist = (int)(size * 0.32);
        // 花瓣（5 片）
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2 * i / 5 - Math.PI / 2;
            int px = cx + (int)(dist * Math.cos(angle));
            int py = cy + (int)(dist * Math.sin(angle));
            g2d.drawOval(px - r, py - r, r * 2, r * 2);
        }
        // 花心
        g2d.fillOval(cx - r / 2, cy - r / 2, r, r);
        // 花莖
        g2d.drawLine(cx, cy + dist, cx, cy + size / 2 + 5);
        // 葉子
        g2d.drawArc(cx, cy + dist + 5, size / 4, size / 6, 90, 180);
    }

    /**
     * 繪製收據圖示（消費紀錄）
     */
    private void drawReceiptIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = (int)(size * 0.75);
        int h = size;
        int x = cx - w / 2;
        int y = cy - h / 2;
        // 收據主體（矩形，底部鋸齒）
        g2d.drawLine(x, y, x, y + h);
        g2d.drawLine(x + w, y, x + w, y + h);
        g2d.drawLine(x, y, x + w, y);
        // 底部鋸齒
        int teeth = 4;
        int tw = w / teeth;
        for (int i = 0; i < teeth; i++) {
            int tx = x + i * tw;
            g2d.drawLine(tx, y + h, tx + tw / 2, y + h - 8);
            g2d.drawLine(tx + tw / 2, y + h - 8, tx + tw, y + h);
        }
        // 文字線條
        g2d.drawLine(x + 8, y + h / 4, x + w - 8, y + h / 4);
        g2d.drawLine(x + 8, y + h / 4 + 12, x + w - 8, y + h / 4 + 12);
        g2d.drawLine(x + 8, y + h / 4 + 24, x + w / 2, y + h / 4 + 24);
    }

    /**
     * 繪製星章圖示（電子優惠券）
     */
    private void drawCouponStarIcon(Graphics2D g2d, int cx, int cy, int size) {
        int r = size / 2;
        // 五角星
        int[] xPts = new int[10];
        int[] yPts = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2 * i / 10 - Math.PI / 2;
            int rad = (i % 2 == 0) ? r : (int)(r * 0.45);
            xPts[i] = cx + (int)(rad * Math.cos(angle));
            yPts[i] = cy + (int)(rad * Math.sin(angle));
        }
        g2d.drawPolygon(xPts, yPts, 10);
        // 中心圓
        g2d.drawOval(cx - 6, cy - 6, 12, 12);
    }

    /**
     * 繪製剪刀圖示（美容預約）
     */
    private void drawScissorsIcon(Graphics2D g2d, int cx, int cy, int size) {
        int r = size / 4;
        // 兩個圓圈（手柄）
        g2d.drawOval(cx - size / 2, cy + 2, r * 2, r * 2);
        g2d.drawOval(cx + size / 2 - r * 2, cy + 2, r * 2, r * 2);
        // 兩條交叉刀片
        g2d.drawLine(cx - size / 2 + r, cy + r, cx + 4, cy - size / 2 + 4);
        g2d.drawLine(cx + size / 2 - r, cy + r, cx - 4, cy - size / 2 + 4);
    }

    /**
     * 繪製房屋圖示（住宿預約）
     */
    private void drawHouseIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 0.7);
        int x = cx - w / 2;
        int y = cy;
        // 屋頂（三角形）
        g2d.drawLine(cx, cy - size / 2, x, y);
        g2d.drawLine(cx, cy - size / 2, x + w, y);
        // 牆壁
        g2d.drawRect(x + 6, y, w - 12, h);
        // 門
        g2d.drawRect(cx - 8, y + h / 3, 16, h - h / 3);
        // 門把
        g2d.fillOval(cx + 4, y + h / 3 + h / 3, 4, 4);
    }

    /**
     * 繪製信封圖示（會員優惠）
     */
    private void drawEnvelopeIcon(Graphics2D g2d, int cx, int cy, int size) {
        int w = size;
        int h = (int)(size * 0.7);
        int x = cx - w / 2;
        int y = cy - h / 2;
        // 信封主體
        g2d.drawRect(x, y, w, h);
        // 信封翻蓋（V 形）
        g2d.drawLine(x, y, cx, cy);
        g2d.drawLine(cx, cy, x + w, y);
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
        // ========================================
        // 1. 優先嘗試直接載入字型檔案（最可靠）
        // ========================================
        String[] fontFilePaths = {
                "/usr/share/fonts/wenquanyi/wqy-zenhei/wqy-zenhei.ttc",  // Alpine font-wqy-zenhei
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",           // Debian/Ubuntu
                "/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc",             // CentOS/RHEL
                "/usr/share/fonts/wenquanyi/wqy-microhei/wqy-microhei.ttc" // Alpine 微米黑
        };

        for (String path : fontFilePaths) {
            java.io.File fontFile = new java.io.File(path);
            if (fontFile.exists()) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    font = font.deriveFont(style, (float) size);
                    if (font.canDisplay('預')) {
                        log.debug("使用字型檔案：{}", path);
                        return font;
                    }
                } catch (Exception e) {
                    log.warn("載入字型檔案失敗 {}：{}", path, e.getMessage());
                }
            }
        }

        // ========================================
        // 2. 備援：使用系統字型名稱查找
        // ========================================
        String[] fontCandidates = {
                "WenQuanYi Zen Hei",     // Docker/Linux - 文泉驛正黑
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

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> availableFonts = new java.util.HashSet<>();
        for (String fontName : ge.getAvailableFontFamilyNames()) {
            availableFonts.add(fontName);
        }

        for (String fontName : fontCandidates) {
            if (availableFonts.contains(fontName)) {
                Font font = new Font(fontName, style, size);
                if (font.canDisplay('預')) {
                    log.debug("使用系統字型：{}", fontName);
                    return font;
                }
            }
        }

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

    /**
     * 安全下載圖片（不拋異常，失敗回傳 null）
     */
    private byte[] downloadImageSafe(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "BookingPlatform/1.0");

            int status = conn.getResponseCode();
            if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP
                    || status == java.net.HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                String redirect = conn.getHeaderField("Location");
                conn = (java.net.HttpURLConnection) new java.net.URL(redirect).openConnection();
                conn.setRequestProperty("User-Agent", "BookingPlatform/1.0");
            }

            if (conn.getResponseCode() == 200) {
                return conn.getInputStream().readAllBytes();
            }
        } catch (Exception e) {
            log.warn("下載圖片失敗 {}：{}", imageUrl, e.getMessage());
        }
        return null;
    }

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

                // ── 檢查是否有 cell 背景圖片 URL ──
                String cellImageUrl = cellConfig != null ? cellConfig.path("imageUrl").asText("") : "";
                boolean hasCellImage = false;
                if (!cellImageUrl.isEmpty()) {
                    try {
                        byte[] cellImgBytes = downloadImageSafe(cellImageUrl);
                        if (cellImgBytes != null) {
                            BufferedImage cellImg = ImageIO.read(new java.io.ByteArrayInputStream(cellImgBytes));
                            if (cellImg != null) {
                                // Cover 策略：填滿整格（置中裁切）
                                double scaleX = (double) cellW / cellImg.getWidth();
                                double scaleY = (double) cellH / cellImg.getHeight();
                                double scale = Math.max(scaleX, scaleY);
                                int drawW = (int) (cellImg.getWidth() * scale);
                                int drawH = (int) (cellImg.getHeight() * scale);
                                int drawX = cellX + (cellW - drawW) / 2;
                                int drawY = cellY + (cellH - drawH) / 2;

                                // 裁切到格子範圍
                                Shape oldClip = g2d.getClip();
                                g2d.setClip(cellX, cellY, cellW, cellH);
                                g2d.drawImage(cellImg, drawX, drawY, drawW, drawH, null);
                                g2d.setClip(oldClip);

                                hasCellImage = true;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("下載格子 {} 背景圖片失敗：{}", i, e.getMessage());
                    }
                }

                // ── 繪製圓形圖示（無 cell 背景圖時才顯示）──
                if (!hasCellImage && cellIcons != null && cellIcons.containsKey(i)) {
                    byte[] iconBytes = cellIcons.get(i);
                    if (iconBytes != null && iconBytes.length > 0) {
                        try {
                            BufferedImage iconImg = ImageIO.read(new java.io.ByteArrayInputStream(iconBytes));
                            if (iconImg != null) {
                                int iconSize = Math.min(cellW, cellH) / 3;
                                String iconShape = cellConfig != null ? cellConfig.path("iconShape").asText("circle") : "circle";

                                BufferedImage scaledIcon = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
                                Graphics2D iconG = scaledIcon.createGraphics();
                                try {
                                    iconG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                    iconG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                    if ("circle".equals(iconShape)) {
                                        iconG.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, iconSize, iconSize));
                                    }
                                    iconG.drawImage(iconImg, 0, 0, iconSize, iconSize, null);
                                } finally {
                                    iconG.dispose();
                                }
                                int iconX = centerX - iconSize / 2;
                                int iconY = centerY - iconSize / 2 - cellH / 8;
                                g2d.drawImage(scaledIcon, iconX, iconY, null);
                            }
                        } catch (IOException e) {
                            log.warn("載入格子 {} 圖示失敗：{}", i, e.getMessage());
                        }
                    }
                }

                // ── 繪製文字標籤（有 cell 背景圖時跳過，圖片就是圖片）──
                if (!hasCellImage) {
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

                        // 文字位置：有圖示時在圖示下方，否則格子中心
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
