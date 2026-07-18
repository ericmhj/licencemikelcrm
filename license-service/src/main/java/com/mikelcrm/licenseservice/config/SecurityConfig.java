package com.mikelcrm.licenseservice.config;

import com.mikelcrm.licenseservice.payment.PaymentProperties;
import com.mikelcrm.licenseservice.security.GatewayHeaderAuthFilter;
import com.mikelcrm.licenseservice.security.JwtAuthFilter;
import com.mikelcrm.licenseservice.security.RateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class, PaymentProperties.class})
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final RateLimitProperties rateLimitProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public SecurityConfig(JwtProperties jwtProperties,
                          RateLimitProperties rateLimitProperties,
                          StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/payments/**").permitAll()
                        .requestMatchers("/api/v1/tenants").permitAll()
                        .requestMatchers("/api/v1/tenants/*/apertura").permitAll()
                        .requestMatchers("/api/v1/tenants/*/contracts").permitAll()
                        .requestMatchers("/api/v1/tenants/*/credits/packages").permitAll()
                        .requestMatchers("/api/v1/notifications/**").permitAll()
                        .requestMatchers("/api/v1/plans").permitAll()
                        .requestMatchers("/api/v1/plans/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter(), JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public GatewayHeaderAuthFilter gatewayHeaderAuthFilter() {
        return new GatewayHeaderAuthFilter(jwtProperties.isTrustGatewayHeaders());
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtProperties);
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(stringRedisTemplate, rateLimitProperties);
    }
}
