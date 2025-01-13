package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);
    List<Transaction> findByBankAccountId(Long bankAccountId, Pageable pageable);
    List<Transaction> findByUserId(Long userId);
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    int countByStatus(@Param("status") String status);

    List<Transaction> findTop5ByOrderByDateDesc();

}
