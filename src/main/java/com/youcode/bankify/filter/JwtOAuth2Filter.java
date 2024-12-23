package com.youcode.bankify.filter;

import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.util.JwtAuthenticationConverterWrapper;
import com.youcode.bankify.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtOAuth2Filter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthProvider;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtOAuth2Filter(JwtDecoder jwtDecoder, JwtUtil jwtUtil, UserRepository userRepository){
        this.jwtAuthProvider = new JwtAuthenticationProvider(jwtDecoder);
        this.jwtAuthProvider.setJwtAuthenticationConverter(new JwtAuthenticationConverterWrapper());
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            String token = authorizationHeader.substring(7);

            if(jwtUtil.isCustomToken(token)){
                // Custom token should not be processed by this filter
                filterChain.doFilter(request, response);
                return;
            }

            try {
                var authentication = jwtAuthProvider.authenticate(new BearerTokenAuthenticationToken(token));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if(authentication != null && authentication.isAuthenticated()){
                    Jwt principalJwt = (Jwt) authentication.getPrincipal();

                    String preferredUsername = principalJwt.getClaimAsString("preferred_username");
                    if(preferredUsername == null || preferredUsername.isEmpty()){
                        preferredUsername = principalJwt.getSubject();
                    }
                    String keycloakSub = principalJwt.getSubject();

                    var optionalUser = userRepository.findByKeycloakId(keycloakSub);
                    if(optionalUser.isEmpty()){
                        User newUser = new User();
                        newUser.setKeycloakId(keycloakSub);
                        newUser.setUsername(preferredUsername);
                        newUser.setPassword("N/A"); // Dummy password

                        userRepository.save(newUser);
                        logger.info("Created new local user for Keycloak principal: " + preferredUsername);
                    }
                }
            } catch (Exception e){
                logger.error("Keycloak token authentication failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
