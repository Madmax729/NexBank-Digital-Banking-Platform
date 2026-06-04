package com.banking.user.controller;

import com.banking.user.entity.UserProfile;
import com.banking.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class UserController {
    private final UserProfileRepository userProfileRepository;

    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("X-User-Id") String userId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(UUID.fromString(userId))
                .orElseGet(() -> UserProfile.builder().authUserId(UUID.fromString(userId))
                        .fullName("User").email("user@bank.com").build());
        return ResponseEntity.ok(Map.of("success", true, "data", profile));
    }

    @PutMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader("X-User-Id") String userId, @RequestBody UserProfile updates) {
        UserProfile profile = userProfileRepository.findByAuthUserId(UUID.fromString(userId))
                .orElseGet(() -> UserProfile.builder().authUserId(UUID.fromString(userId)).build());
        if (updates.getFullName() != null) profile.setFullName(updates.getFullName());
        if (updates.getPhoneNumber() != null) profile.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getAddress() != null) profile.setAddress(updates.getAddress());
        if (updates.getCity() != null) profile.setCity(updates.getCity());
        if (updates.getEmail() != null) profile.setEmail(updates.getEmail());
        profile = userProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("success", true, "data", profile));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<UserProfile> users;
        if (search != null && !search.isEmpty()) {
            users = userProfileRepository.searchUsers(search, PageRequest.of(page, size));
        } else {
            users = userProfileRepository.findAll(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", users));
    }
}
