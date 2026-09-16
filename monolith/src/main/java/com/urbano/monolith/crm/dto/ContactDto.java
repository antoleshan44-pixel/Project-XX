package com.urbano.monolith.crm.dto;

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
public class ContactDto {
    private UUID id;
    private UUID pmAccountId;  // ✅ Added
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String mobile;
    private String type;
    private String company;
    private String position;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String preferredContactMethod;
    private String notes;
    private UUID assignedTo;
    private boolean isActive;
    private LocalDateTime lastContactDate;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}