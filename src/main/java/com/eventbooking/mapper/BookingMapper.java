package com.eventbooking.mapper;

import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Refund;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.util.SeatNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMapper {
    private final RefundRepository refundRepository;

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getTitle(),
                booking.getQuantity(),
                booking.getTotalPrice(),
                booking.getBookingDate(),
                booking.getStatus(),
                refundRepository.findFirstByBookingIdOrderByIdDesc(booking.getId())
                        .map(Refund::getStatus)
                        .orElse(null),
                booking.getTier() == null ? null : booking.getTier().getId(),
                booking.getTier() == null ? null : booking.getTier().getName(),
                SeatNumberUtils.split(booking.getSeatNumbers())
        );
    }
}
