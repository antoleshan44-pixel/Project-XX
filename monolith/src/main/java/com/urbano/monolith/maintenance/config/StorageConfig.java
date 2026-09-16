package com.urbano.monolith.maintenance.config;

import com.urbano.common.storage.PhotoStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    @Bean
    @Primary
    public PhotoStorageService photoStorageService() {
        return new PhotoStorageService();
    }
}