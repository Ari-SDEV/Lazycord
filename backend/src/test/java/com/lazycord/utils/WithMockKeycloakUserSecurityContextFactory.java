package com.lazycord.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for creating a SecurityContext with a mock Keycloak user.
 * This is used by the @WithMockKeycloakUser annotation.
 */
public class WithMockKeycloakUserSecurityContextFactory 
        implements WithSecurityContextFactory<WithMockKeycloakUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockKeycloakUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Convert roles to GrantedAuthorities
        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Create UserDetails
        UserDetails userDetails = User.builder()
                .username(annotation.username())
                .password("")
                .authorities(authorities)
                .disabled(!annotation.enabled())
                .build();

        // Create authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities
        );

        context.setAuthentication(authentication);
        return context;
    }
}
