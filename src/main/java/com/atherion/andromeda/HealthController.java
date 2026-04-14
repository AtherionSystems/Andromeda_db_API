package com.atherion.andromeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("health",   "GET  /health");
        endpoints.put("register", "POST /api/auth/register");
        endpoints.put("login",    "POST /api/auth/login");
        endpoints.put("users",    "GET  /api/users");
        endpoints.put("user",     "GET  /api/users/{id}");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",       "Andromeda Backend API");
        response.put("version",   "0.0.1-SNAPSHOT");
        response.put("status",    "UP");
        response.put("endpoints", endpoints);
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
