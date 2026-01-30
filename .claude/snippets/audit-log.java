/**
 * 稽核日誌片段
 * 
 * 使用方式：記錄重要操作
 */

// 記錄建立操作
auditLogService.log(
        AuditAction.CREATE,
        "Booking",
        booking.getId(),
        null,           // 建立沒有舊值
        booking,        // 新值
        "建立預約：" + booking.getServiceName()
);

// 記錄更新操作
auditLogService.log(
        AuditAction.UPDATE,
        "Booking",
        booking.getId(),
        oldBooking,     // 舊值
        booking,        // 新值
        "更新預約狀態為：" + booking.getStatus()
);

// 記錄刪除操作
auditLogService.log(
        AuditAction.DELETE,
        "Booking",
        booking.getId(),
        booking,        // 舊值
        null,           // 刪除沒有新值
        "刪除預約"
);