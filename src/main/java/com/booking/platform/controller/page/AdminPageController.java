package com.booking.platform.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin 後台頁面 Controller
 *
 * 處理超級管理後台的頁面路由
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    // ========================================
    // 登入頁面
    // ========================================

    /**
     * 登入頁面
     */
    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    // ========================================
    // 儀表板
    // ========================================

    /**
     * 儀表板頁面
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageTitle", "儀表板");
        return "admin/dashboard";
    }

    /**
     * 首頁重導向到儀表板
     */
    @GetMapping("")
    public String index() {
        return "redirect:/admin/dashboard";
    }

    // ========================================
    // 店家管理
    // ========================================

    /**
     * 店家列表頁面
     */
    @GetMapping("/tenants")
    public String tenants(Model model) {
        model.addAttribute("currentPage", "tenants");
        model.addAttribute("pageTitle", "店家管理");
        return "admin/tenants";
    }

    /**
     * 店家詳情頁面
     */
    @GetMapping("/tenants/{id}")
    public String tenantDetail(@PathVariable String id, Model model) {
        model.addAttribute("currentPage", "tenants");
        model.addAttribute("pageTitle", "店家詳情");
        model.addAttribute("tenantId", id);
        return "admin/tenant-detail";
    }

    // ========================================
    // 財務管理
    // ========================================

    /**
     * 儲值審核頁面
     */
    @GetMapping("/point-topups")
    public String pointTopups(Model model) {
        model.addAttribute("currentPage", "point-topups");
        model.addAttribute("pageTitle", "儲值審核");
        return "admin/point-topups";
    }

    // ========================================
    // 系統管理
    // ========================================

    /**
     * 功能管理頁面
     */
    @GetMapping("/features")
    public String features(Model model) {
        model.addAttribute("currentPage", "features");
        model.addAttribute("pageTitle", "功能管理");
        return "admin/features";
    }
}
