package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice,Long> {
    List<Invoice> findByUserId(Long userId);
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = :status")
    int countByStatus(@Param("status") String status);

}
