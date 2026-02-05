package com.booking.platform.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 公開頁面 Controller
 *
 * 處理不需要登入的公開頁面路由
 * 包含首頁（Landing Page）等 SEO 相關頁面
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
}
