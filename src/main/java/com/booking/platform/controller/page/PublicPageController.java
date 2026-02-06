package com.booking.platform.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 公開頁面 Controller
 *
 * 處理不需要登入的公開頁面路由
 * 包含首頁（Landing Page）、FAQ、功能介紹等 SEO 相關頁面
 */
@Controller
public class PublicPageController {

    /**
     * 首頁 - Landing Page
     * SEO 優化的介紹頁面，用於搜尋引擎索引
     */
    @GetMapping("/")
    public String index() {
        return "public/index";
    }

    /**
     * 常見問題 FAQ 頁面
     * SEO 優化，包含結構化資料
     */
    @GetMapping("/faq")
    public String faq() {
        return "public/faq";
    }

    /**
     * 功能介紹頁面
     * SEO 優化，詳細介紹平台功能
     */
    @GetMapping("/features")
    public String features() {
        return "public/features";
    }

    /**
     * 價格方案頁面
     * SEO 優化，展示價格與方案
     */
    @GetMapping("/pricing")
    public String pricing() {
        return "public/pricing";
    }

    // ========================================
    // 行業專屬頁面（SEO 長尾關鍵字）
    // ========================================

    /**
     * 美容預約系統
     */
    @GetMapping("/beauty")
    public String beauty() {
        return "public/industry/beauty";
    }

    /**
     * 美髮預約系統
     */
    @GetMapping("/hair-salon")
    public String hairSalon() {
        return "public/industry/hair-salon";
    }

    /**
     * SPA 按摩預約系統
     */
    @GetMapping("/spa")
    public String spa() {
        return "public/industry/spa";
    }

    /**
     * 健身教練預約系統
     */
    @GetMapping("/fitness")
    public String fitness() {
        return "public/industry/fitness";
    }

    /**
     * 餐廳訂位系統
     */
    @GetMapping("/restaurant")
    public String restaurant() {
        return "public/industry/restaurant";
    }

    /**
     * 診所預約系統
     */
    @GetMapping("/clinic")
    public String clinic() {
        return "public/industry/clinic";
    }

    // ========================================
    // 城市專屬頁面（SEO 地區長尾關鍵字）
    // ========================================

    /**
     * 台北預約系統
     */
    @GetMapping("/taipei")
    public String taipei() {
        return "public/city/taipei";
    }

    /**
     * 新北預約系統
     */
    @GetMapping("/new-taipei")
    public String newTaipei() {
        return "public/city/new-taipei";
    }

    /**
     * 桃園預約系統
     */
    @GetMapping("/taoyuan")
    public String taoyuan() {
        return "public/city/taoyuan";
    }

    /**
     * 台中預約系統
     */
    @GetMapping("/taichung")
    public String taichung() {
        return "public/city/taichung";
    }

    /**
     * 台南預約系統
     */
    @GetMapping("/tainan")
    public String tainan() {
        return "public/city/tainan";
    }

    /**
     * 高雄預約系統
     */
    @GetMapping("/kaohsiung")
    public String kaohsiung() {
        return "public/city/kaohsiung";
    }

    /**
     * 新竹預約系統
     */
    @GetMapping("/hsinchu")
    public String hsinchu() {
        return "public/city/hsinchu";
    }

    // ========================================
    // 法律頁面
    // ========================================

    /**
     * 服務條款頁面
     */
    @GetMapping("/terms")
    public String terms() {
        return "public/terms";
    }

    /**
     * 隱私權政策頁面
     */
    @GetMapping("/privacy")
    public String privacy() {
        return "public/privacy";
    }
}
