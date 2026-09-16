package com.urbano.monolith.property.dto;

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
public class PropertyResponse {
    private UUID id;
    private String name;
    private String location;
    private String description;
    private UUID pmAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}