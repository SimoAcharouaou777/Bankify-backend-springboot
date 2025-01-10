package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);
    List<Transaction> findByBankAccountId(Long bankAccountId, Pageable pageable);
    List<Transaction> findByUserId(Long userId);
}
