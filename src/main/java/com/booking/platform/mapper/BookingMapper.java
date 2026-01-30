package com.booking.platform.mapper;

import com.booking.platform.dto.response.BookingResponse;
import com.booking.platform.entity.booking.Booking;
import org.springframework.stereotype.Component;

/**
 * 預約物件轉換器
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking entity) {
        if (entity == null) {
            return null;
        }

        return BookingResponse.builder()
                .id(entity.getId())
                .bookingDate(entity.getBookingDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .customerPhone(entity.getCustomerPhone())
                .staffId(entity.getStaffId())
                .staffName(entity.getStaffName())
                .serviceId(entity.getServiceId())
                .serviceName(entity.getServiceName())
                .price(entity.getPrice())
                .duration(entity.getDuration())
                .status(entity.getStatus())
                .customerNote(entity.getCustomerNote())
                .internalNote(entity.getInternalNote())
                .cancelReason(entity.getCancelReason())
                .cancelledAt(entity.getCancelledAt())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
