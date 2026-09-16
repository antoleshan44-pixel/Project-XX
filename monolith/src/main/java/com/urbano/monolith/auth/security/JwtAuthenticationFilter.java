package com.urbano.monolith.auth.security;

import com.urbano.common.context.TenantContext;
import com.urbano.common.enums.UserRole;
import com.urbano.common.security.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:default-secret-change-in-production}")
    private String secret;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                filterChain.doFilter(request, response);
                return;
            }

            String userIdStr = claims.get("userId", String.class);
            String roleStr = claims.get("role", String.class);
            String pmAccountIdStr = claims.get("pmAccountId", String.class);
            String email = claims.getSubject();

            if (userIdStr == null || roleStr == null) {
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

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}