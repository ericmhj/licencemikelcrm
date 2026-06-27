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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT authentication filter that validates RS256-signed tokens,
 * checks TTL, and extracts tenant context claims.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicKey publicKey;
    private final long maxTtlMinutes;
    private final boolean skipValidation;

    public JwtAuthFilter(JwtProperties jwtProperties) {
        this.skipValidation = jwtProperties.isSkipValidation();
        this.maxTtlMinutes = jwtProperties.getMaxTtlMinutes();
        if (skipValidation) {
            this.publicKey = null;
            log.warn("⚠️  JWT validation is DISABLED (skip-validation=true). This should only be used in development!");
        } else {
            this.publicKey = parsePublicKey(jwtProperties.getPublicKey());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // In development mode, skip JWT validation and set a default dev authentication
        if (skipValidation) {
            UUID devTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID devUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            TenantAuthenticationToken devAuth = new TenantAuthenticationToken(devUserId, devTenantId, "ADMIN_CUENTA");
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
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Validate TTL: reject tokens with expiration too far in the future
            validateTtl(claims);

            // Extract claims
            UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String rol = claims.get("rol", String.class);

            TenantAuthenticationToken authentication =
                    new TenantAuthenticationToken(userId, tenantId, rol);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Token expired\"}");
            response.setContentType("application/json");
            return;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid token\"}");
            response.setContentType("application/json");
            return;
        }

        filterChain.doFilter(request, response);
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
