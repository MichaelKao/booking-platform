package com.booking.platform.common.config;

import com.booking.platform.entity.catalog.ServiceItem;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.ServiceStatus;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.repository.ServiceItemRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 資料初始化器
 *
 * <p>在應用程式啟動時執行初始化操作
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    // ========================================
    // 依賴注入
    // ========================================

    private final AuthService authService;
    private final TenantRepository tenantRepository;
    private final ServiceItemRepository serviceItemRepository;

    // ========================================
    // 初始化
    // ========================================

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("開始資料初始化...");
        log.info("========================================");

        // 初始化預設超級管理員帳號
        initAdminUser();

        // 初始化測試服務資料
        initTestServices();

        log.info("========================================");
        log.info("資料初始化完成");
        log.info("========================================");
    }

    /**
     * 初始化預設超級管理員帳號
     */
    private void initAdminUser() {
        try {
            authService.initDefaultAdminUser();
        } catch (Exception e) {
            log.error("初始化超級管理員帳號失敗：{}", e.getMessage());
        }
    }

    /**
     * 初始化測試服務資料
     */
    private void initTestServices() {
        try {
            // 查詢所有啟用的租戶
            List<Tenant> tenants = tenantRepository.findAll().stream()
                    .filter(t -> t.getDeletedAt() == null)
                    .filter(t -> TenantStatus.ACTIVE.equals(t.getStatus()))
                    .toList();

            for (Tenant tenant : tenants) {
                // 檢查是否已有服務
                List<ServiceItem> existingServices = serviceItemRepository
                        .findByTenantIdAndStatusAndDeletedAtIsNull(tenant.getId(), ServiceStatus.ACTIVE);

                if (existingServices.isEmpty()) {
                    log.info("為租戶 {} 建立預設服務...", tenant.getCode());
                    createDefaultServices(tenant.getId());
                }
            }
        } catch (Exception e) {
            log.error("初始化測試服務失敗：{}", e.getMessage());
        }
    }

    /**
     * 建立預設服務
     */
    private void createDefaultServices(String tenantId) {
        // 服務 1: 剪髮
        ServiceItem haircut = ServiceItem.builder()
                .name("剪髮")
                .description("專業剪髮服務，包含洗髮及造型")
                .price(new BigDecimal("500"))
                .duration(60)
                .bufferTime(10)
                .status(ServiceStatus.ACTIVE)
                .isVisible(true)
                .requiresStaff(true)
                .sortOrder(1)
                .build();
        haircut.setTenantId(tenantId);
        serviceItemRepository.save(haircut);

        // 服務 2: 染髮
        ServiceItem coloring = ServiceItem.builder()
                .name("染髮")
                .description("時尚染髮服務，使用高品質染劑")
                .price(new BigDecimal("1500"))
                .duration(120)
                .bufferTime(15)
                .status(ServiceStatus.ACTIVE)
                .isVisible(true)
                .requiresStaff(true)
                .sortOrder(2)
                .build();
        coloring.setTenantId(tenantId);
        serviceItemRepository.save(coloring);

        // 服務 3: 護髮
        ServiceItem treatment = ServiceItem.builder()
                .name("護髮")
                .description("深層護髮療程，修護受損髮質")
                .price(new BigDecimal("800"))
                .duration(45)
                .bufferTime(5)
                .status(ServiceStatus.ACTIVE)
                .isVisible(true)
                .requiresStaff(true)
                .sortOrder(3)
                .build();
        treatment.setTenantId(tenantId);
        serviceItemRepository.save(treatment);

        // 服務 4: 洗髮
        ServiceItem shampoo = ServiceItem.builder()
                .name("洗髮")
                .description("舒適洗髮服務，含頭皮按摩")
                .price(new BigDecimal("200"))
                .duration(30)
                .bufferTime(5)
                .status(ServiceStatus.ACTIVE)
                .isVisible(true)
                .requiresStaff(true)
                .sortOrder(4)
                .build();
        shampoo.setTenantId(tenantId);
        serviceItemRepository.save(shampoo);

        log.info("已為租戶建立 4 項預設服務");
    }
}
