package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoanRepository  extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);
    List<Loan> findByStatus(String status);
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    int countByStatus(@Param("status") String status);

}
