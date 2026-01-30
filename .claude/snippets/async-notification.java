/**
 * 非同步通知片段
 * 
 * 使用方式：發送通知時不阻塞主流程
 */

// Service 中發送非同步通知
@Transactional
public BookingResponse createBooking(CreateBookingRequest request) {
    // ... 建立預約邏輯 ...
    
    // 非同步發送通知（不阻塞主流程）
    notificationService.sendBookingConfirmationAsync(booking);
    
    return bookingMapper.toResponse(booking);
}

// NotificationService 中的非同步方法
@Async("notificationExecutor")
public void sendBookingConfirmationAsync(Booking booking) {
    try {
        // 發送 LINE 訊息
        lineMessageService.sendBookingConfirmation(booking);
        
        // 記錄成功
        saveNotificationLog(booking.getId(), NotificationStatus.SUCCESS, null);
    } catch (Exception e) {
        // 記錄失敗（可以稍後重試）
        saveNotificationLog(booking.getId(), NotificationStatus.FAILED, e.getMessage());
        log.error("發送預約確認通知失敗", e);
    }
}