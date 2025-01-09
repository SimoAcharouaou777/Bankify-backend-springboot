package com.youcode.bankify.repository.jpa;

import com.youcode.bankify.entity.ScheduledTransfer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer,Long> {
    List<ScheduledTransfer> findByNextExecutionDateBefore(OffsetDateTime now);
    List<ScheduledTransfer> findByFromAccountIdOrToAccountId(Long fromAccountId,Long toAccount, Pageable pageable);
}
