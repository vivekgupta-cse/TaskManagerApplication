package com.taskmanager.app.service;
import com.taskmanager.app.config.JwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
public class JwtServiceTest {
    private JwtService jwtService;
    private JwtConfig jwtConfig;
    @BeforeEach
    void setUp() throws Exception {
        jwtConfig = Mockito.mock(JwtConfig.class);
        when(jwtConfig.getSecret()).thenReturn("test-secret-0123456789");
        jwtService = new JwtService(jwtConfig);
        // set ttlSeconds (private field) via reflection for deterministic tests
        Field ttlField = JwtService.class.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        ttlField.setLong(jwtService, 3600L);
    }
    @AfterEach
    void tearDown() {
        // nothing to cleanup
    }
    @Test
    void generateAndValidateToken_shouldReturnUsername() {
        String token = jwtService.generateToken("alice");
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have three parts");
        String username = jwtService.extractUsernameIfTokenIsValid(token);
        assertEquals("alice", username);
    }
    @Test
    void extractUsernameIfTokenIsValid_malformedToken_returnsNull() {
        String result = jwtService.extractUsernameIfTokenIsValid("not-a-valid-token");
        assertNull(result);
    }
    @Test
    void extractUsernameIfTokenIsValid_tamperedSignature_returnsNull() {
        String token = jwtService.generateToken("bob");
        String[] parts = token.split("\\.");
        // change signature (last part)
        parts[2] = parts[2] + "tamper";
        String tampered = String.join(".", parts);
        assertNull(jwtService.extractUsernameIfTokenIsValid(tampered));
    }
    @Test
    void generateToken_withNegativeTtl_createsExpiredToken() throws Exception {
        // set ttlSeconds to negative so generated token is expired
        Field ttlField = JwtService.class.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        ttlField.setLong(jwtService, -10L);
        String token = jwtService.generateToken("carol");
        assertNotNull(token);
        assertNull(jwtService.extractUsernameIfTokenIsValid(token));
    }
    @Test
    void constantTimeEquals_reflection_checksBothBranches() throws Exception {
        // equal strings
        String a = "abcdef";
        String b = "abcdef";
        java.lang.reflect.Method m = JwtService.class.getDeclaredMethod("constantTimeEquals", String.class, String.class);
        m.setAccessible(true);
        boolean eq = (boolean) m.invoke(null, a, b);
        assertTrue(eq);
        // different lengths -> false
        String c = "short";
        String d = "longer";
        boolean neq = (boolean) m.invoke(null, c, d);
        assertFalse(neq);
    }
}
