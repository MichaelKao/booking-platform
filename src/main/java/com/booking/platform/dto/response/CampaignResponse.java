package com.booking.platform.dto.response;

import com.booking.platform.enums.CampaignStatus;
import com.booking.platform.enums.CampaignType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行銷活動回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private String id;
    private String name;
    private String description;
    private CampaignType type;
    private CampaignStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endAt;

    private String imageUrl;
    private BigDecimal thresholdAmount;
    private Integer recallDays;
    private String couponId;
    private String couponName;
    private Integer bonusPoints;
    private String pushMessage;
    private Boolean isAutoTrigger;
    private Integer participantCount;
    private String note;
    private Boolean isEffective;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
