package com.youcode.bankify.service;

import com.youcode.bankify.dto.TransferRequest;
import com.youcode.bankify.entity.ScheduledTransfer;
import com.youcode.bankify.repository.jpa.ScheduledTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTransferService {

    @Autowired
    private ScheduledTransferRepository scheduledTransferRepository;

    @Autowired UserService userService;

    @Scheduled(cron = "0 0 0 * * *")
    public void executeScheduledTransfers(){
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTransfer> transfersToExecute = scheduledTransferRepository.findByNextExecutionDateBefore(now);

        for(ScheduledTransfer transfer : transfersToExecute){
            try{
                TransferRequest transferRequest = new TransferRequest();
                transferRequest.setFromAccount(transfer.getFromAccountId());
                transferRequest.setToAccount(transfer.getToAccountId());
                transferRequest.setAmount(transfer.getAmount().doubleValue());
                transferRequest.setTransactionType("PERMANENT");

                userService.transferFunds(transferRequest, transfer.getUserId());

                transfer.setNextExecutionDate(calculateNextExecutionDate(transfer));
                scheduledTransferRepository.save(transfer);
            }catch (Exception e){
                System.out.println("Error executing transfer: " +e.getMessage());
            }
        }
    }
    private LocalDateTime calculateNextExecutionDate(ScheduledTransfer transfer){
        LocalDateTime nextExecutionDate = transfer.getNextExecutionDate();

        switch (transfer.getFrequency().toUpperCase()){
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

}
