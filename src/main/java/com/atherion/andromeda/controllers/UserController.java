package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.UpdateUserRequest;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserTypeService userTypeService;
    private final BCryptPasswordEncoder passwordEncoder;

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        List<UserResponse> users = userService.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return userService.findById(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found")));
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRequest req) {
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        if (req.name() != null)     user.setName(req.name());
        if (req.email() != null)    user.setEmail(req.email());
        if (req.phone() != null)    user.setPhone(req.phone());

        if (req.username() != null && !req.username().equals(user.getUsername())) {
            if (userService.usernameExists(req.username())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already taken"));
            }
            user.setUsername(req.username());
        }

        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }

        if (req.userTypeId() != null) {
            UserType userType = userTypeService.findById(req.userTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("UserType not found: " + req.userTypeId()));
            user.setUserType(userType);
        }

        return ResponseEntity.ok(UserResponse.from(userService.save(user)));
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
