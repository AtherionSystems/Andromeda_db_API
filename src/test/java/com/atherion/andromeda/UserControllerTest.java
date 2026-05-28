package com.atherion.andromeda;

import com.atherion.andromeda.controllers.UserController;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.services.UserService;
import com.atherion.andromeda.services.UserTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService          userService;
    @Mock private UserTypeService      userTypeService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private UserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private User buildUser(Long id, String username) {
        UserType ut = new UserType();
        ut.setId(1L);
        ut.setUserType("developer");

        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPasswordHash("$2a$10$hash");
        user.setUserType(ut);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    // ── GET /api/users ─────────────────────────────────────────────────────────

    @Test
    void getAll_returns200WithUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                buildUser(1L, "alice"),
                buildUser(2L, "bob")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    void getAll_emptyList_returns200WithEmptyArray() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAll_responseNeverExposePasswordHash() throws Exception {
        when(userService.findAll()).thenReturn(List.of(buildUser(1L, "alice")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    // ── GET /api/users/{id} ────────────────────────────────────────────────────

    @Test
    void getById_found_returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice")));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(userService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    // ── PUT /api/users/{id} ────────────────────────────────────────────────────

    @Test
    void update_found_updatesNameAndReturns200() throws Exception {
        User existing = buildUser(1L, "alice");
        User saved    = buildUser(1L, "alice");
        saved.setName("Alice Updated");

        when(userService.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(userService.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void update_duplicateUsername_returns409() throws Exception {
        User existing = buildUser(1L, "alice");
        when(userService.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.usernameExists("bob")).thenReturn(true);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    void update_newPassword_isEncodedBeforeSave() throws Exception {
        User existing = buildUser(1L, "alice");
        when(userService.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPass123")).thenReturn("$2a$10$encodedHash");
        when(userService.save(any(User.class))).thenReturn(existing);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"newPass123\"}"))
                .andExpect(status().isOk());

        verify(passwordEncoder).encode("newPass123");
    }

    @Test
    void update_sameUsername_doesNotCheckForDuplicate() throws Exception {
        User existing = buildUser(1L, "alice");
        when(userService.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.save(any(User.class))).thenReturn(existing);

        // sending the same username back must not trigger a conflict check
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isOk());

        verify(userService, never()).usernameExists("alice");
    }

    // ── DELETE /api/users/{id} ─────────────────────────────────────────────────

    @Test
    void delete_found_returns204AndCallsService() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(buildUser(1L, "alice")));
        doNothing().when(userService).deleteById(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteById(1L);
    }

    @Test
    void delete_notFound_returns404AndNeverCallsDelete() throws Exception {
        when(userService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userService, never()).deleteById(any());
    }
}
