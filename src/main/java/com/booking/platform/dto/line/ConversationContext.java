package com.booking.platform.dto.line;

import com.booking.platform.enums.line.ConversationState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 對話上下文
 *
 * <p>儲存在 Redis 中，記錄用戶的對話狀態和暫存資料
 *
 * <p>Redis Key 格式：line:conversation:{tenantId}:{lineUserId}
 * <p>TTL：30 分鐘
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========================================
    // 識別資訊
    // ========================================

    /**
     * 租戶 ID
     */
    private String tenantId;

    /**
     * LINE User ID
     */
    private String lineUserId;

    /**
     * 顧客 ID
     */
    private String customerId;

    // ========================================
    // 對話狀態
    // ========================================

    /**
     * 當前對話狀態
     */
    @Builder.Default
    private ConversationState state = ConversationState.IDLE;

    /**
     * 狀態更新時間
     */
    private LocalDateTime stateChangedAt;

    // ========================================
    // 預約暫存資料
    // ========================================

    /**
     * 選擇的服務 ID
     */
    private String selectedServiceId;

    /**
     * 選擇的服務名稱
     */
    private String selectedServiceName;

    /**
     * 選擇的服務時長（分鐘）
     */
    private Integer selectedServiceDuration;

    /**
     * 選擇的服務價格
     */
    private Integer selectedServicePrice;

    /**
     * 選擇的員工 ID（null 表示不指定）
     */
    private String selectedStaffId;

    /**
     * 選擇的員工名稱
     */
    private String selectedStaffName;

    /**
     * 選擇的日期
     */
    private LocalDate selectedDate;

    /**
     * 選擇的時間
     */
    private LocalTime selectedTime;

    /**
     * 待取消的預約 ID
     */
    private String cancelBookingId;

    /**
     * 顧客備註
     */
    private String customerNote;

    // ========================================
    // 商品暫存資料
    // ========================================

    /**
     * 選擇的商品 ID
     */
    private String selectedProductId;

    /**
     * 選擇的商品名稱
     */
    private String selectedProductName;

    /**
     * 選擇的商品價格
     */
    private Integer selectedProductPrice;

    /**
     * 選擇的購買數量
     */
    private Integer selectedQuantity;

    // ========================================
    // 票券暫存資料
    // ========================================

    /**
     * 選擇的票券 ID
     */
    private String selectedCouponId;

    /**
     * 選擇的票券名稱
     */
    private String selectedCouponName;

    // ========================================
    // 回溯資訊
    // ========================================

    /**
     * 上一個狀態（用於返回）
     */
    private ConversationState previousState;

    // ========================================
    // 業務方法
    // ========================================

    /**
     * 轉換到新狀態
     *
     * @param newState 新狀態
     */
    public void transitionTo(ConversationState newState) {
        this.previousState = this.state;
        this.state = newState;
        this.stateChangedAt = LocalDateTime.now();
    }

    /**
     * 回到上一個狀態
     */
    public void goBack() {
        if (this.previousState != null) {
            this.state = this.previousState;
            this.previousState = null;
            this.stateChangedAt = LocalDateTime.now();
        }
    }

    /**
     * 重置對話（回到閒置狀態）
     */
    public void reset() {
        this.state = ConversationState.IDLE;
        this.previousState = null;
        this.stateChangedAt = LocalDateTime.now();
        clearAllData();
    }

    /**
     * 清除預約暫存資料
     */
    public void clearBookingData() {
        this.selectedServiceId = null;
        this.selectedServiceName = null;
        this.selectedServiceDuration = null;
        this.selectedServicePrice = null;
        this.selectedStaffId = null;
        this.selectedStaffName = null;
        this.selectedDate = null;
        this.selectedTime = null;
        this.cancelBookingId = null;
        this.customerNote = null;
    }

    /**
     * 清除商品暫存資料
     */
    public void clearProductData() {
        this.selectedProductId = null;
        this.selectedProductName = null;
        this.selectedProductPrice = null;
        this.selectedQuantity = null;
    }

    /**
     * 清除票券暫存資料
     */
    public void clearCouponData() {
        this.selectedCouponId = null;
        this.selectedCouponName = null;
    }

    /**
     * 清除所有暫存資料
     */
    public void clearAllData() {
        clearBookingData();
        clearProductData();
        clearCouponData();
    }

    /**
     * 設定服務資訊
     *
     * @param serviceId   服務 ID
     * @param serviceName 服務名稱
     * @param duration    服務時長（分鐘）
     * @param price       服務價格
     */
    public void setService(String serviceId, String serviceName, Integer duration, Integer price) {
        this.selectedServiceId = serviceId;
        this.selectedServiceName = serviceName;
        this.selectedServiceDuration = duration;
        this.selectedServicePrice = price;
    }

    /**
     * 設定員工資訊
     *
     * @param staffId   員工 ID
     * @param staffName 員工名稱
     */
    public void setStaff(String staffId, String staffName) {
        this.selectedStaffId = staffId;
        this.selectedStaffName = staffName;
    }

    /**
     * 設定預約時間
     *
     * @param date 日期
     * @param time 時間
     */
    public void setDateTime(LocalDate date, LocalTime time) {
        this.selectedDate = date;
        this.selectedTime = time;
    }

    /**
     * 設定商品資訊
     *
     * @param productId   商品 ID
     * @param productName 商品名稱
     * @param price       商品價格
     */
    public void setProduct(String productId, String productName, Integer price) {
        this.selectedProductId = productId;
        this.selectedProductName = productName;
        this.selectedProductPrice = price;
    }

    /**
     * 設定購買數量
     *
     * @param quantity 數量
     */
    public void setQuantity(Integer quantity) {
        this.selectedQuantity = quantity;
    }

    /**
     * 設定票券資訊
     *
     * @param couponId   票券 ID
     * @param couponName 票券名稱
     */
    public void setCoupon(String couponId, String couponName) {
        this.selectedCouponId = couponId;
        this.selectedCouponName = couponName;
    }

    /**
     * 設定待取消的預約 ID
     *
     * @param bookingId 預約 ID
     */
    public void setCancelBookingId(String bookingId) {
        this.cancelBookingId = bookingId;
    }

    /**
     * 設定顧客備註
     *
     * @param note 備註內容
     */
    public void setCustomerNote(String note) {
        this.customerNote = note;
    }

    /**
     * 取得顧客備註
     *
     * @return 備註內容
     */
    public String getCustomerNote() {
        return this.customerNote;
    }

    /**
     * 檢查是否可以確認購買
     *
     * @return true 表示可以確認
     */
    public boolean canConfirmPurchase() {
        return this.selectedProductId != null && this.selectedQuantity != null && this.selectedQuantity > 0;
    }

    /**
     * 檢查是否可以確認預約（所有必要資訊都已填寫）
     *
     * @return true 表示可以確認
     */
    public boolean canConfirmBooking() {
        return this.selectedServiceId != null
                && this.selectedDate != null
                && this.selectedTime != null;
    }

    /**
     * 取得預約開始時間
     *
     * @return 預約開始時間
     */
    @JsonIgnore
    public LocalDateTime getBookingStartTime() {
        if (this.selectedDate == null || this.selectedTime == null) {
            return null;
        }
        return LocalDateTime.of(this.selectedDate, this.selectedTime);
    }

    /**
     * 取得預約結束時間
     *
     * @return 預約結束時間
     */
    @JsonIgnore
    public LocalDateTime getBookingEndTime() {
        LocalDateTime startTime = getBookingStartTime();
        if (startTime == null || this.selectedServiceDuration == null) {
            return null;
        }
        return startTime.plusMinutes(this.selectedServiceDuration);
    }

    /**
     * 建立新的對話上下文
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 新的對話上下文
     */
    public static ConversationContext create(String tenantId, String lineUserId) {
        return ConversationContext.builder()
                .tenantId(tenantId)
                .lineUserId(lineUserId)
                .state(ConversationState.IDLE)
                .stateChangedAt(LocalDateTime.now())
                .build();
    }
}
