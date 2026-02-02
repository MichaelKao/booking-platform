package com.booking.platform.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Tenant 店家後台頁面 Controller
 *
 * 處理店家後台的頁面路由
 */
@Controller
@RequestMapping("/tenant")
public class TenantPageController {

    // ========================================
    // 登入與首頁
    // ========================================

    /**
     * 登入頁面
     */
    @GetMapping("/login")
    public String login() {
        return "tenant/login";
    }

    /**
     * 店家註冊頁面
     */
    @GetMapping("/register")
    public String register() {
        return "tenant/register";
    }

    /**
     * 忘記密碼頁面
     */
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "tenant/forgot-password";
    }

    /**
     * 重設密碼頁面
     */
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "tenant/reset-password";
    }

    /**
     * 首頁重導向到儀表板
     */
    @GetMapping("")
    public String index() {
        return "redirect:/tenant/dashboard";
    }

    /**
     * 儀表板頁面
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageTitle", "儀表板");
        return "tenant/dashboard";
    }

    // ========================================
    // 預約管理
    // ========================================

    /**
     * 預約列表頁面
     */
    @GetMapping("/bookings")
    public String bookings(Model model) {
        model.addAttribute("currentPage", "bookings");
        model.addAttribute("pageTitle", "預約管理");
        return "tenant/bookings";
    }

    /**
     * 行事曆頁面
     */
    @GetMapping("/calendar")
    public String calendar(Model model) {
        model.addAttribute("currentPage", "calendar");
        model.addAttribute("pageTitle", "行事曆");
        return "tenant/calendar";
    }

    // ========================================
    // 顧客管理
    // ========================================

    /**
     * 顧客列表頁面
     */
    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("currentPage", "customers");
        model.addAttribute("pageTitle", "顧客管理");
        return "tenant/customers";
    }

    /**
     * 顧客詳情頁面
     */
    @GetMapping("/customers/{id}")
    public String customerDetail(@PathVariable String id, Model model) {
        model.addAttribute("currentPage", "customers");
        model.addAttribute("pageTitle", "顧客詳情");
        model.addAttribute("customerId", id);
        return "tenant/customer-detail";
    }

    /**
     * 會員等級頁面
     */
    @GetMapping("/membership-levels")
    public String membershipLevels(Model model) {
        model.addAttribute("currentPage", "membership-levels");
        model.addAttribute("pageTitle", "會員等級");
        return "tenant/membership-levels";
    }

    // ========================================
    // 店家設定
    // ========================================

    /**
     * 員工管理頁面
     */
    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("currentPage", "staff");
        model.addAttribute("pageTitle", "員工管理");
        return "tenant/staff";
    }

    /**
     * 服務項目頁面
     */
    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("currentPage", "services");
        model.addAttribute("pageTitle", "服務項目");
        return "tenant/services";
    }

    /**
     * 商品管理頁面
     */
    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("currentPage", "products");
        model.addAttribute("pageTitle", "商品管理");
        return "tenant/products";
    }

    // ========================================
    // 行銷推廣
    // ========================================

    /**
     * 票券管理頁面
     */
    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("currentPage", "coupons");
        model.addAttribute("pageTitle", "票券管理");
        return "tenant/coupons";
    }

    /**
     * 行銷活動頁面
     */
    @GetMapping("/campaigns")
    public String campaigns(Model model) {
        model.addAttribute("currentPage", "campaigns");
        model.addAttribute("pageTitle", "行銷活動");
        return "tenant/campaigns";
    }

    /**
     * 行銷推播頁面
     */
    @GetMapping("/marketing")
    public String marketing(Model model) {
        model.addAttribute("currentPage", "marketing");
        model.addAttribute("pageTitle", "行銷推播");
        return "tenant/marketing";
    }

    // ========================================
    // 系統設定
    // ========================================

    /**
     * 店家設定頁面
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("currentPage", "settings");
        model.addAttribute("pageTitle", "店家設定");
        return "tenant/settings";
    }

    /**
     * LINE 設定頁面
     */
    @GetMapping("/line-settings")
    public String lineSettings(Model model) {
        model.addAttribute("currentPage", "line-settings");
        model.addAttribute("pageTitle", "LINE 設定");
        return "tenant/line-settings";
    }

    /**
     * 功能商店頁面
     */
    @GetMapping("/feature-store")
    public String featureStore(Model model) {
        model.addAttribute("currentPage", "feature-store");
        model.addAttribute("pageTitle", "功能商店");
        return "tenant/feature-store";
    }

    /**
     * 點數管理頁面
     */
    @GetMapping("/points")
    public String points(Model model) {
        model.addAttribute("currentPage", "points");
        model.addAttribute("pageTitle", "點數管理");
        return "tenant/points";
    }

    /**
     * 營運報表頁面
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("pageTitle", "營運報表");
        return "tenant/reports";
    }
}
