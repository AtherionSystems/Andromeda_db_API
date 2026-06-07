package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.ChatRequest;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.telegram.AiIntentRouter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.atherion.andromeda.util.ControllerUtils.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiIntentRouter aiIntentRouter;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = resolveUserId(authentication);
        if (userId == null) return notFound("User not found");

        String reply = aiIntentRouter.route(request.message(), userId);
        if (reply == null) return badRequest("No response from AI");

        return ResponseEntity.ok(Map.of("reply", reply));
    }

    // prod (OCI JWT): principal is Jwt, resolved by iamSub
    // dev (internal JWT): principal is username String, resolved by username
    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return userRepository.findByIamSub(jwt.getSubject())
                    .map(u -> u.getId()).orElse(null);
        }
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getId()).orElse(null);
        }
        return null;
    }
}
