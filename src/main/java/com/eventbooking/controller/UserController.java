package com.eventbooking.controller;

import com.eventbooking.dto.user.ProfileRequest;
import com.eventbooking.dto.user.ReminderRequest;
import com.eventbooking.dto.user.ReminderResponse;
import com.eventbooking.dto.user.UserResponse;
import com.eventbooking.entity.Reminder;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.ReminderRepository;
import com.eventbooking.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;

    @GetMapping("/profile")
    public UserResponse getProfile() {
        return toResponse(currentUser());
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@RequestBody ProfileRequest request) {
        User user = currentUser();
        user.setFullName(request.getFullName());
        user.setAvatar(request.getAvatar());
        return toResponse(userRepository.save(user));
    }

    @PutMapping("/reminders")
    public ReminderResponse updateReminder(@Valid @RequestBody ReminderRequest request) {
        User user = currentUser();
        Reminder reminder = reminderRepository.findByUserUsername(user.getUsername())
                .orElseGet(() -> {
                    Reminder created = new Reminder();
                    created.setUser(user);
                    return created;
                });
        reminder.setEventReminder(request.getEventReminder());
        return new ReminderResponse(reminderRepository.save(reminder).getEventReminder());
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private UserResponse toResponse(User user) {
        String role = user.getRoles().stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse("USER");
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getAvatar(), role);
    }
}
