package com.atherion.andromeda.security;

import com.atherion.andromeda.services.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthUserSyncFilter extends OncePerRequestFilter {

    private final UserSyncService userSyncService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
            String iamSub = jwt.getSubject();
            String displayName = jwt.getClaimAsString("user_displayname");

            if (iamSub != null) {
                try {
                    userSyncService.syncOAuthUser(iamSub, displayName);
                } catch (Exception e) {
                    log.error("Error sincronizando usuario OAuth {}: {}", iamSub, e.getMessage());
                    // No bloqueamos el request aunque falle el sync
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}