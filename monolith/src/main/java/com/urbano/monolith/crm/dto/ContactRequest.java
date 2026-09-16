package com.urbano.monolith.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    @NotNull(message = "PM Account ID is required")
    private UUID pmAccountId;  // ✅ Added

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String mobile;

    @NotBlank(message = "Contact type is required")
    private String type; // TENANT, PROPERTY_OWNER, VENDOR, EMPLOYEE, PROSPECT, LANDLORD

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

    private String source;
}