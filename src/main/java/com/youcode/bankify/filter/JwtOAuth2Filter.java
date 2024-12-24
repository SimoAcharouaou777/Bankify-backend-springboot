package com.youcode.bankify.filter;

import com.youcode.bankify.entity.User;
import com.youcode.bankify.repository.jpa.UserRepository;
import com.youcode.bankify.util.JwtAuthenticationConverterWrapper;
import com.youcode.bankify.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class JwtOAuth2Filter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthProvider;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtOAuth2Filter(JwtDecoder jwtDecoder, JwtUtil jwtUtil, UserRepository userRepository) {
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
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            if (jwtUtil.isCustomToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // Validate Keycloak token
                var authentication = jwtAuthProvider.authenticate(new BearerTokenAuthenticationToken(token));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (authentication != null && authentication.isAuthenticated()) {
                    // Extract claims
                    Jwt principalJwt = (Jwt) authentication.getPrincipal();

                    String keycloakSub = principalJwt.getSubject();
                    String preferredUsername = principalJwt.getClaimAsString("preferred_username");
                    if (preferredUsername == null || preferredUsername.isEmpty()) {
                        preferredUsername = keycloakSub;
                    }

                    String firstName = principalJwt.getClaimAsString("given_name");
                    String lastName = principalJwt.getClaimAsString("family_name");

                    Optional<User> existingBySub = userRepository.findByKeycloakId(keycloakSub);
                    if (existingBySub.isEmpty()) {

                        Optional<User> existingByName = userRepository.findByUsername(preferredUsername);

                        if (existingByName.isPresent()) {
                            User existingUser = existingByName.get();
                            if (existingUser.getKeycloakId() == null) {
                                existingUser.setKeycloakId(keycloakSub);
                                userRepository.save(existingUser);
                                logger.info("Bound existing user record to Keycloak principal: " + preferredUsername);
                            } else {
                                logger.error("Collision: username '{}' is already taken by a different Keycloak user");
                            }
                        } else {
                            User newUser = new User();
                            newUser.setKeycloakId(keycloakSub);
                            newUser.setUsername(preferredUsername);
                            newUser.setFirstName(firstName != null ? firstName : "Unknown");
                            newUser.setLastName(lastName != null ? lastName : "Unknown");
                            newUser.setPassword("N/A");
                            newUser.setEnabled(true);

                            userRepository.save(newUser);
                            logger.info("Created new local user for Keycloak principal: " + preferredUsername);
                        }
                    }
                }
            } catch (ConstraintViolationException cve) {
                logger.error("Keycloak token authentication failed due to DB constraint: " + cve.getMessage());
            } catch (InvalidBearerTokenException |
                     AuthenticationCredentialsNotFoundException |
                     AuthenticationServiceException authEx) {
                logger.error("Keycloak token is invalid / expired or auth error: " + authEx.getMessage());
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid / expired token");
                return;
            } catch (Exception e){
                logger.error("Unexpected error in JwtOAuth2Filter: "+ e.getMessage());
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
