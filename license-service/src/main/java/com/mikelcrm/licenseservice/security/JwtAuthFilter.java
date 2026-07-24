package com.mikelcrm.licenseservice.security;

import com.mikelcrm.licenseservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JWT authentication filter that validates tokens from TWO sources:
 * 1. Keycloak JWT (RS256, validated via JWKS endpoint)
 * 2. Local JWT (RS256, validated via configured public key)
 *
 * Keycloak tokens have issuer containing "/realms/".
 * Local tokens have issuer "auth-service" or similar.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicKey localPublicKey;
    private final long maxTtlMinutes;
    private final boolean skipValidation;
    private final String keycloakJwksUrl;

    // Cache JWKS keys by kid
    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();
    private long jwksCacheExpiry = 0;
    private static final long JWKS_CACHE_TTL_MS = 300_000; // 5 minutes

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/api/v1/health") ||
               path.startsWith("/api/v1/payments") ||
               path.startsWith("/api/v1/notifications") ||
               path.startsWith("/api/v1/plans") ||
               path.startsWith("/api/v1/funcion-roles") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }

    public JwtAuthFilter(JwtProperties jwtProperties) {
        this.skipValidation = jwtProperties.isSkipValidation();
        this.maxTtlMinutes = jwtProperties.getMaxTtlMinutes();
        this.keycloakJwksUrl = jwtProperties.getKeycloakJwksUrl();
        if (skipValidation) {
            this.localPublicKey = null;
            log.warn("⚠️  JWT validation is DISABLED (skip-validation=true). This should only be used in development!");
        } else {
            this.localPublicKey = (jwtProperties.getPublicKey() != null && !jwtProperties.getPublicKey().isBlank())
                    ? parsePublicKey(jwtProperties.getPublicKey()) : null;
        }
        if (keycloakJwksUrl != null && !keycloakJwksUrl.isBlank()) {
            log.info("Keycloak JWKS validation enabled: {}", keycloakJwksUrl);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // If already authenticated (e.g., by GatewayHeaderAuthFilter), skip
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Dev mode: skip validation
        if (skipValidation) {
            UUID devTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID devUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            TenantAuthenticationToken devAuth = new TenantAuthenticationToken(devUserId, devTenantId, "admin");
            SecurityContextHolder.getContext().setAuthentication(devAuth);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Determine token source by decoding header to get kid, and payload to check issuer
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT structure");

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(payloadJson);
            String issuer = payload.has("iss") ? payload.get("iss").asText() : "";

            Claims claims;

            if (issuer.contains("/realms/") && keycloakJwksUrl != null) {
                // Keycloak token — validate via JWKS
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
                JsonNode header = mapper.readTree(headerJson);
                String kid = header.has("kid") ? header.get("kid").asText() : null;

                PublicKey keycloakKey = getKeycloakKey(kid);
                if (keycloakKey == null) {
                    throw new IllegalArgumentException("Unknown kid: " + kid);
                }

                claims = Jwts.parser()
                        .verifyWith(keycloakKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Extract Keycloak-style claims
                String sub = claims.getSubject();
                String rol = claims.get("rol", String.class);
                if (rol == null) {
                    // Try roles array
                    Object rolesObj = claims.get("roles");
                    if (rolesObj instanceof List<?> rolesList && !rolesList.isEmpty()) {
                        rol = rolesList.get(0).toString();
                    }
                }
                String tenantId = claims.get("tenant_id", String.class);

                UUID userId = UUID.fromString(sub);
                UUID tenantUuid = (tenantId != null) ? deriveTenantId(tenantId) : UUID.fromString("00000000-0000-0000-0000-000000000001");

                TenantAuthenticationToken authentication =
                        new TenantAuthenticationToken(userId, tenantUuid, rol != null ? rol : "tecnico");
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else if (localPublicKey != null) {
                // Local token — validate with configured public key
                claims = Jwts.parser()
                        .verifyWith(localPublicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                validateTtl(claims);

                UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
                UUID userId = UUID.fromString(claims.get("userId", String.class));
                String rol = claims.get("rol", String.class);

                TenantAuthenticationToken authentication =
                        new TenantAuthenticationToken(userId, tenantId, rol);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                throw new IllegalArgumentException("No key available to validate token");
            }

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired\"}");
            return;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"detail\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UUID deriveTenantId(String tenantId) {
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(tenantId.getBytes());
        }
    }

    private PublicKey getKeycloakKey(String kid) {
        // Check cache
        if (System.currentTimeMillis() < jwksCacheExpiry && jwksCache.containsKey(kid)) {
            return jwksCache.get(kid);
        }

        // Fetch JWKS
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(keycloakJwksUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("Failed to fetch JWKS: status {}", resp.statusCode());
                return jwksCache.get(kid); // return stale cache if available
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jwks = mapper.readTree(resp.body());
            JsonNode keys = jwks.get("keys");

            jwksCache.clear();
            for (JsonNode key : keys) {
                String keyId = key.get("kid").asText();
                String n = key.get("n").asText();
                String e = key.get("e").asText();
                PublicKey pubKey = buildRsaPublicKey(n, e);
                jwksCache.put(keyId, pubKey);
            }
            jwksCacheExpiry = System.currentTimeMillis() + JWKS_CACHE_TTL_MS;

            return jwksCache.get(kid);
        } catch (Exception e) {
            log.error("Error fetching JWKS: {}", e.getMessage());
            return jwksCache.get(kid); // return stale
        }
    }

    private PublicKey buildRsaPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] eBytes = Base64.getUrlDecoder().decode(exponentBase64);
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private void validateTtl(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        if (issuedAt != null && expiration != null) {
            Duration tokenDuration = Duration.between(
                    issuedAt.toInstant(),
                    expiration.toInstant()
            );
            if (tokenDuration.toMinutes() > maxTtlMinutes) {
                throw new IllegalArgumentException(
                        "Token TTL exceeds maximum allowed: " + tokenDuration.toMinutes() + " > " + maxTtlMinutes);
            }
        }

        // Also reject if token is expired (JJWT does this by default, but explicit check)
        if (expiration != null && expiration.toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token is expired");
        }
    }

    private static PublicKey parsePublicKey(String publicKeyStr) {
        try {
            // Strip PEM headers if present
            String cleanKey = publicKeyStr
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key", e);
        }
    }
}
