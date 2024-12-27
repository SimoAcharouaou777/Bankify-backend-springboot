package com.youcode.bankify.service;

import com.youcode.bankify.entity.BlackListedToken;
import com.youcode.bankify.repository.jpa.BlackListedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class BlackListedTokenService {

    private final BlackListedTokenRepository blackListedTokenRepository;

    public BlackListedTokenService(BlackListedTokenRepository blackListedTokenRepository){
        this.blackListedTokenRepository = blackListedTokenRepository;
    }

    @Transactional
    public void blacklistToken(String token){
        BlackListedToken blackListedToken = new BlackListedToken(token, Instant.now());
        blackListedTokenRepository.save(blackListedToken);
    }

    public boolean isTokenBlackListed(String token){
        Optional<BlackListedToken> blackListedToken = blackListedTokenRepository.findByToken(token);
        return blackListedToken.isPresent();
    }
}
