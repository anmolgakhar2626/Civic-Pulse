package com.smartcity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Utilities Unit Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    // 256-bit base64-encoded secret
    private final String testSecret = "c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
    }

    @Test
    @DisplayName("Generate JWT token from Authentication and successfully extract username and roles")
    void testGenerateTokenAndParseClaims() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "operator_raj",
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_COMMAND_OPERATOR"))
        );

        String token = jwtUtils.generateJwtToken(auth);
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("operator_raj", jwtUtils.getUsernameFromJwtToken(token));
        assertEquals(expirationMs, jwtUtils.getExpirationMs());
    }

    @Test
    @DisplayName("Generate JWT token directly from username and roles")
    void testGenerateTokenFromUsername() {
        String token = jwtUtils.generateTokenFromUsername("admin_user", List.of("ROLE_ADMIN"));
        assertNotNull(token);

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("admin_user", jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    @DisplayName("Validate token returns false for invalid / tampered token")
    void testValidateTamperedToken() {
        String validToken = jwtUtils.generateTokenFromUsername("citizen_jane", List.of("ROLE_CITIZEN"));
        String tamperedToken = validToken + "corrupted";

        assertFalse(jwtUtils.validateJwtToken(tamperedToken));
    }

    @Test
    @DisplayName("Validate token returns false for malformed string")
    void testValidateMalformedToken() {
        assertFalse(jwtUtils.validateJwtToken("not.a.valid.jwt.token"));
        assertFalse(jwtUtils.validateJwtToken(""));
        assertFalse(jwtUtils.validateJwtToken(null));
    }

    @Test
    @DisplayName("Validate token returns false for expired token")
    void testValidateExpiredToken() throws InterruptedException {
        JwtUtils shortLivedUtils = new JwtUtils();
        ReflectionTestUtils.setField(shortLivedUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(shortLivedUtils, "jwtExpirationMs", 1L); // 1 millisecond

        String token = shortLivedUtils.generateTokenFromUsername("fast_expire", List.of("ROLE_CITIZEN"));
        Thread.sleep(10); // allow it to expire

        assertFalse(shortLivedUtils.validateJwtToken(token));
    }
}
