package com.bfsi.customer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Protects /internal/** endpoints: only callers holding the shared service secret may use them. */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {
    @Value("${internal.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/internal/")) {
            String h = req.getHeader("X-Internal-Secret");
            boolean ok = h != null && MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), h.getBytes(StandardCharsets.UTF_8));
            if (!ok) { res.sendError(HttpServletResponse.SC_FORBIDDEN, "Internal endpoint"); return; }
        }
        chain.doFilter(req, res);
    }
}
