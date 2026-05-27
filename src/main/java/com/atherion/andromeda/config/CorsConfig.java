package com.atherion.andromeda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * CorsConfigurationSource — NOT CorsFilter.
     *
     * SecurityConfig calls .cors(withDefaults()), which instructs Spring Security to
     * look for a CorsConfigurationSource bean and apply it inside the security filter
     * chain.  Using a CorsFilter bean instead would register a second, independent CORS
     * filter and process every request twice.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // setAllowedOriginPatterns instead of setAllowedOrigins:
        // identical behaviour for explicit URLs, but also supports wildcard ("*") without
        // conflicting with allowCredentials if we ever open it up.
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://159.54.154.149",
                "http://140.84.181.23",
                "http://163.192.143.43",
                "http://160.34.209.27"
        ));

        // PATCH is required — the API has several @PatchMapping endpoints.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all request headers, including Authorization (Bearer token).
        config.setAllowedHeaders(List.of("*"));

        // Expose Location so the browser can read the URI returned in 201 Created responses.
        config.setExposedHeaders(List.of("Location"));

        // JWT Bearer tokens live in the Authorization header, not in cookies.
        // allowCredentials controls cookie / HTTP-auth propagation — not Bearer tokens —
        // so false is correct for a stateless JWT API.
        config.setAllowCredentials(false);

        // Cache the preflight (OPTIONS) result for 1 hour to reduce browser round-trips.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
