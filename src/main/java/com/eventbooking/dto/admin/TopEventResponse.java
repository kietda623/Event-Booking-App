package com.eventbooking.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopEventResponse {
    private Long id;
    private String title;
    private Long bookedCount;
}
