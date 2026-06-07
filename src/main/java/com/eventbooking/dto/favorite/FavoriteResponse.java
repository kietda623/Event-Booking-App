package com.eventbooking.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FavoriteResponse {
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String location;
    private Double price;
    private Boolean favorited;
}
