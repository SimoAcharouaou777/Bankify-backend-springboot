package com.youcode.bankify.controller;

import com.youcode.bankify.dto.*;
import com.youcode.bankify.entity.RefreshToken;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.exception.UsernameAlreadyExistsException;
import com.youcode.bankify.service.AuthService;
import com.youcode.bankify.service.RefreshTokenService;
import com.youcode.bankify.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(AuthService authService, JwtUtil jwtUtil, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
       try {
           authService.register(registerRequest);
           return ResponseEntity.ok(new SuccessResponse("User registered successfully"));
       } catch (UsernameAlreadyExistsException e){
           return ResponseEntity.status(HttpStatus.SC_CONFLICT).body(new ErrorResponse(e.getMessage()));
       } catch (Exception e){
           return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(new ErrorResponse("Registration failed"));
       }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            // Fetch user details
            User user = authService.getUserByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user, authService.getAuthorities(user));
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
            String role = user.getRoles().stream()
                    .findFirst()
                    .map(r -> r.getName().replace("ROLE_", ""))
                    .orElse(null);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken.getToken());
            tokens.put("role", role);

            return ResponseEntity.ok(tokens);
        } catch (AuthenticationException e){
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(new ErrorResponse("Refresh token is missing"));
        }

        String refreshTokenStr = authorizationHeader.substring(7);

        try{

            RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();


            String newAccessToken = jwtUtil.generateToken(user, authService.getAuthorities(user));

            Map<String, String> token = new HashMap<>();
            token.put("accessToken", newAccessToken);
            return ResponseEntity.ok(token);
        } catch(Exception e){
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody LogoutRequest logoutRequest, @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {

        String refreshToken = logoutRequest.getRefreshToken();

        if(refreshToken == null || refreshToken.isEmpty()){
            return ResponseEntity.badRequest().body(new ErrorResponse("Refresh token is missing"));
        }

        try{
            authService.invalidateRefreshToken(refreshToken);
            if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
                String accessToken = authorizationHeader.substring(7);
                authService.blacklistAccessToken(accessToken);
            }

            return ResponseEntity.ok(new SuccessResponse("User logged out successfully"));

        }catch (Exception e){
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
}
