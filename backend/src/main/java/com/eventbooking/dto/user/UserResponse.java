package com.eventbooking.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private Long id;

    private String username;

    private String fullName;

    private String email;

    private String avatar;

    private String role;
}
