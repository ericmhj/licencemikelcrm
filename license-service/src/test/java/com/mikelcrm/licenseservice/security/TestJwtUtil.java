package com.mikelcrm.licenseservice.security;

import io.jsonwebtoken.Jwts;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Test utility for generating signed JWTs for integration and unit tests.
 * Generates an RSA key pair on initialization and provides methods to create
 * tokens with configurable claims (tenantId, userId, rol) and TTL.
 */
public final class TestJwtUtil {

    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate RSA key pair for tests", e);
        }
    }

    private TestJwtUtil() {
        // utility class
    }

    /**
     * Returns the RSA public key used for signing test tokens.
     */
    public static PublicKey getPublicKey() {
        return KEY_PAIR.getPublic();
    }

    /**
     * Returns the RSA private key used for signing test tokens.
     */
    public static PrivateKey getPrivateKey() {
        return KEY_PAIR.getPrivate();
    }

    /**
     * Returns the public key encoded as a Base64 string (suitable for JwtProperties configuration).
     */
    public static String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }

    /**
     * Returns the public key in PEM format with headers.
     */
    public static String getPublicKeyPem() {
        String base64 = getPublicKeyBase64();
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    /**
     * Generates a valid signed JWT with the specified claims and a 15-minute TTL.
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID
     * @param rol      the user role (e.g., ADMIN_CUENTA, SUPERVISOR, TECNICO, ASISTENTE)
     * @return a signed JWT string
     */
    public static String generateToken(UUID tenantId, UUID userId, String rol) {
        return generateToken(tenantId, userId, rol, 15);
    }

    /**
     * Generates a valid signed JWT with the specified claims and custom TTL.
     *
     * @param tenantId   the tenant UUID
     * @param userId     the user UUID
     * @param rol        the user role
     * @param ttlMinutes the token time-to-live in minutes
     * @return a signed JWT string
     */
    public static String generateToken(UUID tenantId, UUID userId, String rol, long ttlMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("rol", rol)
                .issuer("auth-service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Generates an expired token (for testing rejection of expired tokens).
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID
     * @param rol      the user role
     * @return a signed JWT string that is already expired
     */
    public static String generateExpiredToken(UUID tenantId, UUID userId, String rol) {
        Instant issuedAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant expiration = Instant.now().minus(15, ChronoUnit.MINUTES);
        return Jwts.builder()
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("rol", rol)
                .issuer("auth-service")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Generates a token with TTL exceeding the allowed maximum (for testing TTL validation).
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID
     * @param rol      the user role
     * @return a signed JWT string with 60-minute TTL
     */
    public static String generateTokenWithExcessiveTtl(UUID tenantId, UUID userId, String rol) {
        return generateToken(tenantId, userId, rol, 60);
    }

    /**
     * Returns the Authorization header value for a valid token.
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID
     * @param rol      the user role
     * @return "Bearer {token}" string ready to use in Authorization header
     */
    public static String bearerToken(UUID tenantId, UUID userId, String rol) {
        return "Bearer " + generateToken(tenantId, userId, rol);
    }
}
