package com.urbano.monolith.auth.service;

import com.urbano.monolith.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiry:900}")
    private long accessExpiry;

    @Value("${jwt.refresh-expiry:604800}")
    private long refreshExpiry;

    @Value("${jwt.issuer:urbano-homes}")
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ============================================================
    // ACCESS TOKEN
    // ============================================================
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("type", "access");
        if (user.getPmAccountId() != null) {
            claims.put("pmAccountId", user.getPmAccountId().toString());
        }
        return createToken(claims, user.getEmail(), accessExpiry);
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", user.getId().toString());
        return createToken(claims, user.getEmail(), refreshExpiry);
    }

    // ============================================================
    // CORE BUILDER — always sets a jti, always issues an iat
    // ============================================================
    private String createToken(Map<String, Object> claims, String subject, long expirySeconds) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())              // <- jti (the whole point of this commit)
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirySeconds * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    // ============================================================
    // EXTRACTORS
    // ============================================================
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return userId != null ? UUID.fromString(userId) : null;
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public UUID extractPmAccountId(String token) {
        String pmId = extractClaim(token, claims -> claims.get("pmAccountId", String.class));
        return pmId != null ? UUID.fromString(pmId) : null;
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}