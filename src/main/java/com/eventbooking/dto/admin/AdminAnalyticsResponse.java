package com.eventbooking.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private long totalEvents;
    private long totalUsers;
    private long totalBookings;
    private Double totalRevenue;
    private Map<String, Long> bookingsByStatus;
    private List<TopEventResponse> topEvents;
}
