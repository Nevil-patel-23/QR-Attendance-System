package com.university.attendance.controller;

import com.university.attendance.dto.request.PasswordChangeRequest;
import com.university.attendance.dto.response.UserProfileResponse;
import com.university.attendance.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PostMapping("/password")
    public ResponseEntity<String> updatePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.updatePassword(request);
        return ResponseEntity.ok("Password updated successfully");
    }
}
