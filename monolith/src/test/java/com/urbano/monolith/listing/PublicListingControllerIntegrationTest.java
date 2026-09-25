package com.urbano.monolith.listing;

import com.urbano.common.enums.PropertyStatus;
import com.urbano.common.enums.PropertyType;
import com.urbano.common.enums.TransactionType;
import com.urbano.common.enums.UnitStatus;
import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.property.entity.Property;
import com.urbano.monolith.property.entity.Unit;
import com.urbano.monolith.property.repository.PropertyRepository;
import com.urbano.monolith.property.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PublicListingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UnitRepository unitRepository;

    @BeforeEach
    void setUp() {
        unitRepository.deleteAll();
        propertyRepository.deleteAll();
    }

    @Test
    void testGetPublicListings() throws Exception {
        Property property = Property.builder()
                .pmAccountId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Sunset Apartments")
                .address("123 Kilimani Road")
                .city("Nairobi")
                .type("RESIDENTIAL")
                .totalUnits(10)
                .status(PropertyStatus.AVAILABLE)
                .build();
        Property savedProp = propertyRepository.save(property);

        Unit unit = Unit.builder()
                .property(savedProp)
                .unitNumber("A101")
                .floor(1)
                .bedrooms(2)
                .bathrooms(1)
                .rentAmount(45000.0)
                .currency("KES")
                .squareFootage(85.5)
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .propertyType(PropertyType.APARTMENT)
                .transactionType(TransactionType.FOR_RENT)
                .published(true)
                .description("Test unit")
                .build();
        unitRepository.save(unit);

        mockMvc.perform(get("/api/public/listings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Sunset Apartments - A101")))
                .andExpect(jsonPath("$.content[0].price", is(45000.0)));
    }

    @Test
    void testGetPublicListingById() throws Exception {
        Property property = Property.builder()
                .pmAccountId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Kilimani Towers")
                .address("Argwings Kodhek")
                .city("Nairobi")
                .type("STUDIO")
                .totalUnits(5)
                .status(PropertyStatus.AVAILABLE)
                .build();
        Property savedProp = propertyRepository.save(property);

        Unit unit = Unit.builder()
                .property(savedProp)
                .unitNumber("S202")
                .floor(2)
                .bedrooms(1)
                .bathrooms(1)
                .rentAmount(35000.0)
                .currency("KES")
                .squareFootage(45.0)
                .isAvailable(true)
                .status(UnitStatus.AVAILABLE)
                .propertyType(PropertyType.STUDIO)
                .transactionType(TransactionType.FOR_RENT)
                .published(true)
                .description("Cozy Studio")
                .build();
        Unit savedUnit = unitRepository.save(unit);

        mockMvc.perform(get("/api/public/listings/{id}", savedUnit.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedUnit.getId().toString())))
                .andExpect(jsonPath("$.title", is("Kilimani Towers - S202")))
                .andExpect(jsonPath("$.price", is(35000.0)));
    }

    @Test
    void testGetPublicListingNotFound() throws Exception {
        mockMvc.perform(get("/api/public/listings/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
