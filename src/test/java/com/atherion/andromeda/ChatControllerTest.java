package com.atherion.andromeda;

import com.atherion.andromeda.controllers.ChatController;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.telegram.AiIntentRouter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private AiIntentRouter aiIntentRouter;
    @Mock private UserRepository userRepository;
    @InjectMocks private ChatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // Simula el JWT interno (perfil dev): principal = username string
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── POST /api/chat ─────────────────────────────────────────────────────────

    @Test
    void chat_validMessage_returns200WithReply() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(aiIntentRouter.route("list my projects", 1L)).thenReturn("Projects (2)\n[1] Alpha\n[2] Beta");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"list my projects\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Projects (2)\n[1] Alpha\n[2] Beta"));
    }

    @Test
    void chat_unknownUser_returns404() throws Exception {
        // usuario no registrado en la app
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"list my projects\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void chat_aiReturnsNull_returns400() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(aiIntentRouter.route("gibberish xyz", 1L)).thenReturn(null);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"gibberish xyz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No response from AI"));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_missingMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
