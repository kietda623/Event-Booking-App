package com.eventbooking.dto.seat;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SeatHoldRequest {
    @NotEmpty
    private List<String> seatNumbers;
}
