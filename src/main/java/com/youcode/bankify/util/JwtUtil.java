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

    @Value("${jwt.access.secret}")
    private String ACCESS_TOKEN_SECRET;

    @Value("${jwt.refresh.secret}")
    private String REFRESH_TOKEN_SECRET;

    private Key accessTokenKey;
    private Key refreshTokenKey;

    private final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 5; // 5 hours
    private final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 30; // 30 days

    @PostConstruct
    public void init(){
        accessTokenKey = Keys.hmacShaKeyFor(ACCESS_TOKEN_SECRET.getBytes());
        refreshTokenKey = Keys.hmacShaKeyFor(REFRESH_TOKEN_SECRET.getBytes());
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
                .signWith(SignatureAlgorithm.HS256, accessTokenKey)
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
                .signWith(SignatureAlgorithm.HS256, refreshTokenKey)
                .compact();
    }

    /**
     * Check if the token is a custom token
     */
    public boolean isCustomToken(String token) {
        try {
            Claims claims = extractClaims(token, accessTokenKey);
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
            Claims claims = extractClaims(token, accessTokenKey);
            return claims.getSubject();
        } catch (Exception e) {
            try{
                Claims claims = extractClaims(token, refreshTokenKey);
                return claims.getSubject();
            }catch (Exception ex) {
                throw new RuntimeException("Failed to extract username from token", ex);
            }
        }
    }

    /**
     * Extract Authorities from Token
     */
    public List<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        try {
            Claims claims = extractClaims(token, accessTokenKey);
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
    private Claims extractClaims(String token, Key key) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate Token
     */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token, accessTokenKey);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Check if Token is Expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token, accessTokenKey);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }
}
