package com.booking.platform.dto.response;

import com.booking.platform.enums.StaffStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 員工回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {

    private String id;
    private String name;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String phone;
    private String email;
    private StaffStatus status;
    private Boolean isBookable;
    private Boolean isVisible;
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
