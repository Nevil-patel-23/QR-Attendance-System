package com.university.attendance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = null;
        boolean tokenFromCookie = false;

        // 1. Try Authorization header first (for API clients)
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        // 2. Fallback: read from jwt cookie (for browser sessions)
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        tokenFromCookie = true;
                        break;
                    }
                }
            }
        }

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                String prn = jwtUtil.extractPrn(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(prn);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (tokenFromCookie) {
                // Token is invalid or expired — clear the stale cookie so the browser
                // stops sending it, letting Spring Security redirect to login instead of 403
                Cookie clearCookie = new Cookie("jwt", "");
                clearCookie.setMaxAge(0);
                clearCookie.setPath("/");
                clearCookie.setHttpOnly(true);
                response.addCookie(clearCookie);
            }
        }

        filterChain.doFilter(request, response);
    }
}
