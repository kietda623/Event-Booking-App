package com.eventbooking.util;

import java.util.Arrays;
import java.util.List;

public final class SeatNumberUtils {
    private SeatNumberUtils() {
    }

    public static List<String> split(String seatNumbers) {
        if (seatNumbers == null || seatNumbers.isBlank()) {
            return List.of();
        }
        return Arrays.stream(seatNumbers.split(","))
                .filter(seatNumber -> !seatNumber.isBlank())
                .map(String::trim)
                .toList();
    }

    public static List<String> normalize(List<String> seatNumbers) {
        if (seatNumbers == null) {
            return List.of();
        }
        return seatNumbers.stream()
                .filter(seatNumber -> seatNumber != null && !seatNumber.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
