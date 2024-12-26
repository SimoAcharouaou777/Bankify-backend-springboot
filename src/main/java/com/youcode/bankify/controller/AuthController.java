package com.youcode.bankify.controller;

import com.youcode.bankify.dto.ErrorResponse;
import com.youcode.bankify.dto.LoginRequest;
import com.youcode.bankify.dto.RegisterRequest;
import com.youcode.bankify.dto.SuccessResponse;
import com.youcode.bankify.entity.User;
import com.youcode.bankify.exception.UsernameAlreadyExistsException;
import com.youcode.bankify.service.AuthService;
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

    @Autowired
    public AuthController(AuthService authService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
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
            String refreshToken = jwtUtil.generateRefreshToken(user);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);

            return ResponseEntity.ok(tokens);
        } catch (AuthenticationException e){
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            return ResponseEntity.status(401).body("Refresh token is missing or invalid.");
        }

        String refreshToken = authorizationHeader.substring(7);

        try{
            String username = jwtUtil.extractUsername(refreshToken);
            if(jwtUtil.isTokenExpired(refreshToken)){
                return ResponseEntity.status(401).body("Refresh token is expired.");
            }

            User user = authService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = jwtUtil.generateToken(user, authService.getAuthorities(user));

            Map<String, String> token = new HashMap<>();
            token.put("accessToken", newAccessToken);
            return ResponseEntity.ok(token);
        } catch(Exception e){
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }
}
