package com.atherion.andromeda.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@Profile("prod")
public class JwksController {

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<String> jwks() throws IOException {
        InputStream is = getClass().getResourceAsStream("/jwks.json");
        String json = new String(is.readAllBytes());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}