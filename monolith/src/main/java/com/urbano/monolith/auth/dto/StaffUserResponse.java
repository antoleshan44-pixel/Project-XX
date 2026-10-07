package com.urbano.monolith.auth.dto;

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
public class StaffUserResponse {
    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private String status;
    private boolean isActive;
    private LocalDateTime createdAt;
}