package com.youcode.bankify.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtAuthenticationConverterWrapper implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakRealmRoleConverter roleConverter = new KeycloakRealmRoleConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = roleConverter.convert(jwt);

        String keycloakUsername = jwt.getClaimAsString("preferred_username");
        if(keycloakUsername == null || keycloakUsername.isBlank()){
            keycloakUsername = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, keycloakUsername);
    }
}