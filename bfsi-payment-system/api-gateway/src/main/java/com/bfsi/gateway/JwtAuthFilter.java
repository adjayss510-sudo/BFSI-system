package com.bfsi.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the JWT once at the edge and forwards the caller's identity to downstream
 * services as trusted headers. Any client-supplied identity headers are stripped first.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/login", "/api/auth/register");
    private static final List<String> TRUSTED = List.of("X-User-Id", "X-User-Role", "X-User-Email", "X-Internal-Secret");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        if (req.getMethod() == HttpMethod.OPTIONS) return chain.filter(exchange);

        String path = req.getURI().getPath();
        if (PUBLIC_PATHS.contains(path)) {
            ServerHttpRequest clean = req.mutate().headers(h -> TRUSTED.forEach(h::remove)).build();
            return chain.filter(exchange.mutate().request(clean).build());
        }

        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) return unauthorized(exchange);
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(auth.substring(7)).getPayload();
            ServerHttpRequest r = req.mutate().headers(h -> {
                TRUSTED.forEach(h::remove);
                h.add("X-User-Id", c.getSubject());
                h.add("X-User-Role", c.get("role", String.class));
                h.add("X-User-Email", c.get("email", String.class));
            }).build();
            return chain.filter(exchange.mutate().request(r).build());
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange ex) {
        ex.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return ex.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -1; }
}
