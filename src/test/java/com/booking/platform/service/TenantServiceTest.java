package com.booking.platform.service;

import com.booking.platform.common.exception.BusinessException;
import com.booking.platform.common.exception.ResourceNotFoundException;
import com.booking.platform.dto.request.CreateTenantRequest;
import com.booking.platform.dto.request.UpdateTenantRequest;
import com.booking.platform.dto.response.TenantDetailResponse;
import com.booking.platform.dto.response.TenantResponse;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.TenantStatus;
import com.booking.platform.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * 租戶服務測試
 *
 * @author Developer
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantServiceTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // 建立測試用租戶
        testTenant = Tenant.builder()
                .code("test-shop")
                .name("測試店家")
                .description("這是一個測試店家")
                .phone("0912345678")
                .email("test@shop.com")
                .address("台北市測試路1號")
                .status(TenantStatus.ACTIVE)
                .isTestAccount(true)
                .pointBalance(BigDecimal.valueOf(1000))
                .maxStaffCount(5)
                .monthlyPushQuota(200)
                .monthlyPushUsed(0)
                .build();

        String tenantId = java.util.UUID.randomUUID().toString();
        testTenant.setId(tenantId);
        testTenant.setTenantId(tenantId);

        tenantRepository.save(testTenant);
    }

    @Test
    @DisplayName("建立租戶成功")
    void create_Success() {
        CreateTenantRequest request = CreateTenantRequest.builder()
                .code("new-shop")
                .name("新店家")
                .description("新店家描述")
                .phone("0987654321")
                .email("new@shop.com")
                .address("台北市新店路2號")
                .isTestAccount(false)
                .build();

        TenantResponse response = tenantService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("new-shop");
        assertThat(response.getName()).isEqualTo("新店家");
        assertThat(response.getStatus()).isEqualTo(TenantStatus.PENDING);
    }

    @Test
    @DisplayName("建立租戶失敗 - 代碼重複")
    void create_DuplicateCode() {
        CreateTenantRequest request = CreateTenantRequest.builder()
                .code("test-shop") // 已存在的代碼
                .name("另一個店家")
                .build();

        assertThatThrownBy(() -> tenantService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("查詢租戶詳情成功")
    void getDetail_Success() {
        TenantDetailResponse response = tenantService.getDetail(testTenant.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testTenant.getId());
        assertThat(response.getCode()).isEqualTo("test-shop");
        assertThat(response.getName()).isEqualTo("測試店家");
    }

    @Test
    @DisplayName("查詢租戶詳情失敗 - 不存在")
    void getDetail_NotFound() {
        assertThatThrownBy(() -> tenantService.getDetail("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("更新租戶成功")
    void update_Success() {
        UpdateTenantRequest request = UpdateTenantRequest.builder()
                .name("更新後的店家名稱")
                .description("更新後的描述")
                .phone("0911111111")
                .build();

        TenantResponse response = tenantService.update(testTenant.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("更新後的店家名稱");
    }

    @Test
    @DisplayName("啟用租戶成功")
    void activate_Success() {
        // 先將租戶狀態設為 PENDING
        testTenant.setStatus(TenantStatus.PENDING);
        tenantRepository.save(testTenant);

        TenantResponse response = tenantService.activate(testTenant.getId());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("停用租戶成功")
    void suspend_Success() {
        TenantResponse response = tenantService.suspend(testTenant.getId());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("增加點數成功")
    void addPoints_Success() {
        BigDecimal originalBalance = testTenant.getPointBalance();
        BigDecimal addAmount = BigDecimal.valueOf(500);

        TenantResponse response = tenantService.addPoints(testTenant.getId(), addAmount);

        assertThat(response).isNotNull();
        assertThat(response.getPointBalance()).isEqualByComparingTo(originalBalance.add(addAmount));
    }

    @Test
    @DisplayName("刪除租戶成功（軟刪除）")
    void delete_Success() {
        tenantService.delete(testTenant.getId());

        // 軟刪除後應該查不到
        assertThatThrownBy(() -> tenantService.getDetail(testTenant.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
