package com.lobosoft.api.config;

import com.lobosoft.api.auth.AuthGrpcClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthGrpcClient authGrpcClient;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/") || path.startsWith("/actuator/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) {

        String path = request.getRequestURI();

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing/invalid Authorization header");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            String userId = authGrpcClient.validateTokenAndGetUserId(token);
            log.debug("Authenticated userId={} for path={}", userId, path);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (ResponseStatusException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.UNAUTHORIZED;
            }

            if (status.is5xxServerError()) {
                log.error("Auth error for path={}: {}", path, ex.getReason(), ex);
            } else {
                log.warn("Auth failed for path={}: {}", path, ex.getReason());
            }

            response.setStatus(status.value());
        } catch (Exception ex) {
            log.error("Auth failed for path={}", path, ex);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
