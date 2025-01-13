package com.youcode.bankify.filter;

import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class DelegatingJwtFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_URLS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final JwtRequestFilter jwtRequestFilter;
    private final JwtOAuth2Filter jwtOAuth2Filter;

    @Autowired
    public DelegatingJwtFilter(JwtUtil jwtUtil, JwtDecoder jwtDecoder, UserRepository userRepository){
        this.jwtUtil = jwtUtil;
        this.jwtRequestFilter = new JwtRequestFilter(jwtUtil);
        this.jwtOAuth2Filter = new JwtOAuth2Filter(jwtDecoder, jwtUtil, userRepository);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        for(String pattern : EXCLUDED_URLS){
            if(pathMatcher.match(pattern, requestPath)){
                logger.debug("Excluding path from JWT filter : "+ requestPath);
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7);

            if(jwtUtil.isCustomToken(token)){
                jwtRequestFilter.doFilter(request, response, filterChain);
                return;
            } else {
                jwtOAuth2Filter.doFilter(request, response , filterChain);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
