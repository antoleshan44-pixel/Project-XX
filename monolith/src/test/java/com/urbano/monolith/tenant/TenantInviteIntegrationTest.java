package com.urbano.monolith.tenant;

import com.urbano.common.enums.InviteStatus;
import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.tenant.dto.TenantInviteRequest;
import com.urbano.monolith.tenant.entity.Tenant;
import com.urbano.monolith.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TenantInviteIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    private User pmUser;
    private UUID pmAccountId;

    @BeforeEach
    void setUp() {
        tenantRepository.deleteAll();
        userRepository.deleteAll();

        pmAccountId = UUID.randomUUID();
        pmUser = createTestPmUser("pminvite@urbano.com", pmAccountId);
        userRepository.save(pmUser);
    }

    @Test
    void testInviteTenantSuccess() throws Exception {
        TenantInviteRequest request = TenantInviteRequest.builder()
                .fullName("Invited Tenant")
                .email("invited@urbano.com")
                .phone("+254711889900")
                .build();

        String token = createBearerToken(pmUser);

        mockMvc.perform(post("/api/tenants/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", notNullValue()))
                .andExpect(jsonPath("$.pmAccountId", is(pmAccountId.toString())))
                .andExpect(jsonPath("$.fullName", is("Invited Tenant")))
                .andExpect(jsonPath("$.email", is("invited@urbano.com")))
                .andExpect(jsonPath("$.phone", is("+254711889900")))
                // The 6-digit invite code is intentionally NOT in the response — it
                // goes out via SMS only, so an attacker who intercepts the HTTP
                // response can't steal the invite.
                .andExpect(jsonPath("$.code").doesNotExist())
                // Field is inviteStatus, not status.
                .andExpect(jsonPath("$.inviteStatus", is("PENDING")))
                .andExpect(jsonPath("$.invitedAt", notNullValue()));

        // Verify the tenant row was persisted with the right state
        List<Tenant> tenants = tenantRepository.findAll();
        assertEquals(1, tenants.size());
        Tenant saved = tenants.get(0);
        assertEquals(InviteStatus.PENDING, saved.getInviteStatus());
        assertNull(saved.getUserId(), "user_id must not be set until activation");
        assertFalse(saved.getIsActive(), "is_active must be false until activation");
    }

    @Test
    void testInviteTenantWithoutAuth_returns401() throws Exception {
        TenantInviteRequest request = TenantInviteRequest.builder()
                .fullName("Ghost")
                .email("ghost@urbano.com")
                .phone("+254711000000")
                .build();

        mockMvc.perform(post("/api/tenants/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInviteTenantDuplicatePhone_returns409() throws Exception {
        String token = createBearerToken(pmUser);

        TenantInviteRequest first = TenantInviteRequest.builder()
                .fullName("First")
                .email("first@urbano.com")
                .phone("+254700111222")
                .build();

        mockMvc.perform(post("/api/tenants/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        TenantInviteRequest dup = TenantInviteRequest.builder()
                .fullName("Second")
                .email("second@urbano.com")
                .phone("+254700111222")
                .build();

        mockMvc.perform(post("/api/tenants/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }
}