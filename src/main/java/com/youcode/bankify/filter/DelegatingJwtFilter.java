package com.youcode.bankify.filter;

import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DelegatingJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtRequestFilter jwtRequestFilter;
    private final JwtOAuth2Filter jwtOAuth2Filter;

    public DelegatingJwtFilter(JwtUtil jwtUtil , JwtDecoder jwtDecoder , UserRepository userRepository){
        this.jwtUtil = jwtUtil;
        this.jwtRequestFilter = new JwtRequestFilter(jwtUtil);
        this.jwtOAuth2Filter = new JwtOAuth2Filter(jwtDecoder,jwtUtil, userRepository);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7);

            // if it's a custome token , call JwtRequestilter
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
