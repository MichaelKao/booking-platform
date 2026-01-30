package com.booking.platform.dto.response;

import com.booking.platform.enums.CustomerStatus;
import com.booking.platform.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 顧客回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String lineUserId;
    private String lineDisplayName;
    private String linePictureUrl;
    private String name;
    private String nickname;
    private String displayName;
    private String phone;
    private String email;
    private Gender gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String address;
    private CustomerStatus status;
    private Boolean isLineBlocked;

    private String membershipLevelId;
    private String membershipLevelName;
    private BigDecimal totalSpent;
    private Integer visitCount;
    private Integer pointBalance;
    private Integer noShowCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastVisitAt;

    private String note;
    private String tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
