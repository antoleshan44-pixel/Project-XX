package com.urbano.monolith.config;

import com.urbano.monolith.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final Environment env;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .exceptionHandling(e -> e
                                .authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                        .authorizeHttpRequests(auth -> auth
                                // Root
                                .requestMatchers("/").permitAll()

                                // Public infrastructure
                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")

                                // Auth flows
                                .requestMatchers(
                                        "/api/auth/register",
                                        "/api/auth/register/verify-phone",
                                        "/api/auth/register/confirm-phone",
                                        "/api/auth/login",
                                        "/api/auth/refresh",
                                        "/api/auth/password/forgot",
                                        "/api/auth/password/reset",
                                        "/api/auth/tenant/register",
                                        "/api/auth/tenant/activate",
                                        "/api/auth/tenant/verify/**")
                                .permitAll()

                                // Public portal (listings, contact, newsletter, blog)
                                .requestMatchers("/api/public/**").permitAll()

                                // Public viewing request
                                .requestMatchers(HttpMethod.POST, "/api/units/*/viewings").permitAll()

                                // Payment gateway webhooks
                                .requestMatchers("/api/callbacks/**").permitAll()

                                // WebSocket handshake
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers("/webjars/**").permitAll()

                                // Swagger / OpenAPI
                                .requestMatchers(
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**")
                                .permitAll()

                                // SUPER_ADMIN — platform-wide
                                .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")

                                // PM dashboard — @PreAuthorize also enforces role
                                .requestMatchers("/api/dashboard/**")
                                .hasAnyRole("PM_ADMIN", "PM_STAFF")

                                // Everything else authenticated
                                .anyRequest().authenticated())
                        .httpBasic(AbstractHttpConfigurer::disable)
                        .formLogin(AbstractHttpConfigurer::disable)
                        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");

                boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod")
                        || "prod".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"));

                if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
                        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                        log.info("CORS allowed origins (from env): {}", origins);
                        cfg.setAllowedOrigins(origins);
                } else if (isProd) {
                        throw new IllegalStateException(
                                "CORS_ALLOWED_ORIGINS must be set when running with the 'prod' profile. " +
                                        "Refusing to start with wildcard CORS in production.");
                } else {
                        log.warn("CORS_ALLOWED_ORIGINS not set — dev default " +
                                "(localhost/127.0.0.1) in use. DO NOT run this in production.");
                        cfg.setAllowedOriginPatterns(List.of(
                                "http://localhost:*",
                                "http://127.0.0.1:*"));
                }

                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("*"));
                cfg.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));
                cfg.setAllowCredentials(true);
                cfg.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }
}