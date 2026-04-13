package com.atherion.andromeda;

import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.repositories.UserTypeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTypeRepository userTypeRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private static Long userTypeId;
    private static Long createdUserId;

    // ── setup ──────────────────────────────────────────────────────────────────

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @BeforeAll
    static void seedUserType(@Autowired UserTypeRepository userTypeRepository) {
        // find-or-create: previous test run may have left this row if cleanup crashed
        userTypeId = userTypeRepository.findIdByUserType("tester").orElseGet(() -> {
            UserType ut = new UserType();
            ut.setUserType("tester");
            ut.setDescription("Auth integration test role");
            return userTypeRepository.save(ut).getId();
        });
    }

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepository,
                        @Autowired UserTypeRepository userTypeRepository) {
        // Use JPQL deletes — avoids findById which reads Oracle DATE columns (ORA-18716)
        if (createdUserId != null)
            userRepository.deleteByIdJpql(createdUserId);
        if (userTypeId != null)
            userTypeRepository.deleteByIdJpql(userTypeId);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String registerJson(String username, String email) {
        return """
                {
                  "name": "Test User",
                  "username": "%s",
                  "password": "secret123",
                  "email": "%s",
                  "userTypeId": %d
                }
                """.formatted(username, email, userTypeId);
    }

    private String loginJson(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    // ── register tests ─────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void register_success_returns201AndUserResponse() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("auth_user", "auth@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("auth_user"))
                .andExpect(jsonPath("$.email").value("auth@test.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // extract id for cleanup — parse manually, no ObjectMapper needed
        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        createdUserId = Long.valueOf(idStr);
    }

    @Test
    @Order(2)
    void register_duplicateUsername_returns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("auth_user", "other@test.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    @Order(3)
    void register_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("other_user", "auth@test.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    @Order(4)
    void register_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── password hash verification ─────────────────────────────────────────────

    @Test
    @Order(5)
    void register_passwordIsStoredAsHash_notPlaintext() {
        User saved = userRepository.findByUsername("auth_user").orElseThrow();
        assertNotEquals("secret123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("secret123", saved.getPasswordHash()),
                "Stored hash should match the original password");
    }

    // ── login tests ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    void login_correctCredentials_returns200AndUserResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("auth_user", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth_user"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @Order(7)
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("auth_user", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    void login_unknownUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ghost_user", "secret123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
