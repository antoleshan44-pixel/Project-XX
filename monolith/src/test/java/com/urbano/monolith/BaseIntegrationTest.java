package com.urbano.monolith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbano.common.enums.UserRole;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @MockitoBean
    protected CacheManager cacheManager;

    @MockitoBean(name = "stringRedisTemplate")
    protected StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    protected RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpBase() {
        NoOpCacheManager noOpCacheManager = new NoOpCacheManager();
        Mockito.when(cacheManager.getCache(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> noOpCacheManager.getCache(invocation.getArgument(0, String.class)));

        Mockito.when(stringRedisTemplate.hasKey(ArgumentMatchers.anyString()))
                .thenReturn(Boolean.FALSE);

        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get(ArgumentMatchers.anyString())).thenReturn(null);
    }

    protected User createTestUser(String email, UserRole role, UUID pmAccountId) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$10$e8wSg7Q8uF4F7v1x2y3z4u5v6w7x8y9z0a1b2c3d4e5f6g7h8i9j")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .pmAccountId(pmAccountId)
                .build();
    }

    protected User createTestPmUser(String email, UUID pmAccountId) {
        return createTestUser(email, UserRole.PM_ADMIN, pmAccountId);
    }

    protected User createTestTenantUser(String email) {
        return createTestUser(email, UserRole.TENANT, null);
    }

    protected String createBearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
