package com.youcode.bankify.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "blacklisted_tokens")
public class BlackListedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant blackListedAt;

    public BlackListedToken() {}

    public BlackListedToken( String token, Instant blackListedAt){
        this.token = token;
        this.blackListedAt = blackListedAt;
    }
}
