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
}
