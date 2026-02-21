package com.lazycord.security;

import com.lazycord.model.User;
import com.lazycord.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtDecoder jwtDecoder;
    private final UserService userService;

    public JwtAuthFilter(JwtDecoder jwtDecoder, UserService userService) {
        this.jwtDecoder = jwtDecoder;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                
                // Extract user information from token
                String keycloakId = jwt.getSubject();
                String username = jwt.getClaimAsString("preferred_username");
                String email = jwt.getClaimAsString("email");
                
                // Extract roles from realm_access
                @SuppressWarnings("unchecked")
                Map<String, List<String>> realmAccess = jwt.getClaim("realm_access");
                List<String> roles = realmAccess != null ? realmAccess.get("roles") : List.of();
                
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                
                // Sync user with local database
                Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userService.updateLastActive(user);
                    
                    // Create authentication with user details
                    UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                            .username(user.getUsername())
                            .password("")
                            .authorities(authorities)
                            .build();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authentication.setDetails(user);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("Authenticated user: {} with roles: {}", username, roles);
                } else {
                    // User not in local DB yet, create minimal authentication
                    UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                            .username(username != null ? username : keycloakId)
                            .password("")
                            .authorities(authorities)
                            .build();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("Authenticated user without local profile: {}", username);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to validate JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for public endpoints
        return path.startsWith("/api/auth/") || 
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/actuator/health");
    }
}
