package com.atherion.andromeda;

import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 32-byte key Base64-encoded — meets HMAC-SHA256 minimum key size
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-for-andromeda-01".getBytes());

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L); // 1 hour
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private User buildUser() {
        UserType ut = new UserType();
        ut.setId(1L);
        ut.setUserType("developer");

        User user = new User();
        user.setId(42L);
        user.setUsername("santi");
        user.setName("Santiago");
        user.setEmail("santi@test.com");
        user.setPasswordHash("$2a$10$placeholder");
        user.setUserType(ut);
        return user;
    }

    // ── generation ─────────────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonBlankString() {
        String token = jwtUtil.generateToken(buildUser());
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_hasThreeJwtParts() {
        String token = jwtUtil.generateToken(buildUser());
        assertEquals(3, token.split("\\.").length,
                "A signed JWT must have exactly three dot-separated parts");
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        User u1 = buildUser();

        User u2 = buildUser();
        u2.setId(99L);
        u2.setUsername("admin");

        assertNotEquals(jwtUtil.generateToken(u1), jwtUtil.generateToken(u2));
    }

    // ── extraction ─────────────────────────────────────────────────────────────

    @Test
    void extractUsername_returnsCorrectSubject() {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);
        assertEquals("santi", jwtUtil.extractUsername(token));
    }

    @Test
    void roundTrip_usernamePreservedAfterGenerateAndExtract() {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);
        assertEquals(user.getUsername(), jwtUtil.extractUsername(token));
    }

    // ── validation ─────────────────────────────────────────────────────────────

    @Test
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken(buildUser());
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken(buildUser());
        // Flip the last character of the signature to break it
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");
        assertFalse(jwtUtil.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 1L);
        String token = jwtUtil.generateToken(buildUser());
        Thread.sleep(20); // wait for the 1 ms expiry
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_randomNonJwtString_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.real.token"));
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid(""));
    }
}
