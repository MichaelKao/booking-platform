package com.booking.platform.service.line;

import com.booking.platform.dto.line.ConversationContext;
import com.booking.platform.enums.line.ConversationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * LINE 對話狀態服務
 *
 * <p>管理 LINE 對話的狀態機，使用 Redis 儲存對話上下文
 *
 * <p>Redis Key 格式：line:conversation:{tenantId}:{lineUserId}
 * <p>預設 TTL：30 分鐘
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineConversationService {

    // ========================================
    // 依賴注入
    // ========================================

    private final RedisTemplate<String, Object> redisTemplate;

    // ========================================
    // 配置
    // ========================================

    /**
     * 對話狀態 TTL（秒）
     */
    @Value("${line.conversation.ttl:1800}")
    private int conversationTtl;

    /**
     * Redis Key 前綴
     */
    @Value("${line.conversation.key-prefix:line:conversation:}")
    private String keyPrefix;

    // ========================================
    // 查詢方法
    // ========================================

    /**
     * 取得對話上下文
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 對話上下文（如果不存在則建立新的）
     */
    public ConversationContext getContext(String tenantId, String lineUserId) {
        String key = buildKey(tenantId, lineUserId);

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // 刷新 TTL
                redisTemplate.expire(key, Duration.ofSeconds(conversationTtl));

                if (value instanceof ConversationContext) {
                    return (ConversationContext) value;
                }
            }
        } catch (Exception e) {
            log.warn("讀取對話上下文失敗，將建立新的：{}", e.getMessage());
        }

        // 建立新的對話上下文
        return createContext(tenantId, lineUserId, null);
    }

    /**
     * 檢查對話是否存在
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return true 表示存在
     */
    public boolean hasContext(String tenantId, String lineUserId) {
        String key = buildKey(tenantId, lineUserId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 取得當前對話狀態
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 對話狀態
     */
    public ConversationState getState(String tenantId, String lineUserId) {
        ConversationContext context = getContext(tenantId, lineUserId);
        return context.getState();
    }

    // ========================================
    // 寫入方法
    // ========================================

    /**
     * 建立對話上下文
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param customerId 顧客 ID（可選）
     * @return 新的對話上下文
     */
    public ConversationContext createContext(String tenantId, String lineUserId, String customerId) {
        ConversationContext context = ConversationContext.create(tenantId, lineUserId);
        context.setCustomerId(customerId);

        saveContext(context);

        log.debug("建立對話上下文，租戶：{}，LINE User：{}", tenantId, lineUserId);

        return context;
    }

    /**
     * 儲存對話上下文
     *
     * @param context 對話上下文
     */
    public void saveContext(ConversationContext context) {
        String key = buildKey(context.getTenantId(), context.getLineUserId());

        try {
            redisTemplate.opsForValue().set(key, context, Duration.ofSeconds(conversationTtl));
        } catch (Exception e) {
            log.error("儲存對話上下文失敗，租戶：{}，LINE User：{}",
                    context.getTenantId(), context.getLineUserId(), e);
        }
    }

    /**
     * 轉換對話狀態
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param newState   新狀態
     * @return 更新後的對話上下文
     */
    public ConversationContext transitionTo(String tenantId, String lineUserId, ConversationState newState) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.transitionTo(newState);
        saveContext(context);

        log.debug("對話狀態轉換，租戶：{}，LINE User：{}，新狀態：{}",
                tenantId, lineUserId, newState);

        return context;
    }

    /**
     * 設定選擇的分類
     *
     * @param tenantId     租戶 ID
     * @param lineUserId   LINE User ID
     * @param categoryId   分類 ID
     * @param categoryName 分類名稱
     * @return 更新後的對話上下文
     */
    public ConversationContext setSelectedCategory(
            String tenantId,
            String lineUserId,
            String categoryId,
            String categoryName
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.setCategory(categoryId, categoryName);
        context.transitionTo(ConversationState.SELECTING_SERVICE);
        saveContext(context);

        log.debug("設定選擇的分類，租戶：{}，LINE User：{}，分類：{}",
                tenantId, lineUserId, categoryName);

        return context;
    }

    /**
     * 設定選擇的服務
     *
     * @param tenantId    租戶 ID
     * @param lineUserId  LINE User ID
     * @param serviceId   服務 ID
     * @param serviceName 服務名稱
     * @param duration    服務時長（分鐘）
     * @param price       服務價格
     * @return 更新後的對話上下文
     */
    public ConversationContext setSelectedService(
            String tenantId,
            String lineUserId,
            String serviceId,
            String serviceName,
            Integer duration,
            Integer price
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        // 清除下游資料（日期、員工、時間），防止重複點擊導致舊資料殘留
        context.clearDownstreamFromDate();
        context.setService(serviceId, serviceName, duration, price);
        // 修改流程：選服務後先選日期，再選員工
        context.transitionTo(ConversationState.SELECTING_DATE);
        saveContext(context);

        log.debug("設定選擇的服務，租戶：{}，LINE User：{}，服務：{}",
                tenantId, lineUserId, serviceName);

        return context;
    }

    /**
     * 設定選擇的員工
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param staffId    員工 ID（null 表示不指定）
     * @param staffName  員工名稱
     * @return 更新後的對話上下文
     */
    public ConversationContext setSelectedStaff(
            String tenantId,
            String lineUserId,
            String staffId,
            String staffName
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        // 清除下游資料（時間），防止重複點擊導致舊資料殘留
        context.clearDownstreamFromTime();
        context.setStaff(staffId, staffName);
        // 修改流程：選員工後選時間
        context.transitionTo(ConversationState.SELECTING_TIME);
        saveContext(context);

        log.debug("設定選擇的員工，租戶：{}，LINE User：{}，員工：{}",
                tenantId, lineUserId, staffName != null ? staffName : "不指定");

        return context;
    }

    /**
     * 設定選擇的日期
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param date       日期
     * @return 更新後的對話上下文
     */
    public ConversationContext setSelectedDate(
            String tenantId,
            String lineUserId,
            LocalDate date
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        // 清除下游資料（員工、時間），防止重複點擊導致舊資料殘留
        context.clearDownstreamFromStaff();
        context.setSelectedDate(date);
        // 修改流程：選日期後選員工
        context.transitionTo(ConversationState.SELECTING_STAFF);
        saveContext(context);

        log.debug("設定選擇的日期，租戶：{}，LINE User：{}，日期：{}",
                tenantId, lineUserId, date);

        return context;
    }

    /**
     * 設定選擇的時間（進入備註輸入狀態）
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param time       時間
     * @return 更新後的對話上下文
     */
    public ConversationContext setSelectedTime(
            String tenantId,
            String lineUserId,
            LocalTime time
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.setSelectedTime(time);
        context.transitionTo(ConversationState.INPUTTING_NOTE);
        saveContext(context);

        log.debug("設定選擇的時間，租戶：{}，LINE User：{}，時間：{}",
                tenantId, lineUserId, time);

        return context;
    }

    /**
     * 設定顧客備註並進入確認狀態
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @param note       備註內容（可為 null 表示跳過）
     * @return 更新後的對話上下文
     */
    public ConversationContext setCustomerNote(
            String tenantId,
            String lineUserId,
            String note
    ) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.setCustomerNote(note);
        context.transitionTo(ConversationState.CONFIRMING_BOOKING);
        saveContext(context);

        log.debug("設定顧客備註，租戶：{}，LINE User：{}，備註：{}",
                tenantId, lineUserId, note != null ? note : "無");

        return context;
    }

    /**
     * 重置對話
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 重置後的對話上下文
     */
    public ConversationContext reset(String tenantId, String lineUserId) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.reset();
        saveContext(context);

        log.debug("重置對話，租戶：{}，LINE User：{}", tenantId, lineUserId);

        return context;
    }

    /**
     * 刪除對話上下文
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     */
    public void deleteContext(String tenantId, String lineUserId) {
        String key = buildKey(tenantId, lineUserId);
        redisTemplate.delete(key);

        log.debug("刪除對話上下文，租戶：{}，LINE User：{}", tenantId, lineUserId);
    }

    /**
     * 開始預約流程
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 更新後的對話上下文
     */
    public ConversationContext startBooking(String tenantId, String lineUserId) {
        return startBooking(tenantId, lineUserId, ConversationState.SELECTING_SERVICE);
    }

    /**
     * 開始預約流程（指定初始狀態）
     *
     * @param tenantId     租戶 ID
     * @param lineUserId   LINE User ID
     * @param initialState 初始狀態（SELECTING_CATEGORY 或 SELECTING_SERVICE）
     * @return 更新後的對話上下文
     */
    public ConversationContext startBooking(String tenantId, String lineUserId, ConversationState initialState) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.clearBookingData();
        context.transitionTo(initialState);
        saveContext(context);

        log.debug("開始預約流程，租戶：{}，LINE User：{}，初始狀態：{}", tenantId, lineUserId, initialState);

        return context;
    }

    /**
     * 回到上一步
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return 更新後的對話上下文
     */
    public ConversationContext goBack(String tenantId, String lineUserId) {
        ConversationContext context = getContext(tenantId, lineUserId);
        context.goBack();
        saveContext(context);

        log.debug("回到上一步，租戶：{}，LINE User：{}，當前狀態：{}",
                tenantId, lineUserId, context.getState());

        return context;
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 建立 Redis Key
     *
     * @param tenantId   租戶 ID
     * @param lineUserId LINE User ID
     * @return Redis Key
     */
    private String buildKey(String tenantId, String lineUserId) {
        return keyPrefix + tenantId + ":" + lineUserId;
    }
}
