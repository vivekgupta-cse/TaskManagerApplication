package com.taskmanager.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // In production, move this to application.properties and rotate keys
    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Token validity: 1 hour
    private static final long TTL_SECONDS = 60 * 60;

    public String generateToken(String username) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = Instant.now().getEpochSecond();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", username);
            payload.put("iat", now);
            payload.put("exp", now + TTL_SECONDS);

            String headerJson = MAPPER.writeValueAsString(header);
            String payloadJson = MAPPER.writeValueAsString(payload);

            String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerB64 + "." + payloadB64;
            String signature = computeHmacSha256(signingInput, SECRET);

            return signingInput + "." + signature;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate authentication token", ex);
        }
    }

    public String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT token format");

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            String signingInput = headerB64 + "." + payloadB64;
            String expectedSig = computeHmacSha256(signingInput, SECRET);
            if (!constantTimeEquals(expectedSig, signatureB64)) {
                throw new RuntimeException("Invalid JWT signature");
            }

            byte[] payloadBytes = base64UrlDecode(payloadB64);
            Map<String, Object> payload = MAPPER.readValue(payloadBytes, Map.class);

            // check exp
            Object expObj = payload.get("exp");
            if (expObj != null) {
                long exp = ((Number) expObj).longValue();
                long now = Instant.now().getEpochSecond();
                if (now > exp) throw new RuntimeException("JWT token expired");
            }

            Object sub = payload.get("sub");
            return sub == null ? null : sub.toString();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse JWT token", ex);
        }
    }

    // Helpers
    private static String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(sig);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    // Prevent timing attacks for signature comparison
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
