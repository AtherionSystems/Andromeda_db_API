package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.LoginRequest;
import com.atherion.andromeda.dto.RegisterRequest;
import com.atherion.andromeda.dto.UserResponse;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.services.UserService;
import com.atherion.andromeda.services.UserTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserTypeService userTypeService;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.usernameExists(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already taken"));
        }
        if (userService.emailExists(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        UserType userType = userTypeService.findById(req.userTypeId())
                .orElseThrow(() -> new IllegalArgumentException("UserType not found: " + req.userTypeId()));

        User user = new User();
        user.setName(req.name());
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setUserType(userType);

        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userService.findByUsername(req.username()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        return ResponseEntity.ok(UserResponse.from(user));
    }
}
