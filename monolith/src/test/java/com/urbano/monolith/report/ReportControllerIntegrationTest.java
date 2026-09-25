package com.urbano.monolith.report;

import com.urbano.monolith.BaseIntegrationTest;
import com.urbano.monolith.auth.entity.User;
import com.urbano.monolith.auth.repository.UserRepository;
import com.urbano.monolith.report.dto.ReportRequest;
import com.urbano.monolith.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReportControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    private User pmUser;
    private UUID pmAccountId;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        userRepository.deleteAll();

        pmAccountId = UUID.randomUUID();
        pmUser = createTestPmUser("pmreports@urbano.com", pmAccountId);
        userRepository.save(pmUser);
    }

    @Test
    void testGenerateReportSuccess() throws Exception {
        ReportRequest request = ReportRequest.builder()
                .name("Monthly Financial Summary")
                .type("FINANCIAL")
                .format("CSV")
                .description("September 2026 Financial Report")
                .build();

        String token = createBearerToken(pmUser);

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testGetReportStatistics() throws Exception {
        String token = createBearerToken(pmUser);

        mockMvc.perform(get("/api/reports/stats")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports", notNullValue()));
    }
}
