package com.mikelcrm.licenseservice.security;

import com.mikelcrm.licenseservice.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private static KeyPair keyPair;
    private static String publicKeyBase64;

    private JwtAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        JwtProperties properties = new JwtProperties();
        properties.setPublicKey(publicKeyBase64);
        properties.setMaxTtlMinutes(15);

        filter = new JwtAuthFilter(properties);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String rol = "ADMIN_CUENTA";

        String token = buildValidToken(tenantId, userId, rol, 15);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth).isInstanceOf(TenantAuthenticationToken.class);

        TenantAuthenticationToken tenantAuth = (TenantAuthenticationToken) auth;
        assertThat(tenantAuth.getTenantId()).isEqualTo(tenantId);
        assertThat(tenantAuth.getUserId()).isEqualTo(userId);
        assertThat(tenantAuth.getRol()).isEqualTo(rol);
        assertThat(tenantAuth.getPrincipal()).isEqualTo(userId);
        assertThat(tenantAuth.getCredentials()).isNull();
        assertThat(tenantAuth.isAuthenticated()).isTrue();
    }

    @Test
    void noAuthorizationHeader_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerToken_continuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredToken_returns401() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Instant issuedAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant expiration = Instant.now().minus(15, ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("rol", "TECNICO")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(keyPair.getPrivate())
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenWithTtlExceedingMax_returns401() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Token with 60-minute TTL (exceeds 15-minute max)
        String token = buildValidToken(tenantId, userId, "SUPERVISOR", 60);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void invalidSignature_returns401() throws Exception {
        // Sign with a different key
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        String token = Jwts.builder()
                .claim("tenantId", UUID.randomUUID().toString())
                .claim("userId", UUID.randomUUID().toString())
                .claim("rol", "TECNICO")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                .signWith(otherKeyPair.getPrivate())
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validToken_setsRoleAuthority() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String rol = "SUPERVISOR";

        String token = buildValidToken(tenantId, userId, rol, 15);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SUPERVISOR");
    }

    private String buildValidToken(UUID tenantId, UUID userId, String rol, long ttlMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("tenantId", tenantId.toString())
                .claim("userId", userId.toString())
                .claim("rol", rol)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(keyPair.getPrivate())
                .compact();
    }
}
