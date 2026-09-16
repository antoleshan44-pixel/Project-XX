package com.urbano.monolith.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoUploadUrlResponse {
    private String uploadUrl;
    private String key;
    private String publicUrl;
    private Long expiresIn;
}