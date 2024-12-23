package com.youcode.bankify.util;

import com.youcode.bankify.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private Key key;

    private final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 5; // 5 hours
    private final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 30; // 30 days

    @PostConstruct
    public void init(){
        key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Generate Access Token
     */
    public String generateToken(User user, Collection<SimpleGrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", roles)
                .setIssuer("custom")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuer("custom")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }

    /**
     * Check if the token is a custom token
     */
    public boolean isCustomToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "custom".equals(claims.getIssuer());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract Username from Token
     */
    public String extractUsername(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract username from token", e);
        }
    }

    /**
     * Extract Authorities from Token
     */
    public List<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        }
    }

    /**
     * Extract Claims from Token
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate Token
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Check if Token is Expired
     */
    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
