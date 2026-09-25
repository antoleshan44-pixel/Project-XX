package com.urbano.monolith.auth;

import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.auth.dto.AuthRequest;
import com.urbano.monolith.auth.dto.RegisterRequest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("New User")
                .email("newuser@urbano.com")
                .password("Password123!")
                .phone("+254700112233")
                .companyName("Urbano Real Estate")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.email", is("newuser@urbano.com")));
    }

    @Test
    void testLoginSuccess() throws Exception {
        User user = createTestTenantUser("tenant1@urbano.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        userRepository.save(user);

        AuthRequest request = AuthRequest.builder()
                .email("tenant1@urbano.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.email", is("tenant1@urbano.com")));
    }

    @Test
    void testTenantLoginSecondary() throws Exception {
        User user = createTestTenantUser("tenant2@urbano.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        userRepository.save(user);

        AuthRequest request = AuthRequest.builder()
                .email("tenant2@urbano.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("tenant2@urbano.com")));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .email("nonexistent@urbano.com")
                .password("WrongPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCurrentUserAuthenticated() throws Exception {
        User user = createTestPmUser("me@urbano.com", UUID.randomUUID());
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        User savedUser = userRepository.save(user);

        String token = createBearerToken(savedUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("me@urbano.com")))
                .andExpect(jsonPath("$.role", is("PM_ADMIN")));
    }

    @Test
    void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetFirebaseTokenWhenUnconfigured() throws Exception {
        User user = createTestPmUser("firebase@urbano.com", UUID.randomUUID());
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        User savedUser = userRepository.save(user);

        String token = createBearerToken(savedUser);

        mockMvc.perform(post("/api/auth/firebase-token")
                        .header("Authorization", token))
                .andExpect(status().isServiceUnavailable());
    }
}
