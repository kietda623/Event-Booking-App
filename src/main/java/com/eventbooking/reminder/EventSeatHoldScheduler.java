package com.eventbooking.reminder;

import com.eventbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventSeatHoldScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventSeatHoldScheduler.class);

    private final SeatRepository seatRepository;

    @Scheduled(fixedRate = 120000)
    @Transactional
    public int releaseExpiredHolds() {
        int released = seatRepository.releaseExpiredHolds(LocalDateTime.now());
        if (released > 0) {
            log.info("expired_seat_holds_released count={}", released);
        }
        return released;
    }
}
