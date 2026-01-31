package com.booking.platform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Favicon 控制器
 *
 * <p>處理瀏覽器自動請求的 favicon.ico
 *
 * <p>回傳 204 No Content 避免錯誤日誌
 *
 * @author Developer
 * @since 1.0.0
 */
@Controller
public class FaviconController {

    /**
     * 處理 favicon.ico 請求
     *
     * @return 204 No Content
     */
    @GetMapping("favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
