package com.booking.platform.controller;

import com.booking.platform.repository.line.TenantLineConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 公開圖片存取控制器
 *
 * <p>提供不需要認證即可存取的圖片（供 LINE Flex Message 使用）
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicImageController {

    private final TenantLineConfigRepository lineConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * 取得 Flex Menu 卡片圖片
     *
     * @param tenantId 租戶 ID
     * @param cardIndex 卡片索引
     * @return JPEG 圖片
     */
    @GetMapping("/flex-card-image/{tenantId}/{cardIndex}")
    public ResponseEntity<byte[]> getFlexCardImage(
            @PathVariable String tenantId,
            @PathVariable int cardIndex
    ) {
        return lineConfigRepository.findByTenantId(tenantId)
                .map(config -> {
                    try {
                        if (config.getFlexMenuCardImages() == null || config.getFlexMenuCardImages().isBlank()) {
                            return ResponseEntity.notFound().<byte[]>build();
                        }
                        Map<String, String> images = objectMapper.readValue(
                                config.getFlexMenuCardImages(),
                                new TypeReference<Map<String, String>>() {});
                        String base64 = images.get(String.valueOf(cardIndex));
                        if (base64 == null) {
                            return ResponseEntity.notFound().<byte[]>build();
                        }
                        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
                        return ResponseEntity.ok()
                                .contentType(MediaType.IMAGE_JPEG)
                                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                                .body(imageBytes);
                    } catch (Exception e) {
                        log.error("讀取卡片圖片失敗，租戶：{}，索引：{}", tenantId, cardIndex, e);
                        return ResponseEntity.notFound().<byte[]>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
