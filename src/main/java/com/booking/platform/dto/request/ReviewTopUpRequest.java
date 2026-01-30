package com.booking.platform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 審核儲值申請請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTopUpRequest {

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String note;
}
