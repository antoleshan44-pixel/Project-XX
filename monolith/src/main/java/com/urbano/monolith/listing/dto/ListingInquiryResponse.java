package com.urbano.monolith.listing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingInquiryResponse {
    private UUID inquiryId;
    private String status;
    private String message;
    private String propertyManagerName;
    private String propertyManagerPhone;
    private String propertyManagerEmail;
    private LocalDateTime submittedAt;
}