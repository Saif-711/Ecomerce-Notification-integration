package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.LoginRequest;
import com.ecommerce.platform.dto.UserRequest;
import com.ecommerce.platform.dto.UserResponse;
import com.ecommerce.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User registration, login, and profile APIs")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with username, email, and password")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user with username and password")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile", description = "Retrieves user profile by ID")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long id) {
        UserResponse response = userService.getProfile(id);
        return ResponseEntity.ok(response);
    }
}
