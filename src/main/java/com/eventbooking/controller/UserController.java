package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.user.ProfileRequest;
import com.eventbooking.dto.user.ReminderRequest;
import com.eventbooking.dto.user.ReminderResponse;
import com.eventbooking.dto.user.UserResponse;
import com.eventbooking.entity.Reminder;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.ReminderRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final FavoriteService favoriteService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    public ApiResponse<UserResponse> getProfile() {
        return ApiResponse.success("Profile retrieved successfully", toResponse(currentUser()));
    }

    @GetMapping("/favorites")
    @Operation(summary = "List current user's favorite events")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorite events retrieved successfully")
    public ApiResponse<PageResponse<EventResponse>> getFavoriteEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("Favorite events retrieved successfully", favoriteService.myFavoriteEvents(page, size));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user's profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully")
    public ApiResponse<UserResponse> updateProfile(@RequestBody ProfileRequest request) {
        User user = currentUser();
        user.setFullName(request.getFullName());
        user.setAvatar(request.getAvatar());
        return ApiResponse.success("Profile updated successfully", toResponse(userRepository.save(user)));
    }

    @PutMapping("/reminders")
    @Operation(summary = "Update reminder settings")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reminder updated successfully")
    public ApiResponse<ReminderResponse> updateReminder(@Valid @RequestBody ReminderRequest request) {
        User user = currentUser();
        Reminder reminder = reminderRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> {
                    Reminder created = new Reminder();
                    created.setUser(user);
                    return created;
                });
        reminder.setEventReminder(request.getEventReminder());
        return ApiResponse.success("Reminder updated successfully",
                new ReminderResponse(reminderRepository.save(reminder).getEventReminder()));
    }

    private User currentUser() {
        return userRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private UserResponse toResponse(User user) {
        String role = user.getRoles().stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse("USER");
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAvatar(), role);
    }
}
