package com.urbano.monolith.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Commit 4: Firebase Admin SDK initialization.
 *
 * <p>The service account JSON is supplied as a base64-encoded env var
 * ({@code FIREBASE_SERVICE_ACCOUNT_B64}) so it fits in a single-line
 * configuration value on Render/Docker without newline mangling.</p>
 *
 * <p>Boot behavior:</p>
 * <ul>
 *   <li>Env var absent → WARN, Firebase disabled. The rest of the app
 *       boots normally. {@code POST /api/auth/firebase-token} returns 503.</li>
 *   <li>Env var present but malformed → boot fails fast. A "successfully
 *       booted but every login fails silently" state is worse than a
 *       clean crash at deploy time.</li>
 * </ul>
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-base64:}")
    private String serviceAccountBase64;

    @PostConstruct
    public void init() {
        if (serviceAccountBase64 == null || serviceAccountBase64.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_B64 is not set — Firebase integration disabled. " +
                    "POST /api/auth/firebase-token will return 503 until this is configured.");
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64.trim());
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(decoded));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            } else {
                log.info("Firebase Admin SDK already initialized — skipping re-init");
            }
        } catch (IllegalArgumentException e) {
            log.error("FIREBASE_SERVICE_ACCOUNT_B64 is not valid base64: {}", e.getMessage());
            throw new IllegalStateException("Invalid Firebase service account encoding", e);
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            throw new IllegalStateException("Firebase initialization failed", e);
        }
    }

    /** Returns true if Firebase was successfully initialized at boot. */
    public boolean isEnabled() {
        return !FirebaseApp.getApps().isEmpty();
    }
}