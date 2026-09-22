package com.urbano.monolith.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/auth/firebase-token.
 * The client exchanges {@code customToken} with Firebase Auth
 * (signInWithCustomToken) to obtain a real Firebase session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseTokenResponse {
    private String customToken;
    /** Approximate TTL in seconds (Firebase default: 3600). */
    private int expiresIn;
}