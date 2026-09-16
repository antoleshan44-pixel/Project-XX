package com.urbano.monolith.maintenance.service;

import com.urbano.common.storage.PhotoStorageService;
import com.urbano.monolith.maintenance.dto.PhotoUploadUrlResponse;
import com.urbano.monolith.maintenance.entity.MaintenanceRequest;
import com.urbano.monolith.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service("maintenancePhotoService")
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoStorageService photoStorageService;
    private final MaintenanceRepository maintenanceRepository;

    
    public PhotoUploadUrlResponse getPhotoUploadUrl(UUID requestId, UUID pmAccountId, String fileName) {
        // Verify the request exists and belongs to the PM account
        MaintenanceRequest request = maintenanceRepository.findByIdAndPmAccountId(requestId, pmAccountId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found"));

        // Generate unique key for the photo
        String key = "maintenance/" + requestId + "/" + UUID.randomUUID() + "_" + fileName;

        // Generate presigned URL for uploading
        String uploadUrl = photoStorageService.generatePresignedUrl(key);

        // Generate public URL for accessing the photo
        String publicUrl = photoStorageService.generatePublicUrl(key);

        log.info("Generated photo upload URL for maintenance request: {}", requestId);

        return PhotoUploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .key(key)
                .publicUrl(publicUrl)
                .expiresIn(300L)  // 5 minutes
                .fileId(UUID.randomUUID().toString())
                .build();
    }

    public String uploadPhoto(String maintenanceId, String fileName, byte[] content) {
        String key = "maintenance/" + maintenanceId + "/" + fileName;
        return photoStorageService.uploadFile(key, content, "image/jpeg");
    }

    public byte[] downloadPhoto(String photoUrl) {
        // In a real implementation, this would download from S3/R2
        log.info("Downloading photo from URL: {}", photoUrl);
        return new byte[0];
    }

    public void deletePhoto(String photoUrl) {
        log.info("Photo deleted: {}", photoUrl);
        // TODO: Implement actual deletion from storage
    }
}