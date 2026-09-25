package com.urbano.monolith.property;

import com.urbano.common.enums.PropertyStatus;
import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
import com.urbano.common.enums.ViewingStatus;
import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.property.dto.ViewingRequest;
import com.urbano.monolith.property.dto.ViewingStatusUpdateRequest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.entity.Viewing;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import com.urbano.monolith.property.repository.ViewingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ViewingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ViewingRepository viewingRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private Property testProperty;
    private Unit testUnit;
    private User pmUser;
    private UUID pmAccountId;

    @BeforeEach
    void setUp() {
        viewingRepository.deleteAll();
        unitRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        pmAccountId = UUID.randomUUID();
        pmUser = createTestPmUser("pmviewings@urbano.com", pmAccountId);
        userRepository.save(pmUser);

        testProperty = Property.builder()
                .pmAccountId(pmAccountId)
                .ownerId(pmUser.getId())
                .name("Viewing Tower")
                .address("Parklands Road")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(5)
                .status(PropertyStatus.AVAILABLE)
                .build();
        propertyRepository.save(testProperty);

        testUnit = Unit.builder()
                .property(testProperty)
                .unitNumber("V101")
                .floor(1)
                .bedrooms(2)
                .bathrooms(1)
                .rentAmount(50000.0)
                .currency("KES")
                .squareFootage(75.0)
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .propertyType(PropertyType.APARTMENT)
                .transactionType(TransactionType.FOR_RENT)
                .published(true)
                .build();
        unitRepository.save(testUnit);
    }

    @Test
    void testCreatePublicViewingSuccess() throws Exception {
        ViewingRequest request = ViewingRequest.builder()
                .requestedByName("Jane Prospect")
                .requestedByPhone("+254711223344")
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .notes("Interested in a weekend viewing")
                .build();

        mockMvc.perform(post("/api/units/{unitId}/viewings", testUnit.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.requestedByName", is("Jane Prospect")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    void testGetViewingsAsPropertyManager() throws Exception {
        Viewing viewing = Viewing.builder()
                .pmAccountId(pmAccountId)
                .unitId(testUnit.getId())
                .propertyId(testProperty.getId())
                .requestedByName("John Prospect")
                .requestedByPhone("+254722334455")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(ViewingStatus.SCHEDULED)
                .build();
        viewingRepository.save(viewing);

        String token = createBearerToken(pmUser);

        mockMvc.perform(get("/api/viewings")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].requestedByName", is("John Prospect")));
    }

    @Test
    void testUpdateViewingStatusAsPropertyManager() throws Exception {
        Viewing viewing = Viewing.builder()
                .pmAccountId(pmAccountId)
                .unitId(testUnit.getId())
                .propertyId(testProperty.getId())
                .requestedByName("Alice Prospect")
                .requestedByPhone("+254733445566")
                .scheduledAt(LocalDateTime.now().plusDays(3))
                .status(ViewingStatus.SCHEDULED)
                .build();
        Viewing savedViewing = viewingRepository.save(viewing);

        ViewingStatusUpdateRequest updateRequest = ViewingStatusUpdateRequest.builder()
                .status(ViewingStatus.COMPLETED)
                .notes("Viewing completed successfully")
                .build();

        String token = createBearerToken(pmUser);

        mockMvc.perform(put("/api/viewings/{id}/status", savedViewing.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }
}
