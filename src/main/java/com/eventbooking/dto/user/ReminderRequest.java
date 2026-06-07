package com.eventbooking.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReminderRequest {
    @NotNull
    private Boolean eventReminder;
}
