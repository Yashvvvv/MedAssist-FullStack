package com.medassist.medassist_backend.security;

import com.medassist.medassist_backend.service.JwtTokenService;
import com.medassist.medassist_backend.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("JWT token found for request: " + requestURI);
                }
                
                if (jwtTokenService.validateToken(jwt)) {
                    // Check if it's an access token (not a refresh token)
                    if (jwtTokenService.isAccessToken(jwt)) {
                        String username = jwtTokenService.getUsernameFromToken(jwt);
                        if (logger.isDebugEnabled()) {
                            logger.debug("JWT validation successful for user: " + username + " on request: " + requestURI);
                        }

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Authentication set successfully for user: " + username + " on request: " + requestURI);
                        }
                    } else {
                        logger.warn("Refresh token used for authentication on request: " + requestURI);
                    }
                } else {
                    logger.warn("JWT token validation failed for request: " + requestURI);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No JWT token found for request: " + requestURI);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context for request: " + requestURI, ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
