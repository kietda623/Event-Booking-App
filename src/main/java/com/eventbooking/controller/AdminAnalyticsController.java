package com.eventbooking.controller;

import com.eventbooking.dto.admin.AdminAnalyticsResponse;
import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics")
public class AdminAnalyticsController {
    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/analytics")
    @Operation(summary = "Get admin analytics")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    public ApiResponse<AdminAnalyticsResponse> getAnalytics() {
        return ApiResponse.success("Analytics retrieved successfully", adminAnalyticsService.getAnalytics());
    }
}
