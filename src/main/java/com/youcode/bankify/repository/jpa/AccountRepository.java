package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<BankAccount,Long> {
    Page<BankAccount> findByUserId(Long userId, Pageable pageable);
    boolean existsByAccountNumber(String accountNumber);
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByUserId(Long userId);
    int countByStatus(String status);
    List<BankAccount> findTop5ByOrderByIdDesc();
}
