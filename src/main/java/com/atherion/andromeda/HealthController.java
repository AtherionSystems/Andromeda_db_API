package com.atherion.andromeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "api",     "Andromeda Backend API",
            "version", "0.0.1-SNAPSHOT",
            "status",  "UP"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
