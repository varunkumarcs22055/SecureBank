package com.securebank.user.controller;

import com.securebank.common.dto.ApiResponse;
import com.securebank.user.dto.UserResponse;
import com.securebank.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse user = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", user));
    }
}
