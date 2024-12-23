package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByIdentityNumber(String identityNumber);
    Optional<User> findByKeycloakId(String keycloakId);
}
