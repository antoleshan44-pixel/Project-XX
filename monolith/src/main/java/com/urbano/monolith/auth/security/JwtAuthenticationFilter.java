package com.urbano.monolith.auth.security;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.UserRole;
import com.urbano.common.security.JwtClaims;
import com.urbano.monolith.auth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    private final TokenBlacklistService tokenBlacklistService;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT] No Bearer token for {} {} — passing through unauthenticated", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.debug("[JWT] Authenticating {} {} with Bearer token ({} chars)",
                method, path, token.length());

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("[JWT] Parsed OK: jti={}, sub={}, iat={}, exp={}, type={}",
                    claims.getId(),
                    claims.getSubject(),
                    claims.getIssuedAt(),
                    claims.getExpiration(),
                    claims.get("type", String.class));

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                log.debug("[JWT] REJECT: token type is '{}' (expected 'access')", type);
                filterChain.doFilter(request, response);
                return;
            }

            // ---- C2 fix: consult blacklist + user epoch ----
            log.debug("[JWT] Checking revocation for jti={}", claims.getId());
            boolean revoked = tokenBlacklistService.isTokenRevoked(token);
            log.debug("[JWT] Revocation check result for jti={}: {}", claims.getId(), revoked);
            if (revoked) {
                log.debug("[JWT] REJECT: token is revoked (blacklisted or user-epoch mismatch)");
                filterChain.doFilter(request, response);
                return;
            }

            String userIdStr = claims.get("userId", String.class);
            String roleStr = claims.get("role", String.class);
            String pmAccountIdStr = claims.get("pmAccountId", String.class);
            String email = claims.getSubject();

            log.debug("[JWT] Claims: userId={}, role={}, pmAccountId={}, email={}",
                    userIdStr, roleStr, pmAccountIdStr, email);

            if (userIdStr == null || roleStr == null) {
                log.debug("[JWT] REJECT: missing userId or role claim");
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(userIdStr);
            UserRole role = UserRole.valueOf(roleStr);
            UUID pmAccountId = pmAccountIdStr != null ? UUID.fromString(pmAccountIdStr) : null;

            JwtClaims jwtClaims = new JwtClaims(userId, role, pmAccountId, email, claims.getId());

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authentication = new UsernamePasswordAuthenticationToken(jwtClaims, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            TenantContext.setUserId(userId);
            TenantContext.setUserRole(role);
            if (pmAccountId != null) {
                TenantContext.setPmAccountId(pmAccountId);
            }
            if (role == UserRole.SUPER_ADMIN) {
                TenantContext.setAdminBypass(true);
            }

            log.debug("[JWT] Authentication set — TenantContext pmAccountId={}, userId={}, role={}",
                    TenantContext.getPmAccountId(),
                    TenantContext.getUserId(),
                    TenantContext.getUserRole());

            filterChain.doFilter(request, response);

            log.debug("[JWT] Request completed for {} {}", method, path);
        } catch (Exception ex) {
            log.debug("[JWT] EXCEPTION during validation: {} — {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}