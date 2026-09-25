package com.urbano.monolith.property;

import com.urbano.common.enums.PropertyStatus;
import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.property.dto.PropertyRequest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PropertyControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreatePropertyAsPropertyManager() throws Exception {
        UUID pmAccountId = UUID.randomUUID();
        User pmUser = createTestPmUser("pm@urbano.com", pmAccountId);
        User savedPm = userRepository.save(pmUser);

        PropertyRequest request = PropertyRequest.builder()
                .name("Greenwood Heights")
                .address("45 Westlands Road")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(20)
                .build();

        String token = createBearerToken(savedPm);

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Greenwood Heights")));
    }

    @Test
    void testGetPropertiesScopedToPmAccount() throws Exception {
        UUID pmAccountId = UUID.randomUUID();
        User pmUser = createTestPmUser("pm2@urbano.com", pmAccountId);
        User savedPm = userRepository.save(pmUser);

        Property prop1 = Property.builder()
                .pmAccountId(pmAccountId)
                .ownerId(savedPm.getId())
                .name("PM Property 1")
                .address("Address 1")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(5)
                .status(PropertyStatus.AVAILABLE)
                .build();
        propertyRepository.save(prop1);

        Property prop2 = Property.builder()
                .pmAccountId(UUID.randomUUID()) // Other PM
                .ownerId(UUID.randomUUID())
                .name("Other PM Property")
                .address("Address 2")
                .city("Mombasa")
                .type("RESIDENTIAL")
                .totalUnits(10)
                .status(PropertyStatus.AVAILABLE)
                .build();
        propertyRepository.save(prop2);

        String token = createBearerToken(savedPm);

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("PM Property 1")));
    }

    @Test
    void testGetPropertiesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isUnauthorized());
    }
}
