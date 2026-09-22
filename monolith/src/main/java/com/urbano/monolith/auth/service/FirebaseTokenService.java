package com.urbano.monolith.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.urbano.common.exception.ServiceUnavailableException;
import com.urbano.monolith.auth.config.FirebaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Commit 4: mints short-lived Firebase custom tokens for authenticated users.
 *
 * <p>The token's {@code uid} is the raw {@code auth_users.id} UUID (string form).
 * The mobile app exchanges the custom token for a Firebase ID token via
 * {@code signInWithCustomToken()}, which then populates {@code request.auth.uid}
 * in Firestore — enabling real per-user security rules instead of the
 * test-mode rules that expire 2026-10-17.</p>
 *
 * <p>If Firebase is not configured, {@link #mintCustomToken(UUID)} throws
 * {@link ServiceUnavailableException} → the controller returns 503.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseTokenService {

    private static final int CUSTOM_TOKEN_TTL_SECONDS = 3600; // Firebase default

    private final FirebaseConfig firebaseConfig;

    /**
     * @return a Firebase custom token (JWT signed by Firebase) for the given user.
     * @throws ServiceUnavailableException if Firebase is not configured.
     * @throws ServiceUnavailableException if Firebase rejects the mint (rare).
     */
    public String mintCustomToken(UUID userId) {
        if (!firebaseConfig.isEnabled()) {
            throw new ServiceUnavailableException(
                    "Firebase integration is not configured on this server");
        }

        try {
            return FirebaseAuth.getInstance().createCustomToken(userId.toString());
        } catch (FirebaseAuthException e) {
            log.error("Firebase custom token mint failed for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException(
                    "Could not mint Firebase token at this time", e);
        }
    }

    public int getCustomTokenTtlSeconds() {
        return CUSTOM_TOKEN_TTL_SECONDS;
    }
}