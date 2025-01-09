package com.youcode.bankify.service;

import com.youcode.bankify.dto.ScheduledTransferResponse;
import com.youcode.bankify.dto.TransferRequest;
import com.youcode.bankify.entity.BankAccount;
import com.youcode.bankify.entity.ScheduledTransfer;
import com.youcode.bankify.repository.jpa.AccountRepository;
import com.youcode.bankify.repository.jpa.ScheduledTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * *")
    public void executeScheduledTransfers(){
        OffsetDateTime now = OffsetDateTime.now();
        List<ScheduledTransfer> transfersToExecute = scheduledTransferRepository.findByNextExecutionDateBefore(now);

        for(ScheduledTransfer transfer : transfersToExecute){
            try{
                TransferRequest transferRequest = new TransferRequest();
                transferRequest.setFromAccount(transfer.getFromAccountId());
                var toAcc = accountRepository.findById(transfer.getToAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

                transferRequest.setToAccountNumber(toAcc.getAccountNumber());
                transferRequest.setAmount(transfer.getAmount().doubleValue());
                transferRequest.setTransactionType("PERMANENT");

                userService.transferFundsForScheduledJob(transferRequest);

                transfer.setNextExecutionDate(calculateNextExecutionDate(transfer));
                scheduledTransferRepository.save(transfer);
            }catch (Exception e){
                System.out.println("Error executing transfer: " +e.getMessage());
            }
        }
    }
    private OffsetDateTime calculateNextExecutionDate(ScheduledTransfer transfer){
        OffsetDateTime nextExecutionDate = transfer.getNextExecutionDate();

        switch (transfer.getFrequency().toUpperCase()){
            case "DAILY":
                return nextExecutionDate.plusDays(1);
            case "WEEKLY":
                return nextExecutionDate.plusWeeks(1);
            case "MONTHLY":
                return nextExecutionDate.plusMonths(1);
            case "YEARLY":
                return  nextExecutionDate.plusYears(1);
            default:
                throw new IllegalArgumentException("Invalid frequency type");
        }
    }

    public List<ScheduledTransferResponse> getScheduledTransfersByAccountId(Long accountId , int page , int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ScheduledTransfer> scheduledTransfers = scheduledTransferRepository.findByFromAccountIdOrToAccountId(accountId, accountId , pageable);
        return scheduledTransfers.stream().map(this::mapToScheduledTransferResponse).collect(Collectors.toList());
    }

    private ScheduledTransferResponse mapToScheduledTransferResponse(ScheduledTransfer transfer) {
        ScheduledTransferResponse dto = new ScheduledTransferResponse();
        dto.setId(transfer.getId());
        dto.setFromAccountId(transfer.getFromAccountId());

        BankAccount fromAccount = accountRepository.findById(transfer.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("From account not found"));
        dto.setFromAccountNumber(fromAccount.getAccountNumber());

        dto.setToAccountId(transfer.getToAccountId());

        BankAccount toAccount = accountRepository.findById(transfer.getToAccountId())
                .orElseThrow(() -> new RuntimeException("To account not found"));
        dto.setToAccountNumber(toAccount.getAccountNumber());

        dto.setAmount(transfer.getAmount());
        dto.setFrequency(transfer.getFrequency());
        dto.setNextExecutionDate(transfer.getNextExecutionDate());
        dto.setEndDate(transfer.getEndDate());
        return dto;
    }
}
