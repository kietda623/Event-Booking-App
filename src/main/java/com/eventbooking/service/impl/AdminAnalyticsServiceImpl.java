package com.eventbooking.service.impl;

import com.eventbooking.dto.admin.AdminAnalyticsResponse;
import com.eventbooking.dto.admin.TopEventResponse;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public AdminAnalyticsResponse getAnalytics() {
        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        bookingsByStatus.put("PENDING", bookingRepository.countByStatus("PENDING"));
        bookingsByStatus.put("PAID", bookingRepository.countByStatus("PAID"));
        bookingsByStatus.put("CANCELLED", bookingRepository.countByStatus("CANCELLED"));

        List<TopEventResponse> topEvents = bookingRepository.findTopEventsByPaidQuantity(PageRequest.of(0, 5)).stream()
                .map(row -> new TopEventResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();

        return new AdminAnalyticsResponse(
                eventRepository.count(),
                userRepository.count(),
                bookingRepository.count(),
                bookingRepository.sumPaidRevenue(),
                bookingsByStatus,
                topEvents
        );
    }
}
