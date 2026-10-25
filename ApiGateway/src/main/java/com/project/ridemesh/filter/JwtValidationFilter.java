package com.project.ridemesh.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);

    @Value("${jwt.secretKey}")
    private String secretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Exclude public endpoints from JWT validation
        if (path.equals("/auth/login") || 
            path.equals("/auth/signup") || 
            path.equals("/auth/google") || 
            path.startsWith("/auth/refresh") || 
            path.startsWith("/auth/check-username") || 
            path.startsWith("/auth/verify-email") || 
            path.startsWith("/webhooks/razorpay") || 
            path.contains("/v3/api-docs") || 
            path.contains("/swagger-ui")) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: " + path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);

            // Parse claims from token
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.valueOf(claims.getSubject());
            Object roleObj = claims.get("role");
            if (roleObj == null) {
                roleObj = claims.get("roles");
            }
            String roleClaim = roleObj != null ? roleObj.toString() : "";

            // Clean brackets and spaces: "[RIDER, DRIVER]" -> "RIDER, DRIVER"
            String cleanedRoles = roleClaim.replace("[", "").replace("]", "").trim();
            String xUserRoles = Arrays.stream(cleanedRoles.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .collect(Collectors.joining(","));

            // 3. Mutate the request headers to forward identity downstream
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Roles", xUserRoles)
                    .build();

            log.info("Successfully validated JWT at edge. Forwarding X-User-Id: " + userId + " Roles: " + xUserRoles);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed: " + e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Execute early in the filter chain
    }
}
